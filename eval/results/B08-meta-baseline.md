---
bug: B08
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.OrderService#cancel
budget: tokens_in=12271 tokens_out=2976 cost_usd=0.1086 tool_calls=7
budget_exceeded: false
wall_ms: 17420
tool_calls_log:
  - list_files({"path":""}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java\\trace\\demoapp\\web\\OrderController.java"}) -> java\trace\demoapp\web\OrderController.java lines 1..61 of 61 package trace.demoapp.web;  import org.springframework.web
  - read_file({"path":"java\\trace\\demoapp\\service\\OrderService.java"}) -> java\trace\demoapp\service\OrderService.java lines 1..106 of 106 package trace.demoapp.service;  import org.springframew
  - read_file({"path":"java\\trace\\demoapp\\service\\InventoryService.java"}) -> java\trace\demoapp\service\InventoryService.java lines 1..66 of 66 package trace.demoapp.service;  import org.springfram
  - read_file({"path":"java\\trace\\demoapp\\SeededBugs.java"}) -> java\trace\demoapp\SeededBugs.java lines 1..22 of 22 package trace.demoapp;  import org.springframework.core.env.Environ
  - read_file({"path":"java\\trace\\demoapp\\service\\CheckoutService.java"}) -> java\trace\demoapp\service\CheckoutService.java lines 1..53 of 53 package trace.demoapp.service;  import org.springframe
  - search_text({"pattern":"decrement|restock","path_glob":"*.java"}) -> java\trace\demoapp\data\StockLevelRepository.java:7: /** Atomic check-and-decrement: one conditional update, no read-mod
---

Root cause: Under profile b08, cancelling an order decrements stock again instead of restoring it, so place ( -1 ) plus cancel ( -1 ) leaves stock at initial-2 (unconfirmed)
Evidence: Observed GET /stock/SKU-1 -> 100000, POST /orders -> {"id":1,"totalCents":1100,"status":"PLACED"}, POST /orders/1/cancel -> {"status":"CANCELLED","id":1}, GET /stock/SKU-1 -> 99998; place decrements via CheckoutService.java:27 inventory.decrement, but OrderService.java:74 calls inventory.decrement on cancel when seeded.active("b08"), instead of the restock at OrderService.java:75 (InventoryService.java:29 restock adds quantity back)
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: In OrderService.cancel, always call inventory.restock(order.sku, order.quantity) and remove the b08 inventory.decrement branch at OrderService.java:74-75
Regression test: Seed SKU-1 stock, place order quantity 1 and assert stock is initial-1, cancel it and assert stock returns to initial and status is CANCELLED
ROOT_CAUSE: trace.demoapp.service.OrderService#cancel
