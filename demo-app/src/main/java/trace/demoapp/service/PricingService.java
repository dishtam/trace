package trace.demoapp.service;

import org.springframework.stereotype.Service;
import trace.demoapp.SeededBugs;
import trace.demoapp.data.Product;
import trace.demoapp.data.ProductRepository;

/** Pricing: subtotal, 10% bulk discount at >= 5000, plus tax. */
@Service
public class PricingService {
    private final ProductRepository products;
    private final TaxService tax;
    private final SeededBugs seeded;
    // SEEDED-B09 (profile b09): shared mutable total on a singleton service.
    private Long lastTotalCents;

    PricingService(ProductRepository products, TaxService tax, SeededBugs seeded) {
        this.products = products; this.tax = tax; this.seeded = seeded;
    }

    public long priceCents(String sku, int quantity) {
        // SEEDED-B09: a previous request's total bleeds into this one.
        if (seeded.active("b09") && lastTotalCents != null) return lastTotalCents;
        Product product = products.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        long subtotal = product.priceCents * quantity;
        // SEEDED-B03 (profile b03): discount comparison inverted (< instead of >=).
        boolean discount = seeded.active("b03") ? subtotal < 5000 : subtotal >= 5000;
        long discounted = discount ? subtotal - subtotal / 10 : subtotal;
        // Unboxing TaxService.taxFor: NPE here when B02 makes it return null.
        long taxAmount = tax.taxFor(sku, discounted);
        long total = discounted + taxAmount;
        if (seeded.active("b09")) lastTotalCents = total;
        return total;
    }
}
