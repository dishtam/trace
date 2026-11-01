---
bug: B12
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: (none parsed)
budget: tokens_in=84536 tokens_out=10535 cost_usd=0.5488 tool_calls=40
budget_exceeded: false
wall_ms: 58822
tool_calls_log:
  - get_failure_point({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 0ad5db9a-5f00-4da6-94da-c3161701c0ce has no uncaught exception","
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - trace_back({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","target_id":11}) -> {"summary":"2 slice events from event 11 (depth <= 20)","items":[{"event_id":11,"kind":"call","invocation_id":6,"where":
  - get_source({"class_name":"trace.demoapp.SeededBugs","method_name":"active"}) -> {"summary":"source for trace.demoapp.SeededBugs#active at SeededBugs.java:8","items":[{"symbol_id":"trace.demoapp.Seeded
  - get_source({"class_name":"trace.demoapp.DemoApplication"}) -> {"summary":"source for trace.demoapp.DemoApplication# at DemoApplication.java:1","items":[{"symbol_id":"trace.demoapp.De
  - get_source({"class_name":"trace.demoapp.DataSeeder","method_name":"seed"}) -> {"summary":"source for trace.demoapp.DataSeeder#seed at DataSeeder.java:11","items":[{"symbol_id":"trace.demoapp.DataSee
  - get_source({"class_name":"trace.demoapp.PricingConfig","method_name":"discountPolicy"}) -> {"summary":"source for trace.demoapp.PricingConfig#discountPolicy at PricingConfig.java:16","items":[{"symbol_id":"trace
  - trace_back({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","target_id":2,"max_depth":50}) -> {"summary":"1 slice events from event 2 (depth <= 50)","items":[{"event_id":2,"kind":"call","invocation_id":1,"where":"t
  - get_source({"class_name":"trace.demoapp.OrderService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.OrderService under the configured source r
  - get_source({"class_name":"trace.demoapp.OrderController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.OrderController under the configured sourc
  - get_source({"class_name":"trace.demoapp.InventoryService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.InventoryService under the configured sour
  - get_source({"class_name":"trace.demoapp.StockService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.StockService under the configured source r
  - get_source({"symbol_id":"trace.demoapp.DataSeeder#seed"}) -> {"summary":"source for trace.demoapp.DataSeeder#seed at DataSeeder.java:11","items":[{"symbol_id":"trace.demoapp.DataSee
  - get_source({"symbol_id":"trace.demoapp.PricingConfig#discountPolicy"}) -> {"summary":"source for trace.demoapp.PricingConfig#discountPolicy at PricingConfig.java:16","items":[{"symbol_id":"trace
  - get_source({"symbol_id":"trace.demoapp.DemoApplication#orderExecutor"}) -> {"summary":"source for trace.demoapp.DemoApplication#orderExecutor at DemoApplication.java:17","items":[{"symbol_id":"tr
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":2,"depth":10}) -> {"summary":"2 invocations under 2 (depth <= 10)","items":[{"invocation_id":2,"parent_invocation_id":1,"call_event_id":3,
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":5,"depth":10}) -> {"summary":"1 invocations under 5 (depth <= 10)","items":[{"invocation_id":5,"parent_invocation_id":1,"call_event_id":9,
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":6,"depth":10}) -> {"summary":"1 invocations under 6 (depth <= 10)","items":[{"invocation_id":6,"parent_invocation_id":1,"call_event_id":11
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":3,"depth":10}) -> {"summary":"1 invocations under 3 (depth <= 10)","items":[{"invocation_id":3,"parent_invocation_id":2,"call_event_id":4,
  - get_source({"class_name":"trace.demoapp.StockLevel"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.StockLevel under the configured source roo
  - get_source({"class_name":"trace.demoapp.StockLevelRepository"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.StockLevelRepository under the configured 
  - get_source({"class_name":"trace.demoapp.ProductRepository"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.ProductRepository under the configured sou
  - get_source({"class_name":"trace.demoapp.Product"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.Product under the configured source roots"
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":7,"depth":10}) -> {"summary":"2 invocations under 7 (depth <= 10)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.w
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":10,"depth":10}) -> {"summary":"1 invocations under 10 (depth <= 10)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":20,"depth":10}) -> {"summary":"24 invocations under 20 (depth <= 10)","items":[{"invocation_id":20,"parent_invocation_id":13,"call_event_id
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":100,"depth":10}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 100 in run 0ad5db9a-5f00-4da6-94da-c3161701c0ce","hi
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":7,"depth":10}) -> {"summary":"2 invocations under 7 (depth <= 10)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.w
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":9,"depth":10}) -> {"summary":"4 invocations under 9 (depth <= 10)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.w
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":13,"depth":10}) -> {"summary":"28 invocations under 13 (depth <= 10)","items":[{"invocation_id":13,"call_event_id":30,"where":"trace.demoap
  - get_call_subtree({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","invocation_id":14,"depth":10}) -> {"summary":"28 invocations under 14 (depth <= 10)","items":[{"invocation_id":14,"call_event_id":32,"where":"trace.demoap
  - get_source({"class_name":"trace.demoapp.service.InventoryService","method_name":"decrement"}) -> {"summary":"source for trace.demoapp.service.InventoryService#decrement at InventoryService.java:36","items":[{"symbol_i
  - get_source({"class_name":"trace.demoapp.service.InventoryService","method_name":"decrementChecked"}) -> {"summary":"source for trace.demoapp.service.InventoryService#decrementChecked at InventoryService.java:39","items":[{"s
  - get_source({"class_name":"trace.demoapp.service.InventoryService","method_name":"awaitRendezvous"}) -> {"summary":"source for trace.demoapp.service.InventoryService#awaitRendezvous at InventoryService.java:54","items":[{"sy
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"placeOrder"}) -> {"summary":"source for trace.demoapp.service.OrderService#placeOrder at OrderService.java:26","items":[{"symbol_id":"tra
  - get_source({"class_name":"trace.demoapp.service.InventoryService"}) -> {"summary":"source for trace.demoapp.service.InventoryService# at InventoryService.java:1","items":[{"symbol_id":"trace.
  - get_source({"class_name":"trace.demoapp.service.CheckoutService","method_name":"checkout"}) -> {"summary":"source for trace.demoapp.service.CheckoutService#checkout at CheckoutService.java:23","items":[{"symbol_id":
  - trace_back({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","target_id":97}) -> {"summary":"4 slice events from event 97 (depth <= 20)","items":[{"event_id":97,"kind":"call","invocation_id":50,"where"
  - trace_back({"run_id":"0ad5db9a-5f00-4da6-94da-c3161701c0ce","target_id":106}) -> {"summary":"5 slice events from event 106 (depth <= 20)","items":[{"event_id":106,"kind":"call","invocation_id":56,"wher
---

Root cause: Under b12, the stock decrement performs a non-atomic read-check-save, so two concurrent quantity-1 orders both read stock=1 and pass the availability check before either saves, allowing both orders to be placed for one unit (unconfirmed)
Evidence: get_failure_point reported no uncaught exception for run 0ad5db9a-5f00-4da6
