package trace.demoapp.web;

import org.springframework.web.bind.annotation.*;
import trace.demoapp.SeededBugs;
import trace.demoapp.service.OrderService;
import trace.demoapp.data.OrderEntity;
import java.util.List;
import java.util.Map;

@RestController
public class OrderController {
    private final OrderService orders;
    private final SeededBugs seeded;
    public OrderController(OrderService orders, SeededBugs seeded) { this.orders = orders; this.seeded = seeded; }

    public record PlaceOrderRequest(String sku, int quantity) {}

    @PostMapping("/orders")
    public Map<String, Object> place(@RequestBody PlaceOrderRequest request) {
        OrderEntity order = orders.placeOrder(request.sku(), request.quantity());
        // SEEDED-B16 (profile b16): response mapping drops totalCents.
        if (seeded.active("b16")) return Map.of("id", order.id, "status", order.status);
        return Map.of("id", order.id, "totalCents", order.totalCents, "status", order.status);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return orders.listOrders(page, size).stream()
                .map(o -> Map.<String, Object>of("id", o.id, "sku", o.sku, "totalCents", o.totalCents, "status", o.status))
                .toList();
    }

    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        OrderEntity order = orders.get(id);
        // Under B10 this dereference is where the swallowed failure surfaces.
        return Map.of("id", order.id, "sku", order.sku, "totalCents", order.totalCents, "status", order.status);
    }

    @GetMapping("/orders/{id}/lines")
    public Map<String, Object> lines(@PathVariable Long id) {
        return Map.of("orderId", id, "lineCount", orders.lineCount(id));
    }
}
