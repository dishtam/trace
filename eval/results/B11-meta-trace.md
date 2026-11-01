---
bug: B11
runner: trace
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.OrderService#confirm
budget: tokens_in=26890 tokens_out=5923 cost_usd=0.2260 tool_calls=19
budget_exceeded: false
wall_ms: 35964
tool_calls_log:
  - get_failure_point({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825"}) -> TOOL_ERROR: {"code":"FAILURE_NOT_FOUND","message":"run 3e0cd069-d7fc-485e-b1c1-877805ce4825 has no uncaught exception","
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":0}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 0 in run 3e0cd069-d7fc-485e-b1c1-877805ce4825","hint
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":1}) -> {"summary":"6 invocations under 1 (depth <= 3)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.Dem
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":1,"depth":10}) -> {"summary":"6 invocations under 1 (depth <= 10)","items":[{"invocation_id":1,"call_event_id":2,"where":"trace.demoapp.De
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":7}) -> {"summary":"2 invocations under 7 (depth <= 3)","items":[{"invocation_id":7,"call_event_id":15,"where":"trace.demoapp.we
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":8}) -> {"summary":"1 invocations under 8 (depth <= 3)","items":[{"invocation_id":8,"parent_invocation_id":7,"call_event_id":16,
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":9}) -> {"summary":"4 invocations under 9 (depth <= 3)","items":[{"invocation_id":9,"call_event_id":20,"where":"trace.demoapp.we
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":10}) -> {"summary":"3 invocations under 10 (depth <= 3)","items":[{"invocation_id":10,"parent_invocation_id":9,"call_event_id":2
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":20}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 20 in run 3e0cd069-d7fc-485e-b1c1-877805ce4825","hin
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":50}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 50 in run 3e0cd069-d7fc-485e-b1c1-877805ce4825","hin
  - get_call_subtree({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","invocation_id":100}) -> TOOL_ERROR: {"code":"INVOCATION_NOT_FOUND","message":"no invocation 100 in run 3e0cd069-d7fc-485e-b1c1-877805ce4825","hi
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"confirm"}) -> {"summary":"source for trace.demoapp.service.OrderService#confirm at OrderService.java:18","items":[{"symbol_id":"trace.
  - get_source({"class_name":"trace.demoapp.service.OrderService","method_name":"confirmAsync"}) -> {"summary":"source for trace.demoapp.service.OrderService#confirmAsync at OrderService.java:39","items":[{"symbol_id":"t
  - get_source({"class_name":"trace.demoapp.web.OrderController","method_name":"confirm"}) -> {"summary":"source for trace.demoapp.web.OrderController#confirm at OrderController.java:52","items":[{"symbol_id":"trac
  - get_source({"class_name":"trace.demoapp.SeededBugs","method_name":"active"}) -> {"summary":"source for trace.demoapp.SeededBugs#active at SeededBugs.java:8","items":[{"symbol_id":"trace.demoapp.Seeded
  - trace_back({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","target_id":21}) -> {"summary":"2 slice events from event 21 (depth <= 20)","items":[{"event_id":21,"kind":"call","invocation_id":10,"where"
  - trace_back({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","target_id":22}) -> {"summary":"3 slice events from event 22 (depth <= 20)","items":[{"event_id":22,"kind":"call","invocation_id":11,"where"
  - trace_back({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","target_id":24}) -> {"summary":"3 slice events from event 24 (depth <= 20)","items":[{"event_id":24,"kind":"call","invocation_id":12,"where"
  - trace_back({"run_id":"3e0cd069-d7fc-485e-b1c1-877805ce4825","target_id":20}) -> {"summary":"1 slice events from event 20 (depth <= 20)","items":[{"event_id":20,"kind":"call","invocation_id":9,"where":
---

Failed: POST /orders/9999/confirm returned HTTP 200 "SUBMITTED" instead of HTTP 500, at trace.demoapp.web.OrderController#confirm.
Root cause: trace.demoapp.service.OrderService#confirm returns "SUBMITTED" without observing the CompletableFuture from confirmAsync, so the failed future for unknown order 9999 is swallowed (unconfirmed)
Evidence: get_failure_point reported FAILURE_NOT_FOUND/no uncaught exception for this run; trace_back events 22->21->20 show OrderService#confirmAsync (invocation_id 11) called by OrderService#confirm (invocation_id 10) called by OrderController#confirm (invocation_id 9), with SeededBugs#active event 24 under confirm; OrderService.java:45-53 confirmAsync returns CompletableFuture.failedFuture(IllegalArgumentException "unknown order") for a missing order; OrderService.java:57-62 confirm stores it in `submitted`, then `if (seeded.active("b11")) return "SUBMITTED";` without `submitted.join()`, while OrderController.java:52-54 returns that status as HTTP 200
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In OrderService#confirm, always observe the future (call submitted.join()) and return "CONFIRMED" only on success, removing the b11 early "SUBMITTED" return
Regression test: POST /orders/9999/confirm for a non-existent order expects HTTP 500, and confirming an existing order still returns "CONFIRMED"
ROOT_CAUSE: trace.demoapp.service.OrderService#confirm
