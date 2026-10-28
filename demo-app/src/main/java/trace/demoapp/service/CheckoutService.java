package trace.demoapp.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trace.demoapp.data.*;

import java.util.concurrent.CompletableFuture;

/** Clean (bug-free) Week 1 services. Seeded bugs arrive in S0-6 via the add-bug-fixture skill. */
@Service
public class CheckoutService {
    private final PricingService pricing;
    private final InventoryService inventory;
    CheckoutService(PricingService pricing, InventoryService inventory) { this.pricing = pricing; this.inventory = inventory; }

    public long checkout(String sku, int quantity) {
        long total = pricing.priceCents(sku, quantity);
        inventory.decrement(sku, quantity);
        return total;
    }
}

