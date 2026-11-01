---
bug: B17
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.web.PricingController#priceCallback
budget: tokens_in=7068 tokens_out=1776 cost_usd=0.0638 tool_calls=3
budget_exceeded: false
wall_ms: 9097
tool_calls_log:
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java\\trace\\demoapp\\web\\PricingController.java"}) -> java\trace\demoapp\web\PricingController.java lines 1..71 of 71 package trace.demoapp.web;  import org.springframework.w
  - read_file({"path":"java\\trace\\demoapp\\SeededBugs.java"}) -> java\trace\demoapp\SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
---

Root cause: PricingController reads `total_cents` under profile b17 while the sender emits `totalCents`, so the payload lookup returns null and is mapped to 0 (unconfirmed)
Evidence: POST /pricing/callback `{"sku":"SKU-1","totalCents":1234}` returned HTTP 200 `{"acceptedTotalCents":0}`; `java\trace\demoapp\web\PricingController.java:58-65` in `priceCallback` selects `"total_cents"` when `seeded.active("b17")` is true, then `payload.get(field)` is non-Number and `accepted` defaults to 0; `java\trace\demoapp\SeededBugs.java:19-21` shows `active("b17")` checks the b17 Spring profile
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In `PricingController#priceCallback`, always read the sender's field name `totalCents` (remove the b17 `total_cents` ternary).
Regression test: POST /pricing/callback with `{"sku":"SKU-1","totalCents":1234}` and assert HTTP 200 with `acceptedTotalCents` == 1234.
ROOT_CAUSE: trace.demoapp.web.PricingController#priceCallback
