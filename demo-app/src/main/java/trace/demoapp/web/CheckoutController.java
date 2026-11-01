package trace.demoapp.web;

import org.springframework.web.bind.annotation.*;
import trace.demoapp.service.CheckoutService;
import java.util.Map;

@RestController
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout) { this.checkout = checkout; }

    @GetMapping("/checkout/shipping")
    public Map<String, Object> shipping(@RequestParam String sku, @RequestParam int quantity) {
        return Map.of("sku", sku, "shippingCents", checkout.shippingCents(sku, quantity));
    }
}
