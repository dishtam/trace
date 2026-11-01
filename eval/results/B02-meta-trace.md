---
bug: B02
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.TaxService#taxFor
budget: tokens_in=9155 tokens_out=2934 cost_usd=0.0953 tool_calls=5
budget_exceeded: false
wall_ms: 15889
tool_calls_log:
  - get_failure_point({"run_id":"798e395e-05ca-4d8f-aa6c-befe4ddd5f42"}) -> {"summary":"java.lang.NullPointerException at trace.demoapp.service.PricingService#priceCents; 4 frames","items":[{"even
  - trace_back({"run_id":"798e395e-05ca-4d8f-aa6c-befe4ddd5f42","target_id":42}) -> {"summary":"5 slice events from event 42 (depth <= 20)","items":[{"event_id":42,"kind":"throw","invocation_id":15,"where
  - get_source({"class_name":"trace.demoapp.service.PricingService","method_name":"priceCents"}) -> {"summary":"source for trace.demoapp.service.PricingService#priceCents at PricingService.java:29","items":[{"symbol_id":
  - get_source({"class_name":"trace.demoapp.service.TaxService","method_name":"taxFor"}) -> {"summary":"source for trace.demoapp.service.TaxService#taxFor at TaxService.java:27","items":[{"symbol_id":"trace.demoa
  - get_call_subtree({"run_id":"798e395e-05ca-4d8f-aa6c-befe4ddd5f42","invocation_id":15}) -> {"summary":"7 invocations under 15 (depth <= 3)","items":[{"invocation_id":15,"parent_invocation_id":14,"call_event_id":
---

Root cause: trace.demoapp.service.TaxService#taxFor returns null instead of 0L for zero-rated SKU-2, and that null is unboxed by PricingService (unconfirmed)
Evidence: event 20 OrderController#place -> event 25 OrderService#placeOrder -> event 28 CheckoutService#checkout -> event 29/invocation 15 PricingService#priceCents -> invocation 20 TaxService#taxFor (call_event 38), where TaxService.java:27-30 returns `seeded.active("b02") ? null : 0L` for SKU-2 -> PricingService.java:39 `long taxAmount = tax.taxFor(sku, discounted);` unboxes it -> event 42 NullPointerException "return value of TaxService.taxFor(String, long) is null" at PricingService#priceCents
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In TaxService#taxFor, return 0L (not null) for SKU-2.
Regression test: POST /orders {"sku":"SKU-2","quantity":1} (or priceCents("SKU-2",1)) returns HTTP 200 with tax 0 and no NPE.
ROOT_CAUSE: trace.demoapp.service.TaxService#taxFor
