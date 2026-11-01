package trace.demoapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Pricing policy wiring. The always-discount policy is meant for the test-discount profile only. */
@Configuration
public class PricingConfig {

    /** Bulk-discount threshold in cents; orders at or above it get the discount. */
    public record DiscountPolicy(long thresholdCents) {}

    @Bean
    DiscountPolicy discountPolicy(Environment environment, SeededBugs seeded) {
        // SEEDED-B15 (profile b15): the profile-specific policy is installed
        // without checking that its profile is active (missing profile
        // condition), so every profile discounts everything.
        if (seeded.active("b15")) return new DiscountPolicy(0);
        boolean testDiscount = environment.acceptsProfiles(Profiles.of("test-discount"));
        return new DiscountPolicy(testDiscount ? 0 : 5000);
    }
}
