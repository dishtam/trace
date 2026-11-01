---
bug: B14
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.TaxService#percent
budget: tokens_in=28365 tokens_out=4792 cost_usd=0.2093 tool_calls=16
budget_exceeded: false
wall_ms: 26481
tool_calls_log:
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java/trace/demoapp/web/PricingController.java"}) -> java/trace/demoapp/web/PricingController.java lines 1..71 of 71 package trace.demoapp.web;  import org.springframework.w
  - read_file({"path":"java/trace/demoapp/service/PricingService.java"}) -> java/trace/demoapp/service/PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/service/TaxService.java"}) -> java/trace/demoapp/service/TaxService.java lines 1..34 of 34 package trace.demoapp.service;  import org.springframework.
  - read_file({"path":"java/trace/demoapp/PricingConfig.java"}) -> java/trace/demoapp/PricingConfig.java lines 1..24 of 24 package trace.demoapp;  import org.springframework.context.annot
  - read_file({"path":"resources/application.properties"}) -> resources/application.properties lines 1..7 of 7 spring.application.name=trace-demo spring.datasource.url=jdbc:h2:mem:de
  - search_text({"pattern":"pricing\\.vat\\.percent"}) -> java\trace\demoapp\service\TaxService.java:20: // SEEDED-B14 (profile b14): reads the wrong key (pricing.vat.percent is 
  - search_text({"pattern":"SeededBugs","path_glob":"*.java"}) -> java\trace\demoapp\PricingConfig.java:16: DiscountPolicy discountPolicy(Environment environment, SeededBugs seeded) { ja
  - read_file({"path":"java/trace/demoapp/DataSeeder.java"}) -> java/trace/demoapp/DataSeeder.java lines 1..19 of 19 package trace.demoapp;  import org.springframework.boot.CommandLine
  - search_text({"pattern":"pricing\\.tax\\.percent=10"}) -> resources\application.properties:7: pricing.tax.percent=10
  - search_text({"pattern":"long taxAmount = tax\\.taxFor"}) -> java\trace\demoapp\service\PricingService.java:39: long taxAmount = tax.taxFor(sku, discounted);
  - search_text({"pattern":"int percent\\(\\)"}) -> java\trace\demoapp\service\TaxService.java:19: int percent() {
  - read_file({"path":"java/trace/demoapp/SeededBugs.java"}) -> java/trace/demoapp/SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
  - search_text({"pattern":"SKU-1.*1000"}) -> java\trace\demoapp\DataSeeder.java:13: products.save(new Product("SKU-1", "Widget", 1000)); java\trace\demoapp\DataSeede
  - search_text({"pattern":"taxCents\\(long"}) -> java\trace\demoapp\service\TaxService.java:17: long taxCents(long subtotalCents) { return subtotalCents * percent() / 10
  - search_text({"pattern":"pricing\\.priceCents"}) -> java\trace\demoapp\service\CheckoutService.java:25: long lineTotal = pricing.priceCents(sku, quantity); java\trace\demoa
---

Root cause: TaxService reads the unconfigured `pricing.vat.percent` key and silently falls back to 25% instead of the configured 10% tax rate (unconfirmed)
Evidence: GET /pricing?sku=SKU-1&quantity=1 returned 1250 vs expected 1100; PricingController.java:17 delegates to PricingService.priceCents; DataSeeder.java:13 seeds SKU-1 at 1000 cents; PricingService.java:39 adds TaxService.taxFor tax to the subtotal; TaxService.java:17 computes subtotal * percent()/100; TaxService.java:22 (SEEDED-B14) reads `pricing.vat.percent` with default "25" while resources/application.properties:7 only sets `pricing.tax.percent=10`, so 1000 + 25% = 1250, whereas TaxService.java:23's correct key would give 1000 + 10% = 1100
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In TaxService.percent() change the b14 branch key from `pricing.vat.percent`/"25" to `pricing.tax.percent`/"10" (or remove the seeded branch, keeping line 23)
Regression test: With only `pricing.tax.percent=10` configured (no vat property), assert TaxService.percent() returns 10 and GET /pricing?sku=SKU-1&quantity=1 returns totalCents 1100
ROOT_CAUSE: trace.demoapp.service.TaxService#percent
