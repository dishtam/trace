---
bug: B17
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.web.PricingController#priceCallback
budget: tokens_in=28885 tokens_out=5151 cost_usd=0.2186 tool_calls=20
budget_exceeded: false
wall_ms: 32676
tool_calls_log:
  - get_failure_point({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run ceab94da-fe95-4d20-9ebf-2b51e31b5408 has no uncaught exception","
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - get_source({"class_name":"trace.demoapp.SeededBugs","method_name":"active"}) -> {"summary":"source for trace.demoapp.SeededBugs#active at SeededBugs.java:8","items":[{"symbol_id":"trace.demoapp.Seeded
  - get_source({"class_name":"trace.demoapp.PricingConfig"}) -> {"summary":"source for trace.demoapp.PricingConfig# at PricingConfig.java:1","items":[{"symbol_id":"trace.demoapp.Pricin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":7}) -> {"summary":"2 invocations under 7 (depth <= 3)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.we
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":8}) -> {"summary":"1 invocations under 8 (depth <= 3)","items":[{"invocation_id":8,"parent_invocation_id":7,"call_event_id":16,
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":10}) -> {"summary":"1 invocations under 10 (depth <= 3)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":2
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":20}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 20 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":9}) -> {"summary":"2 invocations under 9 (depth <= 3)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.we
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":11}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 11 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":12}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 12 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":13}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 13 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":14}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 14 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":15}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 15 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":16}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 16 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_call_subtree({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","invocation_id":17}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 17 in run ceab94da-fe95-4d20-9ebf-2b51e31b5408","hin
  - get_source({"class_name":"trace.demoapp.web.PricingController","method_name":"priceCallback"}) -> {"summary":"source for trace.demoapp.web.PricingController#priceCallback at PricingController.java:45","items":[{"symbol
  - trace_back({"run_id":"ceab94da-fe95-4d20-9ebf-2b51e31b5408","target_id":20}) -> {"summary":"1 slice events from event 20 (depth <= 20)","items":[{"event_id":20,"kind":"call","invocation_id":9,"where":
  - get_source({"class_name":"trace.demoapp.web.PricingController"}) -> {"summary":"source for trace.demoapp.web.PricingController# at PricingController.java:1","items":[{"symbol_id":"trace.de
---

Root cause: PricingController#priceCallback reads `total_cents` when profile b17 is active, but the sender emits `totalCents`, so the lookup returns null and `accepted` defaults to 0 (unconfirmed)
Evidence: Bug payload sends `totalCents:1234`; run ceab94da-fe95-4d20-9ebf-2b51e31b5408 completed with no uncaught exception; invocation_id 9 / call_event_id 20 is trace.demoapp.web.PricingController#priceCallback, with invocation_id 10 / call_event_id 21 trace.demoapp.SeededBugs#active beneath it; PricingController.java:45 shows `seeded.active("b17") ? "total_cents" : "totalCents"`, then `payload.get(field)` and `value instanceof Number ? ... : 0`, matching observed HTTP 200 {"acceptedTotalCents":0}
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In priceCallback, read `totalCents` instead of `total_cents` (remove the b17 field rename).
Regression test: POST /pricing/callback with {"sku":"SKU-1","totalCents":1234} under profile b17 and assert {"acceptedTotalCents":1234}.
ROOT_CAUSE: trace.demoapp.web.PricingController#priceCallback
