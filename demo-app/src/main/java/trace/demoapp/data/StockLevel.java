package trace.demoapp.data;
import jakarta.persistence.*;
@Entity
public class StockLevel {
    @Id public String sku; public int quantity;
    protected StockLevel() {}
    public StockLevel(String sku, int quantity) { this.sku = sku; this.quantity = quantity; }
}
