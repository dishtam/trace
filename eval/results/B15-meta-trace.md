---
bug: B15
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.PricingConfig#discountPolicy
budget: tokens_in=29045 tokens_out=8279 cost_usd=0.2818 tool_calls=17
budget_exceeded: false
wall_ms: 49410
tool_calls_log:
  - get_failure_point({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 09ebeab6-4a76-4834-8fc7-1688d49e7170 has no uncaught exception","
  - get_call_subtree({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","invocation_id":0}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 0 in run 09ebeab6-4a76-4834-8fc7-1688d49e7170","hint
  - get_call_subtree({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - trace_back({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","target_id":11}) -> {"summary":"2 slice events from event 11 (depth <= 20)","items":[{"event_id":11,"kind":"call","invocation_id":6,"where":
  - trace_back({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","target_id":4}) -> {"summary":"3 slice events from event 4 (depth <= 20)","items":[{"event_id":4,"kind":"call","invocation_id":3,"where":"t
  - trace_back({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","target_id":3}) -> {"summary":"2 slice events from event 3 (depth <= 20)","items":[{"event_id":3,"kind":"call","invocation_id":2,"where":"t
  - get_source({"class_name":"trace.demoapp.PricingConfig","method_name":"discountPolicy"}) -> {"summary":"source for trace.demoapp.PricingConfig#discountPolicy at PricingConfig.java:16","items":[{"symbol_id":"trace
  - get_source({"class_name":"trace.demoapp.SeededBugs","method_name":"active"}) -> {"summary":"source for trace.demoapp.SeededBugs#active at SeededBugs.java:8","items":[{"symbol_id":"trace.demoapp.Seeded
  - get_source({"class_name":"trace.demoapp.DemoApplication","method_name":"main"}) -> {"summary":"source for trace.demoapp.DemoApplication#main at DemoApplication.java:14","items":[{"symbol_id":"trace.demoa
  - get_source({"class_name":"trace.demoapp.PricingConfig"}) -> {"summary":"source for trace.demoapp.PricingConfig# at PricingConfig.java:1","items":[{"symbol_id":"trace.demoapp.Pricin
  - get_call_subtree({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","invocation_id":2,"depth":10}) -> {"summary":"2 invocations under 2 (depth <= 10)","items":[{"invocation_id":2,"parent_invocation_id":1,"call_event_id":3,
  - get_call_subtree({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","invocation_id":5,"depth":10}) -> {"summary":"1 invocations under 5 (depth <= 10)","items":[{"invocation_id":5,"parent_invocation_id":1,"call_event_id":9,
  - get_call_subtree({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","invocation_id":4,"depth":10}) -> {"summary":"1 invocations under 4 (depth <= 10)","items":[{"invocation_id":4,"parent_invocation_id":1,"call_event_id":7,
  - get_source({"class_name":"trace.demoapp.PricingConfig$DiscountPolicy","method_name":"hashCode"}) -> {"summary":"source for trace.demoapp.PricingConfig$DiscountPolicy#hashCode at PricingConfig.java:1","items":[{"symbol_id
  - trace_back({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","target_id":7,"max_depth":50}) -> {"summary":"2 slice events from event 7 (depth <= 50)","items":[{"event_id":7,"kind":"call","invocation_id":4,"where":"t
  - trace_back({"run_id":"09ebeab6-4a76-4834-8fc7-1688d49e7170","target_id":9,"max_depth":50}) -> {"summary":"2 slice events from event 9 (depth <= 50)","items":[{"event_id":9,"kind":"call","invocation_id":5,"where":"t
---

Root cause: PricingConfig#discountPolicy returns DiscountPolicy(0) when the b15 seeded profile is active, lowering the bulk-discount threshold from 5000 to 0 so a 1000-cent subtotal is discounted (unconfirmed)
Evidence: get_failure_point reported no uncaught exception for run 09ebeab6-4a76-4834-8fc7-1688d49e7170; invocation_id 2/event_id 3 trace.demoapp.PricingConfig#discountPolicy called invocation_id 3/event_id 4 trace.demoapp.SeededBugs#active; PricingConfig.java:16 shows `if (seeded.active("b15")) return new DiscountPolicy(0);` before the normal `return new DiscountPolicy(testDiscount ? 0 : 5000);`, matching the observed discounted totalCents 990 instead of clean 1100
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Remove the b15 early return in PricingConfig#discountPolicy so threshold 0 is used only for the test-discount profile and 5000 otherwise
Regression test: With default profile, GET /pricing?sku=SKU-1&quantity=1 returns 1100, while a subtotal at/above 5000 still receives the discount
ROOT_CAUSE: trace.demoapp.PricingConfig#discountPolicy
