package trace.demoapp.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import trace.demoapp.SeededBugs;

/** Tax rules. SKU-2 is zero-rated. Rates come from pricing.tax.percent (default 10). */
@Service
public class TaxService {
    private final Environment environment;
    private final SeededBugs seeded;

    TaxService(Environment environment, SeededBugs seeded) {
        this.environment = environment; this.seeded = seeded;
    }

    long taxCents(long subtotalCents) { return subtotalCents * percent() / 100; }

    int percent() {
        // SEEDED-B14 (profile b14): reads the wrong key (pricing.vat.percent is
        // not configured, so the 25% fallback silently replaces the real 10%).
        if (seeded.active("b14")) return Integer.parseInt(environment.getProperty("pricing.vat.percent", "25"));
        return Integer.parseInt(environment.getProperty("pricing.tax.percent", "10"));
    }

    /** Tax for a product; zero-rated products owe 0. Boxed: may be null only in the seeded variant. */
    Long taxFor(String sku, long subtotalCents) {
        if ("SKU-2".equals(sku)) {
            // SEEDED-B02 (profile b02): zero-rated lookup returns null instead of 0.
            return seeded.active("b02") ? null : 0L;
        }
        return taxCents(subtotalCents);
    }
}
