---
bug: B09
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.PricingService#priceCents
budget: tokens_in=63900 tokens_out=7154 cost_usd=0.3987 tool_calls=33
budget_exceeded: false
wall_ms: 44388
tool_calls_log:
  - get_failure_point({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 61c098d8-015d-4f97-9094-bb828336d6b0 has no uncaught exception","
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_source({"class_name":"trace.demoapp.PricingConfig"}) -> {"summary":"source for trace.demoapp.PricingConfig# at PricingConfig.java:1","items":[{"symbol_id":"trace.demoapp.Pricin
  - get_source({"class_name":"trace.demoapp.SeededBugs"}) -> {"summary":"source for trace.demoapp.SeededBugs# at SeededBugs.java:1","items":[{"symbol_id":"trace.demoapp.SeededBugs",
  - get_source({"class_name":"trace.demoapp.DemoApplication"}) -> {"summary":"source for trace.demoapp.DemoApplication# at DemoApplication.java:1","items":[{"symbol_id":"trace.demoapp.De
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":11}) -> {"summary":"2 slice events from event 11 (depth <= 20)","items":[{"event_id":11,"kind":"call","invocation_id":6,"where":
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":3}) -> {"summary":"2 slice events from event 3 (depth <= 20)","items":[{"event_id":3,"kind":"call","invocation_id":2,"where":"t
  - get_source({"class_name":"trace.demoapp.PricingController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PricingController under the configured sou
  - get_source({"class_name":"trace.demoapp.PricingService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PricingService under the configured source
  - get_source({"symbol_id":"trace.demoapp.PricingController#pricing"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PricingController under the configured sou
  - get_source({"class_name":"trace.demoapp.DataSeeder"}) -> {"summary":"source for trace.demoapp.DataSeeder# at DataSeeder.java:1","items":[{"symbol_id":"trace.demoapp.DataSeeder",
  - get_source({"class_name":"trace.demoapp.Pricing"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.Pricing under the configured source roots"
  - get_source({"class_name":"trace.demoapp.OrderController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.OrderController under the configured sourc
  - get_source({"class_name":"trace.demoapp.InventoryController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.InventoryController under the configured s
  - get_source({"class_name":"trace.demoapp.PriceController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PriceController under the configured sourc
  - get_source({"class_name":"trace.demoapp.QuoteService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.QuoteService under the configured source r
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":2,"depth":10}) -> {"summary":"2 invocations under 2 (depth <= 10)","items":[{"invocation_id":2,"parent_invocation_id":1,"call_event_id":3,
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":5}) -> {"summary":"1 invocations under 5 (depth <= 3)","items":[{"invocation_id":5,"parent_invocation_id":1,"call_event_id":9,"
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":9}) -> {"summary":"2 slice events from event 9 (depth <= 20)","items":[{"event_id":9,"kind":"call","invocation_id":5,"where":"t
  - get_source({"symbol_id":"trace.demoapp.PricingConfig#discountPolicy"}) -> {"summary":"source for trace.demoapp.PricingConfig#discountPolicy at PricingConfig.java:16","items":[{"symbol_id":"trace
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":7,"max_depth":50}) -> {"summary":"2 slice events from event 7 (depth <= 50)","items":[{"event_id":7,"kind":"call","invocation_id":4,"where":"t
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":7}) -> {"summary":"2 invocations under 7 (depth <= 3)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.we
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":10}) -> {"summary":"9 invocations under 10 (depth <= 3)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":2
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":20}) -> {"summary":"3 invocations under 20 (depth <= 3)","items":[{"invocation_id":20,"call_event_id":43,"where":"trace.demoapp.
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":20}) -> {"summary":"1 slice events from event 20 (depth <= 20)","items":[{"event_id":20,"kind":"call","invocation_id":9,"where":
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":9,"depth":10}) -> {"summary":"11 invocations under 9 (depth <= 10)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.
  - get_call_subtree({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","invocation_id":21,"depth":10}) -> {"summary":"2 invocations under 21 (depth <= 10)","items":[{"invocation_id":21,"parent_invocation_id":20,"call_event_id"
  - get_source({"class_name":"trace.demoapp.service.PricingService"}) -> {"summary":"source for trace.demoapp.service.PricingService# at PricingService.java:1","items":[{"symbol_id":"trace.demo
  - get_source({"class_name":"trace.demoapp.web.PricingController"}) -> {"summary":"source for trace.demoapp.web.PricingController# at PricingController.java:1","items":[{"symbol_id":"trace.de
  - get_source({"class_name":"trace.demoapp.service.PricingService","method_name":"priceCents"}) -> {"summary":"source for trace.demoapp.service.PricingService#priceCents at PricingService.java:29","items":[{"symbol_id":
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":44}) -> {"summary":"2 slice events from event 44 (depth <= 20)","items":[{"event_id":44,"kind":"call","invocation_id":21,"where"
  - trace_back({"run_id":"61c098d8-015d-4f97-9094-bb828336d6b0","target_id":21}) -> {"summary":"2 slice events from event 21 (depth <= 20)","items":[{"event_id":21,"kind":"call","invocation_id":10,"where"
---

Root cause: Singleton `PricingService` caches the first request's total in shared field `lastTotalCents` and returns it for the next request instead of computing that SKU's price (unconfirmed)
Evidence: First call invocation_id=9 `PricingController#price` -> invocation_id=10 `PricingService#priceCents` (call_event_id=21) runs full computation (`unitPriceCents`, `DiscountPolicy#thresholdCents`, `TaxService#taxFor`); `PricingService.java:19` declares `private Long lastTotalCents` on the `@Service`, `PricingService.java:31` in `priceCents` returns `lastTotalCents` when `seeded.active("b09") && lastTotalCents != null`, and `PricingService.java:41` stores `lastTotalCents = total`; second call invocation_id=20 -> invocation_id=21 `priceCents` (call_event_id=44) contains only invocation_id=22 `SeededBugs#active` (call_event_id=45) with no product/tax computation, matching the early return and the observed SKU-2 response of 1100 (SKU-1's total; `DataSeeder.java` seeds SKU-1=1000, SKU-2=2500)
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Remove the shared `lastTotalCents` field and both b09 branches in `PricingService#priceCents` (early return at line 31 and store at line 41) so every call computes from its own SKU/quantity
Regression test: In one app instance, call `GET /pricing?sku=SKU-1&quantity=1` then `GET /pricing?sku=SKU-2&quantity=1` and assert the second returns SKU-2's own total (2750), not 1100
ROOT_CAUSE: trace.demoapp.service.PricingService#priceCents
