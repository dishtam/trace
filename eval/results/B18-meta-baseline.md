---
bug: B18
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.PricingService#priceRegionalCents
budget: tokens_in=11266 tokens_out=3350 cost_usd=0.1121 tool_calls=6
budget_exceeded: false
wall_ms: 14980
tool_calls_log:
  - read_file({"path":"src/main/java/trace/demoapp/service/PricingService.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/service/PricingService.java
  - read_file({"path":"src/main/java/trace/demoapp/web/PricingController.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/web/PricingController.java
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java/trace/demoapp/service/PricingService.java"}) -> java/trace/demoapp/service/PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/web/PricingController.java"}) -> java/trace/demoapp/web/PricingController.java lines 1..71 of 71 package trace.demoapp.web;  import org.springframework.w
  - read_file({"path":"java/trace/demoapp/SeededBugs.java"}) -> java/trace/demoapp/SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
---

Root cause: In profile b18, PricingService looks up the regional tax map by sku instead of region, returning null for SKU-1 which is then unboxed (unconfirmed)
Evidence: GET /pricing/regional?sku=SKU-1&quantity=1&region=EU -> HTTP 500 NPE at PricingService.java:55 via PricingController.java:23; PricingService.java:52-54 uses REGION_TAX_PERCENT.get(sku) when b18 is active, while PricingService.java:59 defines the map with only "EU"/"US" keys, so get("SKU-1") is null and PricingService.java:55 unboxes it
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Change PricingService.java:53 to REGION_TAX_PERCENT.getOrDefault(region, 10) (look up by region, as the clean branch does)
Regression test: GET /pricing/regional?sku=SKU-1&quantity=1&region=EU returns HTTP 200 with 10% EU tax applied, and region=US returns 5%
ROOT_CAUSE: trace.demoapp.service.PricingService#priceRegionalCents
