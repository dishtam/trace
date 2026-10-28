package trace.demoapp.data;
import jakarta.persistence.*;
@Entity
public class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY) public OrderEntity order;
    public String sku; public int quantity;
    protected OrderLine() {}
    public OrderLine(OrderEntity order, String sku, int quantity) { this.order = order; this.sku = sku; this.quantity = quantity; }
}
