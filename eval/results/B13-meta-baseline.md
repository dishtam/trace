---
bug: B13
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.OrderService#placeOrder
budget: tokens_in=9933 tokens_out=2665 cost_usd=0.0930 tool_calls=6
budget_exceeded: false
wall_ms: 17175
tool_calls_log:
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java\\trace\\demoapp\\web\\OrderController.java"}) -> java\trace\demoapp\web\OrderController.java lines 1..61 of 61 package trace.demoapp.web;  import org.springframework.web
  - read_file({"path":"java\\trace\\demoapp\\service\\OrderService.java"}) -> java\trace\demoapp\service\OrderService.java lines 1..106 of 106 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java\\trace\\demoapp\\SeededBugs.java"}) -> java\trace\demoapp\SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
  - read_file({"path":"resources\\application.properties"}) -> resources\application.properties lines 1..7 of 7 spring.application.name=trace-demo spring.datasource.url=jdbc:h2:mem:de
  - search_text({"pattern":"confirmAsync|confirmationStatus|confirmations"}) -> java\trace\demoapp\service\OrderService.java:18: private final java.util.concurrent.ConcurrentHashMap<Long, String> conf
---

Root cause: Under profile b13, placeOrder passes null instead of the saved order id to the async confirmation, so the confirmation never runs and status remains NONE (unconfirmed)
Evidence: POST /orders -> 200 {"status":"PLACED","id":1} but GET /orders/1/confirmation -> 200 {"id":1,"status":"NONE"}; OrderController.java:59 delegates to confirmationStatus; OrderService.java:65-66 returns confirmations.getOrDefault(orderId,"NONE"); OrderService.java:47-49 confirmAsync throws on null id and only otherwise puts "CONFIRMED"; OrderService.java:39 calls confirmAsync(seeded.active("b13") ? null : saved.id)
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In OrderService.java:39 always propagate the id, e.g. confirmAsync(saved.id)
Regression test: Place order SKU-1 quantity 1, poll GET /orders/{id}/confirmation for a few seconds and assert it becomes CONFIRMED not NONE
ROOT_CAUSE: trace.demoapp.service.OrderService#placeOrder
