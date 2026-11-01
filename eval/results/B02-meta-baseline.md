---
bug: B02
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.TaxService#taxFor
budget: tokens_in=14166 tokens_out=3427 cost_usd=0.1252 tool_calls=7
budget_exceeded: false
wall_ms: 19168
tool_calls_log:
  - read_file({"path":"src/main/java/trace/demoapp/service/PricingService.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/service/PricingService.java
  - read_file({"path":"src/main/java/trace/demoapp/service/TaxService.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/service/TaxService.java
  - list_files({}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java/trace/demoapp/service/PricingService.java"}) -> java/trace/demoapp/service/PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/service/TaxService.java"}) -> java/trace/demoapp/service/TaxService.java lines 1..34 of 34 package trace.demoapp.service;  import org.springframework.
  - search_text({"pattern":"b02"}) -> java\trace\demoapp\service\TaxService.java:29: // SEEDED-B02 (profile b02): zero-rated lookup returns null instead of 0.
  - read_file({"path":"java/trace/demoapp/SeededBugs.java"}) -> java/trace/demoapp/SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
---

Root cause: TaxService.taxFor returns null instead of 0L for zero-rated SKU-2 when profile b02 is active, and PricingService unboxes that null (unconfirmed)
Evidence: Log NPE "return value of trace.demoapp.service.TaxService.taxFor(String, long) is null" at java/trace/demoapp/service/PricingService.java:39 in priceCents; TaxService.java:27-30 declares boxed Long taxFor and for "SKU-2" returns seeded.active("b02") ? null : 0L; SeededBugs.java:19-20 activates b02 by Spring profile; call chain CheckoutService.java:25 -> OrderService.java:34 per log produced HTTP 500 on POST /orders
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In TaxService.taxFor, always return 0L for SKU-2 (replace `seeded.active("b02") ? null : 0L` with `0L`)
Regression test: POST /orders {"sku":"SKU-2","quantity":1} under b02 profile expects HTTP 200 with zero tax, and a unit test that taxFor("SKU-2", anySubtotal)==0L
ROOT_CAUSE: trace.demoapp.service.TaxService#taxFor
