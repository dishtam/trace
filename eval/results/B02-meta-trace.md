---
bug: B02
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.TaxService#taxFor
budget: tokens_in=11704 tokens_out=2957 cost_usd=0.1060 tool_calls=5
budget_exceeded: false
wall_ms: 16362
tool_calls_log:
  - get_failure_point({"run_id":"827629c4-c373-4d47-ac64-42f331d75a98"}) -> {"summary":"java.lang.NullPointerException at trace.demoapp.service.PricingService#priceCents; 4 frames","items":[{"even
  - trace_back({"run_id":"827629c4-c373-4d47-ac64-42f331d75a98","target_id":42}) -> {"summary":"5 slice events from event 42 (depth <= 20)","items":[{"event_id":42,"kind":"throw","invocation_id":15,"where
  - get_call_subtree({"run_id":"827629c4-c373-4d47-ac64-42f331d75a98","invocation_id":15}) -> {"summary":"7 invocations under 15 (depth <= 3)","items":[{"invocation_id":15,"parent_invocation_id":14,"call_event_id":
  - get_source({"class_name":"trace.demoapp.service.PricingService","method_name":"priceCents"}) -> {"summary":"source for trace.demoapp.service.PricingService#priceCents at PricingService.java:29","items":[{"symbol_id":
  - get_source({"class_name":"trace.demoapp.service.TaxService","method_name":"taxFor"}) -> {"summary":"source for trace.demoapp.service.TaxService#taxFor at TaxService.java:27","items":[{"symbol_id":"trace.demoa
---

Root cause: TaxService.taxFor returns null instead of 0 for zero-rated SKU-2, and PricingService.priceCents unboxes that null to long, causing the NullPointerException (unconfirmed)
Evidence: get_failure_point event_id=42, invocation_id=15: NPE at trace.demoapp.service.PricingService#priceCents because TaxService.taxFor(String,long) returned null; call subtree invocation_id=20 shows TaxService#taxFor returning under PricingService invocation_id=15; TaxService.java:27-30 returns seeded.active("b02") ? null : 0L for SKU-2; PricingService.java:39 does long taxAmount = tax.taxFor(sku, discounted)
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In TaxService.taxFor, return 0L instead of null for SKU-2 (zero-rated)
Regression test: POST /orders {"sku":"SKU-2","quantity":1} should return HTTP 200 with zero tax instead of HTTP 500, and TaxService.taxFor("SKU-2", anySubtotal) should return 0L not null
ROOT_CAUSE: trace.demoapp.service.TaxService#taxFor
