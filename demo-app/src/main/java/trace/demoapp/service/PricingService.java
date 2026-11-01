package trace.demoapp.service;

import org.springframework.stereotype.Service;
import trace.demoapp.PricingConfig.DiscountPolicy;
import trace.demoapp.SeededBugs;
import trace.demoapp.data.Product;
import trace.demoapp.data.ProductRepository;

import java.util.Map;

/** Pricing: subtotal, bulk discount at the policy threshold, plus tax. */
@Service
public class PricingService {
    private final ProductRepository products;
    private final TaxService tax;
    private final SeededBugs seeded;
    private final DiscountPolicy discountPolicy;
    // SEEDED-B09 (profile b09): shared mutable total on a singleton service.
    private Long lastTotalCents;

    PricingService(ProductRepository products, TaxService tax, SeededBugs seeded, DiscountPolicy discountPolicy) {
        this.products = products; this.tax = tax; this.seeded = seeded; this.discountPolicy = discountPolicy;
    }

    private static long unitPriceCents(Product product) {
        return product.salePriceCents > 0 ? product.salePriceCents : product.priceCents;
    }

    public long priceCents(String sku, int quantity) {
        // SEEDED-B09: a previous request's total bleeds into this one.
        if (seeded.active("b09") && lastTotalCents != null) return lastTotalCents;
        Product product = products.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        long subtotal = unitPriceCents(product) * quantity;
        // SEEDED-B03 (profile b03): discount comparison inverted (< instead of >=).
        boolean discount = seeded.active("b03") ? subtotal < discountPolicy.thresholdCents()
                                                : subtotal >= discountPolicy.thresholdCents();
        long discounted = discount ? subtotal - subtotal / 10 : subtotal;
        // Unboxing TaxService.taxFor: NPE here when B02 makes it return null.
        long taxAmount = tax.taxFor(sku, discounted);
        long total = discounted + taxAmount;
        if (seeded.active("b09")) lastTotalCents = total;
        return total;
    }

    /** Regional pricing: region tax rate on the discounted subtotal. */
    public long priceRegionalCents(String sku, int quantity, String region) {
        Product product = products.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        long subtotal = unitPriceCents(product) * quantity;
        long discounted = subtotal >= discountPolicy.thresholdCents() ? subtotal - subtotal / 10 : subtotal;
        // SEEDED-B18 (profile b18): rate map looked up by sku instead of region;
        // the null result is unboxed below.
        Integer percent = seeded.active("b18")
                ? REGION_TAX_PERCENT.get(sku)
                : REGION_TAX_PERCENT.getOrDefault(region, 10);
        long taxAmount = discounted * percent / 100;
        return discounted + taxAmount;
    }

    private static final Map<String, Integer> REGION_TAX_PERCENT = Map.of("EU", 10, "US", 5);

    /** Updates the product's sale price (SEEDED-B07 writes the list price instead). */
    public Product updateSalePrice(String sku, long salePriceCents) {
        Product product = products.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
        // SEEDED-B07 (profile b07): writes to list price instead of sale price field.
        if (seeded.active("b07")) product.priceCents = salePriceCents;
        else product.salePriceCents = salePriceCents;
        return products.save(product);
    }

    public Product product(String sku) {
        return products.findById(sku).orElseThrow(() -> new IllegalArgumentException("unknown sku " + sku));
    }
}
