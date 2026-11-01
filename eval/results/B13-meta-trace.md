---
bug: B13
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.OrderService#placeOrder
budget: tokens_in=57470 tokens_out=5595 cost_usd=0.3418 tool_calls=27
budget_exceeded: false
wall_ms: 42853
tool_calls_log:
  - get_failure_point({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 061faf1a-9b7b-4113-8838-31907ac47318 has no uncaught exception","
  - get_call_subtree({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - get_source({"class_name":"trace.demoapp.DemoApplication"}) -> {"summary":"source for trace.demoapp.DemoApplication# at DemoApplication.java:1","items":[{"symbol_id":"trace.demoapp.De
  - get_source({"class_name":"trace.demoapp.DataSeeder"}) -> {"summary":"source for trace.demoapp.DataSeeder# at DataSeeder.java:1","items":[{"symbol_id":"trace.demoapp.DataSeeder",
  - get_source({"class_name":"trace.demoapp.SeededBugs"}) -> {"summary":"source for trace.demoapp.SeededBugs# at SeededBugs.java:1","items":[{"symbol_id":"trace.demoapp.SeededBugs",
  - trace_back({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","target_id":11}) -> {"summary":"2 slice events from event 11 (depth <= 20)","items":[{"event_id":11,"kind":"call","invocation_id":6,"where":
  - get_source({"class_name":"trace.demoapp.OrderController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.OrderController under the configured sourc
  - get_source({"class_name":"trace.demoapp.OrderService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.OrderService under the configured source r
  - get_source({"class_name":"trace.demoapp.ConfirmationService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.ConfirmationService under the configured s
  - get_source({"class_name":"trace.demoapp.OrderConfirmationService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.OrderConfirmationService under the configu
  - get_source({"symbol_id":"trace.demoapp.DemoApplication#main"}) -> {"summary":"source for trace.demoapp.DemoApplication#main at DemoApplication.java:14","items":[{"symbol_id":"trace.demoa
  - get_source({"symbol_id":"trace.demoapp.PricingConfig#discountPolicy"}) -> {"summary":"source for trace.demoapp.PricingConfig#discountPolicy at PricingConfig.java:16","items":[{"symbol_id":"trace
  - trace_back({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","target_id":2,"max_depth":50}) -> {"summary":"1 slice events from event 2 (depth <= 50)","items":[{"event_id":2,"kind":"call","invocation_id":1,"where":"t
  - get_call_subtree({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","invocation_id":2,"depth":10}) -> {"summary":"2 invocations under 2 (depth <= 10)","items":[{"invocation_id":2,"parent_invocation_id":1,"call_event_id":3,
  - get_call_subtree({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","invocation_id":5,"depth":10}) -> {"summary":"1 invocations under 5 (depth <= 10)","items":[{"invocation_id":5,"parent_invocation_id":1,"call_event_id":9,
  - get_call_subtree({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","invocation_id":6,"depth":10}) -> {"summary":"1 invocations under 6 (depth <= 10)","items":[{"invocation_id":6,"parent_invocation_id":1,"call_event_id":11
  - get_call_subtree({"run_id":"061faf1a-9b7b-4113-8838-31907ac47318","invocation_id":3,"depth":10}) -> {"summary":"1 invocations under 3 (depth <= 10)","items":[{"invocation_id":3,"parent_invocation_id":2,"call_event_id":4,
  - get_source({"class_name":"trace.demoapp.PricingConfig"}) -> {"summary":"source for trace.demoapp.PricingConfig# at PricingConfig.java:1","items":[{"symbol_id":"trace.demoapp.Pricin
  - get_source({"class_name":"trace.demoapp.web.OrderController"}) -> {"summary":"source for trace.demoapp.web.OrderController# at OrderController.java:1","items":[{"symbol_id":"trace.demoap
  - get_source({"class_name":"trace.demoapp.order.OrderService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.order.OrderService under the configured so
  - get_source({"class_name":"trace.demoapp.orders.OrderController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.orders.OrderController under the configure
  - get_source({"class_name":"trace.demoapp.service.OrderService"}) -> {"summary":"source for trace.demoapp.service.OrderService# at OrderService.java:1","items":[{"symbol_id":"trace.demoapp.
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"placeOrder"}) -> {"summary":"source for trace.demoapp.service.OrderService#placeOrder at OrderService.java:26","items":[{"symbol_id":"tra
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"confirmAsync"}) -> {"summary":"source for trace.demoapp.service.OrderService#confirmAsync at OrderService.java:39","items":[{"symbol_id":"t
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"confirmationStatus"}) -> {"summary":"source for trace.demoapp.service.OrderService#confirmationStatus at OrderService.java:65","items":[{"symbol_
  - get_source({"class_name":"trace.demoapp.web.OrderController","method_name":"confirmation"}) -> {"summary":"source for trace.demoapp.web.OrderController#confirmation at OrderController.java:57","items":[{"symbol_id":
---

Root cause: In trace.demoapp.service.OrderService#placeOrder, the saved order id is not propagated to the async confirmation (null is passed when b13 is active), so confirmAsync fails before recording CONFIRMED and the status remains NONE (unconfirmed)
Evidence: Run 061faf1a-9b7b-4113-8838-31907ac47318 has no uncaught exception (get_failure_point: FAILURE_NOT_FOUND, consistent with a swallowed async failure); GET confirmation at OrderController.java:57 delegates to OrderService#confirmationStatus at OrderService.java:65, which returns "NONE" when no entry exists; OrderService#confirmAsync only calls confirmations.put(orderId, "CONFIRMED") after rejecting a null id; OrderService#placeOrder at OrderService.java:26 contains `confirmAsync(seeded.active("b13") ? null : saved.id);` with comment "SEEDED-B13 ... the saved order id is not propagated to the async confirmation"
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In placeOrder, always pass the saved id: `confirmAsync(saved.id);`
Regression test: Place an order (SKU-1, quantity 1) and poll GET /orders/{id}/confirmation for a few seconds, asserting it becomes CONFIRMED rather than staying NONE
ROOT_CAUSE: trace.demoapp.service.OrderService#placeOrder
