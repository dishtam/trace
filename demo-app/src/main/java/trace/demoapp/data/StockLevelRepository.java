package trace.demoapp.data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface StockLevelRepository extends JpaRepository<StockLevel, String> {
    /** Atomic check-and-decrement: one conditional update, no read-modify-write race (clean path for SEEDED-B12). */
    @Modifying
    @Query("update StockLevel s set s.quantity = s.quantity - :qty where s.sku = :sku and s.quantity >= :qty")
    int decrementIfAvailable(@Param("sku") String sku, @Param("qty") int qty);
}
