package trace.demoapp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trace.demoapp.data.*;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/** Inventory. Clean decrement is one atomic conditional update; seeded paths are read-check-act. */
@Service
public class InventoryService {
    private final StockLevelRepository stock;
    private final trace.demoapp.SeededBugs seeded;
    // SEEDED-B12 determinism device: rendezvouses two decrementing threads between check and act.
    private static final CyclicBarrier CHECK_ACT_BARRIER = new CyclicBarrier(2);
    InventoryService(StockLevelRepository stock, trace.demoapp.SeededBugs seeded) {
        this.stock = stock; this.seeded = seeded;
    }

    public int available(String sku) { return stock.findById(sku).map(s -> s.quantity).orElse(0); }

    @Transactional
    public void setStock(String sku, int quantity) {
        stock.save(new StockLevel(sku, quantity));
    }

    @Transactional
    public void restock(String sku, int quantity) {
        StockLevel level = stock.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        level.quantity += quantity;
        stock.save(level);
    }

    @Transactional
    public void decrement(String sku, int quantity) {
        // SEEDED-B04/B12 take the read-check-act path; the clean path is atomic.
        if (seeded.active("b04") || seeded.active("b12")) {
            decrementChecked(sku, quantity);
            return;
        }
        stock.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        if (stock.decrementIfAvailable(sku, quantity) == 0) {
            throw new IllegalStateException("insufficient stock for " + sku);
        }
    }

    private void decrementChecked(String sku, int quantity) {
        StockLevel level = stock.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        // SEEDED-B04 (profile b04): boundary uses >=, so buying exactly the stock fails.
        boolean insufficient = seeded.active("b04") ? quantity >= level.quantity : quantity > level.quantity;
        if (insufficient) throw new IllegalStateException("insufficient stock for " + sku);
        // SEEDED-B12 (profile b12): check-then-act is not atomic; both racers pass the check first.
        if (seeded.active("b12")) awaitRendezvous();
        level.quantity -= quantity;
        stock.save(level);
    }

    private static void awaitRendezvous() {
        try {
            CHECK_ACT_BARRIER.await(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("seeded-b12 rendezvous failed", e);
        }
    }
}
