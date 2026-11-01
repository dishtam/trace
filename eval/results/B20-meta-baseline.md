---
bug: B20
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.CheckoutService#checkout
budget: tokens_in=14799 tokens_out=3806 cost_usd=0.1353 tool_calls=9
budget_exceeded: false
wall_ms: 18272
tool_calls_log:
  - read_file({"path":"src/main/java/trace/demoapp/service/CheckoutService.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/service/CheckoutService.java
  - read_file({"path":"src/main/java/trace/demoapp/service/OrderService.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/service/OrderService.java
  - read_file({"path":"src/main/java/trace/demoapp/web/OrderController.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/web/OrderController.java
  - list_files({}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java/trace/demoapp/service/CheckoutService.java"}) -> java/trace/demoapp/service/CheckoutService.java lines 1..53 of 53 package trace.demoapp.service;  import org.springframe
  - read_file({"path":"java/trace/demoapp/service/OrderService.java"}) -> java/trace/demoapp/service/OrderService.java lines 1..106 of 106 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/web/OrderController.java"}) -> java/trace/demoapp/web/OrderController.java lines 1..61 of 61 package trace.demoapp.web;  import org.springframework.web
  - read_file({"path":"java/trace/demoapp/service/PricingService.java"}) -> java/trace/demoapp/service/PricingService.java lines 1..73 of 73 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/service/InventoryService.java"}) -> java/trace/demoapp/service/InventoryService.java lines 1..66 of 66 package trace.demoapp.service;  import org.springfram
---

Failure: POST /orders with sku UNKNOWN returned HTTP 500 and logged a bare IllegalStateException at CheckoutService.java:31 instead of the original unknown-sku error.

Root cause: CheckoutService.checkout catches the RuntimeException from pricing and, when b20 is active, throws a new IllegalStateException without passing the caught failure as its cause, discarding the original "unknown sku UNKNOWN" IllegalArgumentException (unconfirmed)
Evidence: PricingService.java:32 throws IllegalArgumentException("unknown sku " + sku) for an unknown sku; CheckoutService.java:25 calls pricing.priceCents inside try, CheckoutService.java:29 catches RuntimeException failure, and CheckoutService.java:31 throws new IllegalStateException("checkout failed for " + sku) with no cause (CheckoutService.java:32 is the clean rethrow); the observed log shows only IllegalStateException at CheckoutService.java:31 via OrderService.java:34 and OrderController.java:20
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Change CheckoutService.java:31 to throw new IllegalStateException("checkout failed for " + sku, failure) so the original cause is preserved (or rethrow failure)
Regression test: POST /orders with {"sku":"UNKNOWN","quantity":1} and assert the surfaced/logged exception chain contains IllegalArgumentException "unknown sku UNKNOWN" as the cause
ROOT_CAUSE: trace.demoapp.service.CheckoutService#checkout
