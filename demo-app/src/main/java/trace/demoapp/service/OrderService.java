package trace.demoapp.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trace.demoapp.data.*;

import java.util.concurrent.CompletableFuture;

/** Services. Seeded bugs: B01, B05, B10 (marked SEEDED-Bxx, profile-gated). */
@Service
public class OrderService {
    private final CheckoutService checkout;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final trace.demoapp.SeededBugs seeded;
    OrderService(CheckoutService checkout, OrderRepository orders, ProductRepository products,
                 trace.demoapp.SeededBugs seeded) {
        this.checkout = checkout; this.orders = orders; this.products = products; this.seeded = seeded;
    }

    @Transactional
    public OrderEntity placeOrder(String sku, int quantity) {
        // SEEDED-B01 (profile b01): repository result may be null for an unknown SKU; dereferenced unchecked.
        long b01Adjustment = 0;
        if (seeded.active("b01")) {
            Product product = products.findById(sku).orElse(null);
            long unitPriceCents = product.priceCents; // dereference of a possibly-null repository result
            b01Adjustment = unitPriceCents * 0;
        }
        long total = checkout.checkout(sku, quantity) + b01Adjustment;
        OrderEntity order = new OrderEntity(sku, quantity, total, "PLACED");
        order.lines.add(new OrderLine(order, sku, quantity));
        OrderEntity saved = orders.save(order);
        confirmAsync(saved.id);
        return saved;
    }

    /** One async path (demo-app-spec): exercises thread hand-off capture later. */
    @Async("orderExecutor")
    public CompletableFuture<Void> confirmAsync(Long orderId) {
        return CompletableFuture.completedFuture(null);
    }

    /** JPA/lazy path inside a transaction: touches the LAZY lines collection in-session. */
    @Transactional(readOnly = true)
    public int lineCount(Long orderId) {
        return orders.findById(orderId).map(o -> o.lines.size()).orElse(0);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderEntity> listOrders(int page, int size) {
        java.util.List<OrderEntity> all = orders.findAll();
        // SEEDED-B05 (profile b05): offset is one too far, skipping the first element of each page.
        int from = seeded.active("b05") ? page * size + 1 : page * size;
        if (from >= all.size()) return java.util.List.of();
        return all.subList(from, Math.min(from + size, all.size()));
    }

    @Transactional(readOnly = true)
    public OrderEntity get(Long orderId) {
        // SEEDED-B10 (profile b10): lookup failure swallowed, null returned to the caller.
        if (seeded.active("b10")) {
            try { return orders.findById(orderId).orElseThrow(); }
            catch (RuntimeException swallowed) { return null; }
        }
        return orders.findById(orderId).orElseThrow();
    }
}


