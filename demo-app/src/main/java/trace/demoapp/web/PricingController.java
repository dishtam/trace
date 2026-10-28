package trace.demoapp.web;

import org.springframework.web.bind.annotation.*;
import trace.demoapp.service.PricingService;
import trace.demoapp.service.InventoryService;
import java.util.Map;

@RestController
public class PricingController {
    private final PricingService pricing;
    public PricingController(PricingService pricing) { this.pricing = pricing; }
    @GetMapping("/pricing")
    public Map<String, Object> price(@RequestParam String sku, @RequestParam int quantity) {
        return Map.of("sku", sku, "totalCents", pricing.priceCents(sku, quantity));
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
}
