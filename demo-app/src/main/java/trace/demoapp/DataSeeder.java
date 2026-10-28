package trace.demoapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import trace.demoapp.data.*;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(ProductRepository products, StockLevelRepository stock) {
        return args -> {
            products.save(new Product("SKU-1", "Widget", 1000));
            products.save(new Product("SKU-2", "Gadget", 2500));
            stock.save(new StockLevel("SKU-1", 100000));
            stock.save(new StockLevel("SKU-2", 100000));
        };
    }
}
