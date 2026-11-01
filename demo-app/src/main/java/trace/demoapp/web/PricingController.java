package trace.demoapp.web;

import org.springframework.web.bind.annotation.*;
import trace.demoapp.SeededBugs;
import trace.demoapp.data.Product;
import trace.demoapp.service.PricingService;
import trace.demoapp.service.InventoryService;
import java.util.Map;

@RestController
public class PricingController {
    private final PricingService pricing;
    private final SeededBugs seeded;
    public PricingController(PricingService pricing, SeededBugs seeded) { this.pricing = pricing; this.seeded = seeded; }
    @GetMapping("/pricing")
    public Map<String, Object> price(@RequestParam String sku, @RequestParam int quantity) {
        return Map.of("sku", sku, "totalCents", pricing.priceCents(sku, quantity));
    }

    @GetMapping("/pricing/regional")
    public Map<String, Object> priceRegional(@RequestParam String sku, @RequestParam int quantity,
                                             @RequestParam String region) {
        return Map.of("sku", sku, "region", region, "totalCents", pricing.priceRegionalCents(sku, quantity, region));
    }

    public record SalePriceRequest(long salePriceCents) {}

    @PutMapping("/products/{sku}/sale-price")
    public Map<String, Object> updateSalePrice(@PathVariable String sku, @RequestBody SalePriceRequest request) {
        Product product = pricing.updateSalePrice(sku, request.salePriceCents());
        return productView(product);
    }

    @GetMapping("/products/{sku}")
    public Map<String, Object> product(@PathVariable String sku) {
        return productView(pricing.product(sku));
    }

    private static Map<String, Object> productView(Product product) {
        return Map.of("sku", product.sku, "listPriceCents", product.priceCents, "salePriceCents", product.salePriceCents);
    }

    /** Pricing callback from an external caller; the sender emits `totalCents`. */
    @PostMapping("/pricing/callback")
    public Map<String, Object> priceCallback(@RequestBody Map<String, Object> payload) {
        // SEEDED-B17 (profile b17): field rename breaks deserialization of the callback payload
        // (the receiver reads `total_cents`, the sender emits `totalCents`).
        String field = seeded.active("b17") ? "total_cents" : "totalCents";
        Object value = payload.get(field);
        long accepted = value instanceof Number number ? number.longValue() : 0;
        return Map.of("acceptedTotalCents", accepted);
    }
}

@RestController
class InventoryController {
    private final InventoryService inventory;
    InventoryController(InventoryService inventory) { this.inventory = inventory; }
    @GetMapping("/stock/{sku}")
    public Map<String, Object> stock(@PathVariable String sku) {
        return Map.of("sku", sku, "available", inventory.available(sku));
    }

    public record StockRequest(int quantity) {}

    @PutMapping("/stock/{sku}")
    public Map<String, Object> setStock(@PathVariable String sku, @RequestBody StockRequest request) {
        inventory.setStock(sku, request.quantity());
        return Map.of("sku", sku, "available", inventory.available(sku));
    }
}
