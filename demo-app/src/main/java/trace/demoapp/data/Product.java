package trace.demoapp.data;
import jakarta.persistence.*;
@Entity
public class Product {
    @Id public String sku; public String name; public long priceCents;
    /** Sale (discounted) unit price; 0 means no sale price set (SEEDED-B07 writes the wrong field). */
    public long salePriceCents;
    protected Product() {}
    public Product(String sku, String name, long priceCents) { this.sku = sku; this.name = name; this.priceCents = priceCents; }
}
