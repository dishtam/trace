---
bug: B01
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.OrderService#placeOrder
budget: tokens_in=11398 tokens_out=2402 cost_usd=0.0936 tool_calls=5
budget_exceeded: false
wall_ms: 17003
tool_calls_log:
  - get_failure_point({"run_id":"c089c3a7-9636-4b8f-9fb5-1a8b0b535051"}) -> {"summary":"java.lang.NullPointerException at trace.demoapp.service.OrderService#placeOrder; 2 frames","items":[{"event_
  - trace_back({"run_id":"c089c3a7-9636-4b8f-9fb5-1a8b0b535051","target_id":28}) -> {"summary":"3 slice events from event 28 (depth <= 20)","items":[{"event_id":28,"kind":"throw","invocation_id":12,"where
  - get_call_subtree({"run_id":"c089c3a7-9636-4b8f-9fb5-1a8b0b535051","invocation_id":12}) -> {"summary":"2 invocations under 12 (depth <= 3)","items":[{"invocation_id":12,"parent_invocation_id":9,"call_event_id":2
  - get_call_subtree({"run_id":"c089c3a7-9636-4b8f-9fb5-1a8b0b535051","invocation_id":9}) -> {"summary":"5 invocations under 9 (depth <= 3)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.we
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"placeOrder"}) -> {"summary":"source for trace.demoapp.service.OrderService#placeOrder at OrderService.java:26","items":[{"symbol_id":"tra
---

Root cause: In `OrderService#placeOrder`, under b01 the result of `products.findById(sku).orElse(null)` is dereferenced unchecked, so an unknown SKU yields a null `product` and throws NullPointerException (unconfirmed)
Evidence: POST /orders UNKNOWN-SKU -> `OrderController#place` call event_id 20/invocation_id 9 -> `OrderService#placeOrder` call event_id 25/invocation_id 12 -> `SeededBugs#active` invocation_id 13, then OrderService.java:30-31 `Product product = products.findById(sku).orElse(null); long unitPriceCents = product.priceCents;` -> throw event_id 28 `NullPointerException: Cannot read field "priceCents" because "product" is null` at OrderService.java:31
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In `placeOrder`, replace `orElse(null)` dereference with `orElseThrow(...)` for an unknown SKU (or null-check before reading `priceCents`)
Regression test: Call `placeOrder("UNKNOWN-SKU", 1)` / POST /orders with unknown SKU and assert a controlled not-found error instead of HTTP 500 NullPointerException
ROOT_CAUSE: trace.demoapp.service.OrderService#placeOrder
