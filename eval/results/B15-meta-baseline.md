---
bug: B15
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.PricingConfig#discountPolicy
budget: tokens_in=15452 tokens_out=3815 cost_usd=0.1381 tool_calls=11
budget_exceeded: false
wall_ms: 23695
tool_calls_log:
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java\\trace\\demoapp\\web\\PricingController.java"}) -> java\trace\demoapp\web\PricingController.java lines 1..71 of 71 package trace.demoapp.web;  import org.springframework.w
  - read_file({"path":"java\\trace\\demoapp\\service\\PricingService.java"}) -> java\trace\demoapp\service\PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java\\trace\\demoapp\\PricingConfig.java"}) -> java\trace\demoapp\PricingConfig.java lines 1..24 of 24 package trace.demoapp;  import org.springframework.context.annot
  - read_file({"path":"resources\\application.properties"}) -> resources\application.properties lines 1..7 of 7 spring.application.name=trace-demo spring.datasource.url=jdbc:h2:mem:de
  - read_file({"path":"java\\trace\\demoapp\\service\\TaxService.java"}) -> java\trace\demoapp\service\TaxService.java lines 1..34 of 34 package trace.demoapp.service;  import org.springframework.
  - read_file({"path":"java\\trace\\demoapp\\DataSeeder.java"}) -> java\trace\demoapp\DataSeeder.java lines 1..19 of 19 package trace.demoapp;  import org.springframework.boot.CommandLine
  - read_file({"path":"java\\trace\\demoapp\\SeededBugs.java"}) -> java\trace\demoapp\SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
  - search_text({"pattern":"DiscountPolicy\\(0\\)|testDiscount|thresholdCents"}) -> java\trace\demoapp\PricingConfig.java:13: public record DiscountPolicy(long thresholdCents) {} java\trace\demoapp\Pricin
  - search_text({"pattern":"SKU-1.*1000|new Product\\(\"SKU-1\""}) -> java\trace\demoapp\DataSeeder.java:13: products.save(new Product("SKU-1", "Widget", 1000)); java\trace\demoapp\DataSeede
  - search_text({"pattern":"discounted = discount"}) -> java\trace\demoapp\service\PricingService.java:37: long discounted = discount ? subtotal - subtotal / 10 : subtotal;
---

Root cause: PricingConfig installs a zero bulk-discount threshold regardless of the test-discount profile, so every subtotal qualifies for discount (unconfirmed)
Evidence: GET /pricing?sku=SKU-1&quantity=1 returned 990; DataSeeder.java:13 seeds SKU-1 at 1000 cents for subtotal 1000; PricingConfig.java:20 returns new DiscountPolicy(0) when b15 is active without the test-discount check present at PricingConfig.java:21-22 (otherwise 5000); PricingService.java:35-37 therefore applies 10% discount (900), plus 10% tax from resources/application.properties:7 gives 990 instead of clean 1000+100=1100
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In PricingConfig.java:20, only return DiscountPolicy(0) when the test-discount profile is active (or remove that branch so lines 21-22 decide), otherwise return DiscountPolicy(5000)
Regression test: Without test-discount, assert GET /pricing?sku=SKU-1&quantity=1 returns 1100, and quantity=5 still returns discounted 4950
ROOT_CAUSE: trace.demoapp.PricingConfig#discountPolicy
