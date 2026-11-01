---
bug: B18
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.PricingService#priceRegionalCents
budget: tokens_in=8272 tokens_out=2199 cost_usd=0.0771 tool_calls=5
budget_exceeded: false
wall_ms: 12438
tool_calls_log:
  - get_failure_point({"run_id":"f7d8c53c-6386-42c3-a818-195c71dd458d"}) -> {"summary":"java.lang.NullPointerException at trace.demoapp.service.PricingService#priceRegionalCents; 2 frames","items"
  - trace_back({"run_id":"f7d8c53c-6386-42c3-a818-195c71dd458d","target_id":28}) -> {"summary":"3 slice events from event 28 (depth <= 20)","items":[{"event_id":28,"kind":"throw","invocation_id":10,"where
  - get_source({"class_name":"trace.demoapp.service.PricingService","method_name":"priceRegionalCents"}) -> {"summary":"source for trace.demoapp.service.PricingService#priceRegionalCents at PricingService.java:46","items":[{"sym
  - get_call_subtree({"run_id":"f7d8c53c-6386-42c3-a818-195c71dd458d","invocation_id":10}) -> {"summary":"4 invocations under 10 (depth <= 3)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":2
  - get_call_subtree({"run_id":"f7d8c53c-6386-42c3-a818-195c71dd458d","invocation_id":9}) -> {"summary":"5 invocations under 9 (depth <= 3)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.we
---

Root cause: In PricingService.priceRegionalCents, the regional tax rate is looked up with `REGION_TAX_PERCENT.get(sku)` instead of by `region`; for sku=SKU-1 this returns null and unboxing `percent` throws NPE (unconfirmed)
Evidence: get_failure_point event_id=28 NPE "percent is null" at trace.demoapp.service.PricingService#priceRegionalCents (invocation_id=10), called from PricingController#priceRegional event_id=20; source PricingService.java:46 shows `Integer percent = seeded.active("b18") ? REGION_TAX_PERCENT.get(sku) : REGION_TAX_PERCENT.getOrDefault(region, 10); long taxAmount = discounted * percent / 100;` with `REGION_TAX_PERCENT = Map.of("EU", 10, "US", 5)` keyed by region, not SKU
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Replace the sku lookup with `REGION_TAX_PERCENT.getOrDefault(region, 10)` in priceRegionalCents
Regression test: Call `GET /pricing/regional?sku=SKU-1&quantity=1&region=EU` (and/or unit-test priceRegionalCents with a sku that is not a region key) and expect HTTP 200 with EU 10% tax rather than HTTP 500 NPE
ROOT_CAUSE: trace.demoapp.service.PricingService#priceRegionalCents
