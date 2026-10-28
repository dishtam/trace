package trace.demoapp.data;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String sku; public int quantity; public long totalCents; public String status;
    // LAZY on purpose (demo-app-spec): rendering this outside a session is exactly
    // what the agent must never do (instrumentation-safety, unsafe_to_render).
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<OrderLine> lines = new ArrayList<>();
    protected OrderEntity() {}
    public OrderEntity(String sku, int quantity, long totalCents, String status) {
        this.sku = sku; this.quantity = quantity; this.totalCents = totalCents; this.status = status;
    }
}
