---
bug: B14
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.TaxService#percent
budget: tokens_in=49139 tokens_out=6849 cost_usd=0.3335 tool_calls=28
budget_exceeded: false
wall_ms: 45911
tool_calls_log:
  - get_failure_point({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 01c56e80-9970-4dff-a4cb-b1f327a881f5 has no uncaught exception","
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":2,"depth":5}) -> {"summary":"2 invocations under 2 (depth <= 5)","items":[{"invocation_id":2,"parent_invocation_id":1,"call_event_id":3,"
  - get_source({"class_name":"trace.demoapp.PricingConfig"}) -> {"summary":"source for trace.demoapp.PricingConfig# at PricingConfig.java:1","items":[{"symbol_id":"trace.demoapp.Pricin
  - get_source({"class_name":"trace.demoapp.DemoApplication"}) -> {"summary":"source for trace.demoapp.DemoApplication# at DemoApplication.java:1","items":[{"symbol_id":"trace.demoapp.De
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":5,"depth":5}) -> {"summary":"1 invocations under 5 (depth <= 5)","items":[{"invocation_id":5,"parent_invocation_id":1,"call_event_id":9,"
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":3,"depth":5}) -> {"summary":"1 invocations under 3 (depth <= 5)","items":[{"invocation_id":3,"parent_invocation_id":2,"call_event_id":4,"
  - get_source({"class_name":"trace.demoapp.SeededBugs"}) -> {"summary":"source for trace.demoapp.SeededBugs# at SeededBugs.java:1","items":[{"symbol_id":"trace.demoapp.SeededBugs",
  - get_source({"class_name":"trace.demoapp.PricingController"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PricingController under the configured sou
  - get_source({"class_name":"trace.demoapp.PricingService"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PricingService under the configured source
  - get_source({"class_name":"trace.demoapp.PricingProperties"}) -> TOOL_ERROR: {"code":"SOURCE_NOT_FOUND","message":"no source for trace.demoapp.PricingProperties under the configured sou
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":11}) -> {"summary":"2 slice events from event 11 (depth <= 20)","items":[{"event_id":11,"kind":"call","invocation_id":6,"where":
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":2}) -> {"summary":"1 slice events from event 2 (depth <= 20)","items":[{"event_id":2,"kind":"call","invocation_id":1,"where":"t
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":3}) -> {"summary":"2 slice events from event 3 (depth <= 20)","items":[{"event_id":3,"kind":"call","invocation_id":2,"where":"t
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":9}) -> {"summary":"2 slice events from event 9 (depth <= 20)","items":[{"event_id":9,"kind":"call","invocation_id":5,"where":"t
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":7}) -> {"summary":"2 invocations under 7 (depth <= 3)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.we
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":10}) -> {"summary":"9 invocations under 10 (depth <= 3)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":2
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":20}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 20 in run 01c56e80-9970-4dff-a4cb-b1f327a881f5","hin
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":100}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 100 in run 01c56e80-9970-4dff-a4cb-b1f327a881f5","hi
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":10,"depth":10}) -> {"summary":"10 invocations under 10 (depth <= 10)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id"
  - get_call_subtree({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","invocation_id":9,"depth":10}) -> {"summary":"11 invocations under 9 (depth <= 10)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.
  - get_source({"class_name":"trace.demoapp.service.TaxService"}) -> {"summary":"source for trace.demoapp.service.TaxService# at TaxService.java:1","items":[{"symbol_id":"trace.demoapp.serv
  - get_source({"class_name":"trace.demoapp.service.PricingService"}) -> {"summary":"source for trace.demoapp.service.PricingService# at PricingService.java:1","items":[{"symbol_id":"trace.demo
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":33}) -> {"summary":"6 slice events from event 33 (depth <= 20)","items":[{"event_id":33,"kind":"call","invocation_id":18,"where"
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":32}) -> {"summary":"5 slice events from event 32 (depth <= 20)","items":[{"event_id":32,"kind":"call","invocation_id":17,"where"
  - trace_back({"run_id":"01c56e80-9970-4dff-a4cb-b1f327a881f5","target_id":31}) -> {"summary":"4 slice events from event 31 (depth <= 20)","items":[{"event_id":31,"kind":"call","invocation_id":16,"where"
  - get_source({"class_name":"trace.demoapp.service.TaxService","method_name":"percent"}) -> {"summary":"source for trace.demoapp.service.TaxService#percent at TaxService.java:7","items":[{"symbol_id":"trace.demoa
---

Root cause: TaxService#percent reads the wrong environment key pricing.vat.percent with a 25% fallback when b14 is active, instead of pricing.tax.percent, so tax is applied at 25% instead of 10% (unconfirmed)
Evidence: trace_back from event 33 shows PricingController#price (event 20, invocation 9) -> PricingService#priceCents (event 21, invocation 10) -> TaxService#taxFor (event 30, invocation 15) -> TaxService#taxCents (event 31, invocation 16) -> TaxService#percent (event 32, invocation 17) -> SeededBugs#active (event 33, invocation 18); TaxService.java:22-23 shows the b14 branch returning pricing.vat.percent/"25" while the clean path returns pricing.tax.percent/"10"
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In TaxService#percent, change the b14 branch to read pricing.tax.percent with default "10" (or remove the seeded branch entirely)
Regression test: With pricing.tax.percent=10 and pricing.vat.percent unset, GET /pricing?sku=SKU-1&quantity=1 should return totalCents 1100, not 1250
ROOT_CAUSE: trace.demoapp.service.TaxService#percent
