package trace.demoapp.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trace.demoapp.data.*;

import java.util.concurrent.CompletableFuture;

/** Clean (bug-free) Week 1 services. Seeded bugs arrive in S0-6 via the add-bug-fixture skill. */
@Service
public class InventoryService {
    private final StockLevelRepository stock;
    private final trace.demoapp.SeededBugs seeded;
    InventoryService(StockLevelRepository stock, trace.demoapp.SeededBugs seeded) {
        this.stock = stock; this.seeded = seeded;
    }

    public int available(String sku) { return stock.findById(sku).map(s -> s.quantity).orElse(0); }

    @Transactional
    public void decrement(String sku, int quantity) {
        StockLevel level = stock.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        // SEEDED-B04 (profile b04): boundary uses >=, so buying exactly the stock fails.
        boolean insufficient = seeded.active("b04") ? quantity >= level.quantity : quantity > level.quantity;
        if (insufficient) throw new IllegalStateException("insufficient stock for " + sku);
        level.quantity -= quantity;
        stock.save(level);
    }
}

