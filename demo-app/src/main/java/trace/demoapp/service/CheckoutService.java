package trace.demoapp.service;

import org.springframework.stereotype.Service;
import trace.demoapp.SeededBugs;
import trace.demoapp.data.ProductRepository;

import java.util.List;

/** Checkout: price the order, take the stock, return the total. Free shipping at/above the threshold. */
@Service
public class CheckoutService {
    private static final long FREE_SHIPPING_THRESHOLD_CENTS = 5000;
    private static final long SHIPPING_CENTS = 500;

    private final PricingService pricing;
    private final InventoryService inventory;
    private final ProductRepository products;
    private final SeededBugs seeded;
    CheckoutService(PricingService pricing, InventoryService inventory, ProductRepository products, SeededBugs seeded) {
        this.pricing = pricing; this.inventory = inventory; this.products = products; this.seeded = seeded;
    }

    public long checkout(String sku, int quantity) {
        try {
            long lineTotal = pricing.priceCents(sku, quantity);
            long total = sumLineTotals(List.of(lineTotal));
            inventory.decrement(sku, quantity);
            return total;
        } catch (RuntimeException failure) {
            // SEEDED-B20 (profile b20): cause chain replaced by a new exception without the cause.
            if (seeded.active("b20")) throw new IllegalStateException("checkout failed for " + sku);
            throw failure;
        }
    }

    /** Sums an order's line totals. */
    long sumLineTotals(List<Long> lineTotalsCents) {
        // SEEDED-B06 (profile b06): loop bound drops the last line.
        int bound = seeded.active("b06") ? lineTotalsCents.size() - 1 : lineTotalsCents.size();
        long sum = 0;
        for (int i = 0; i < bound; i++) sum += lineTotalsCents.get(i);
        return sum;
    }

    /** Shipping is free when what the customer pays after discount reaches the threshold. */
    public long shippingCents(String sku, int quantity) {
        // SEEDED-B19 (profile b19): compares the pre-discount subtotal instead of the post-discount total.
        long basis = seeded.active("b19")
                ? products.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku)).priceCents * quantity
                : pricing.priceCents(sku, quantity);
        return basis >= FREE_SHIPPING_THRESHOLD_CENTS ? 0 : SHIPPING_CENTS;
    }
}
