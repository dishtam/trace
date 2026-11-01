---
bug: B07
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.PricingService#updateSalePrice
budget: tokens_in=58101 tokens_out=7542 cost_usd=0.3832 tool_calls=30
budget_exceeded: false
wall_ms: 47662
tool_calls_log:
  - get_failure_point({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 7069f840-00f5-4c36-8431-d951d7b6c08e has no uncaught exception","
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - trace_back({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","target_id":11}) -> {"summary":"2 slice events from event 11 (depth <= 20)","items":[{"event_id":11,"kind":"call","invocation_id":6,"where":
  - get_source({"class_name":"trace.demoapp.DataSeeder","method_name":"seed"}) -> {"summary":"source for trace.demoapp.DataSeeder#seed at DataSeeder.java:11","items":[{"symbol_id":"trace.demoapp.DataSee
  - get_source({"class_name":"trace.demoapp.PricingConfig"}) -> {"summary":"source for trace.demoapp.PricingConfig# at PricingConfig.java:1","items":[{"symbol_id":"trace.demoapp.Pricin
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":5,"depth":10}) -> {"summary":"1 invocations under 5 (depth <= 10)","items":[{"invocation_id":5,"parent_invocation_id":1,"call_event_id":9,
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":2,"depth":10}) -> {"summary":"2 invocations under 2 (depth <= 10)","items":[{"invocation_id":2,"parent_invocation_id":1,"call_event_id":3,
  - get_source({"class_name":"trace.demoapp.SeededBugs","method_name":"active"}) -> {"summary":"source for trace.demoapp.SeededBugs#active at SeededBugs.java:8","items":[{"symbol_id":"trace.demoapp.Seeded
  - get_source({"class_name":"trace.demoapp.Product"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.Product under the configured source roots"
  - get_source({"class_name":"trace.demoapp.ProductRepository"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.ProductRepository under the configured sou
  - get_source({"class_name":"trace.demoapp.ProductController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.ProductController under the configured sou
  - get_source({"class_name":"trace.demoapp.DemoApplication"}) -> {"summary":"source for trace.demoapp.DemoApplication# at DemoApplication.java:1","items":[{"symbol_id":"trace.demoapp.De
  - get_source({"symbol_id":"trace.demoapp.ProductService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.ProductService under the configured source
  - get_source({"symbol_id":"trace.demoapp.ProductController#updateSalePrice"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.ProductController under the configured sou
  - get_source({"symbol_id":"trace.demoapp.Product"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.Product under the configured source roots"
  - trace_back({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","target_id":9}) -> {"summary":"2 slice events from event 9 (depth <= 20)","items":[{"event_id":9,"kind":"call","invocation_id":5,"where":"t
  - trace_back({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","target_id":3}) -> {"summary":"2 slice events from event 3 (depth <= 20)","items":[{"event_id":3,"kind":"call","invocation_id":2,"where":"t
  - trace_back({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","target_id":7}) -> {"summary":"2 slice events from event 7 (depth <= 20)","items":[{"event_id":7,"kind":"call","invocation_id":4,"where":"t
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":6,"depth":10}) -> {"summary":"1 invocations under 6 (depth <= 10)","items":[{"invocation_id":6,"parent_invocation_id":1,"call_event_id":11
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":7,"depth":10}) -> {"summary":"2 invocations under 7 (depth <= 10)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.w
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":8,"depth":10}) -> {"summary":"1 invocations under 8 (depth <= 10)","items":[{"invocation_id":8,"parent_invocation_id":7,"call_event_id":16
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":9,"depth":10}) -> {"summary":"5 invocations under 9 (depth <= 10)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.w
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":10,"depth":10}) -> {"summary":"1 invocations under 10 (depth <= 10)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":
  - get_source({"class_name":"trace.demoapp.web.PricingController","method_name":"updateSalePrice"}) -> {"summary":"source for trace.demoapp.web.PricingController#updateSalePrice at PricingController.java:29","items":[{"symb
  - get_source({"class_name":"trace.demoapp.service.PricingService","method_name":"updateSalePrice"}) -> {"summary":"source for trace.demoapp.service.PricingService#updateSalePrice at PricingService.java:62","items":[{"symbol
  - get_source({"class_name":"trace.demoapp.web.PricingController","method_name":"productView"}) -> {"summary":"source for trace.demoapp.web.PricingController#productView at PricingController.java:31","items":[{"symbol_i
  - trace_back({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","target_id":23}) -> {"summary":"2 slice events from event 23 (depth <= 20)","items":[{"event_id":23,"kind":"call","invocation_id":11,"where"
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":11,"depth":10}) -> {"summary":"2 invocations under 11 (depth <= 10)","items":[{"invocation_id":11,"parent_invocation_id":9,"call_event_id":
  - get_call_subtree({"run_id":"7069f840-00f5-4c36-8431-d951d7b6c08e","invocation_id":14,"depth":10}) -> {"summary":"3 invocations under 14 (depth <= 10)","items":[{"invocation_id":14,"call_event_id":31,"where":"trace.demoapp
---

Root cause: `PricingService.updateSalePrice` writes the requested sale price to `priceCents` (list price) instead of `salePriceCents` when profile b07 is active, leaving `salePriceCents` at 0 (unconfirmed)
Evidence: DataSeeder.java:11 seeds SKU-1 with price 1000; invocation 9 / call_event_id 20 `PricingController#updateSalePrice` (PricingController.java:29) calls invocation 11 / call_event_id 23 `PricingService#updateSalePrice`, which checks `SeededBugs#active` (invocation 12); PricingService.java:62 contains `if (seeded.active("b07")) product.priceCents = salePriceCents; else product.salePriceCents = salePriceCents;`; `productView` maps `priceCents`→`listPriceCents` and `salePriceCents`→`salePriceCents`, and the subsequent GET (invocation 14→15→16) shows the persisted 500/0 state
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In `PricingService#updateSalePrice`, always assign `product.salePriceCents = salePriceCents;` and never assign the sale price to `product.priceCents`
Regression test: Seed SKU-1 at 1000, PUT `/products/SKU-1/sale-price` with 500, then GET `/products/SKU-1` and assert `listPriceCents=1000` and `salePriceCents=500`
ROOT_CAUSE: trace.demoapp.service.PricingService#updateSalePrice
