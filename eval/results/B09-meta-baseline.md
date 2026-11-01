---
bug: B09
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.PricingService#priceCents
budget: tokens_in=31936 tokens_out=5855 cost_usd=0.2448 tool_calls=10
budget_exceeded: false
wall_ms: 38865
tool_calls_log:
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java\\trace\\demoapp\\web\\PricingController.java"}) -> java\trace\demoapp\web\PricingController.java lines 1..71 of 71 package trace.demoapp.web;  import org.springframework.w
  - read_file({"path":"java\\trace\\demoapp\\service\\PricingService.java"}) -> java\trace\demoapp\service\PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java\\trace\\demoapp\\DataSeeder.java"}) -> java\trace\demoapp\DataSeeder.java lines 1..19 of 19 package trace.demoapp;  import org.springframework.boot.CommandLine
  - read_file({"path":"java\\trace\\demoapp\\SeededBugs.java"}) -> java\trace\demoapp\SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
  - read_file({"path":"java\\trace\\demoapp\\service\\TaxService.java"}) -> java\trace\demoapp\service\TaxService.java lines 1..34 of 34 package trace.demoapp.service;  import org.springframework.
  - search_text({"pattern":"lastTotalCents"}) -> java\trace\demoapp\service\PricingService.java:19: private Long lastTotalCents; java\trace\demoapp\service\PricingServic
  - read_file({"path":"java\\trace\\demoapp\\PricingConfig.java"}) -> java\trace\demoapp\PricingConfig.java lines 1..24 of 24 package trace.demoapp;  import org.springframework.context.annot
  - search_text({"pattern":"pricing\\.priceCents"}) -> java\trace\demoapp\service\CheckoutService.java:25: long lineTotal = pricing.priceCents(sku, quantity); java\trace\demoa
  - search_text({"pattern":"class PricingService|@Service","path_glob":"PricingService.java"}) -> java\trace\demoapp\service\PricingService.java:12: @Service java\trace\demoapp\service\PricingService.java:13: public cl
---

Root cause: Singleton PricingService caches the first computed total in shared mutable lastTotalCents and returns it for subsequent calls regardless of sku (unconfirmed)
Evidence: Bug report: GET /pricing?sku=SKU-1&quantity=1 -> 1100 then GET /pricing?sku=SKU-2&quantity=1 -> 1100; java\trace\demoapp\web\PricingController.java:17 delegates to pricing.priceCents(sku, quantity); java\trace\demoapp\service\PricingService.java:12-13 @Service singleton with java\trace\demoapp\service\PricingService.java:19 private Long lastTotalCents; java\trace\demoapp\service\PricingService.java:31 returns lastTotalCents when seeded.active("b09") && lastTotalCents != null, and java\trace\demoapp\service\PricingService.java:41 stores each total there; expected distinct totals from java\trace\demoapp\DataSeeder.java:11-12 (SKU-1=1000, SKU-2=2500) and java\trace\demoapp\service\TaxService.java:27-31 (SKU-2 zero-rated), so SKU-2 should be 2500 not 1100
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Remove the lastTotalCents field, the early return at PricingService.java:31, and the assignment at PricingService.java:41 so priceCents computes per-call.
Regression test: In one service/app instance call priceCents("SKU-1",1) then priceCents("SKU-2",1) and assert 1100 then 2500.
ROOT_CAUSE: trace.demoapp.service.PricingService#priceCents
