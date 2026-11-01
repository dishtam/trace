---
bug: B11
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.OrderService#confirm
budget: tokens_in=6113 tokens_out=1665 cost_usd=0.0578 tool_calls=3
budget_exceeded: false
wall_ms: 9178
tool_calls_log:
  - search_text({"pattern":"/orders.*confirm|confirm"}) -> java\trace\demoapp\service\OrderService.java:18: private final java.util.concurrent.ConcurrentHashMap<Long, String> conf
  - read_file({"path":"java/trace/demoapp/service/OrderService.java"}) -> java/trace/demoapp/service/OrderService.java lines 1..106 of 106 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java/trace/demoapp/web/OrderController.java"}) -> java/trace/demoapp/web/OrderController.java lines 1..61 of 61 package trace.demoapp.web;  import org.springframework.web
---

Root cause: When profile b11 is active, OrderService#confirm returns "SUBMITTED" without joining the CompletableFuture from confirmAsync, so the failed future for a non-existent order is never observed and its exception is swallowed (unconfirmed)
Evidence: Observed POST /orders/9999/confirm -> HTTP 200 {"id":9999,"status":"SUBMITTED"}; java/trace/demoapp/web/OrderController.java:52-54 delegates to orders.confirm(id); java/trace/demoapp/service/OrderService.java:48 confirmAsync fails with IllegalArgumentException for unknown order and java/trace/demoapp/service/OrderService.java:51-52 returns CompletableFuture.failedFuture(failure); java/trace/demoapp/service/OrderService.java:58-61 confirm stores that future but under b11 returns "SUBMITTED" before submitted.join()
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Remove the b11 early return in OrderService#confirm so submitted.join() is always executed and propagates the async failure before returning "CONFIRMED"
Regression test: POST /orders/9999/confirm for a non-existent order should return HTTP 500 rather than 200 "SUBMITTED" (equivalently, confirm(9999L) should throw)
ROOT_CAUSE: trace.demoapp.service.OrderService#confirm
