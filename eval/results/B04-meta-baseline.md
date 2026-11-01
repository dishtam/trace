---
bug: B04
runner: baseline
provider: meta
model: muse-wonder-1.0
claimed_root_cause: trace.demoapp.service.InventoryService#decrementChecked
budget: tokens_in=9572 tokens_out=2193 cost_usd=0.0821 tool_calls=5
budget_exceeded: false
wall_ms: 17159
tool_calls_log:
  - read_file({"path":"src/main/java/trace/demoapp/service/InventoryService.java"}) -> ERROR: no such file: src/main/java/trace/demoapp/service/InventoryService.java
  - list_files({}) -> java\trace\demoapp\DataSeeder.java java\trace\demoapp\DemoApplication.java java\trace\demoapp\LoadClient.java java\trace
  - read_file({"path":"java/trace/demoapp/service/InventoryService.java"}) -> java/trace/demoapp/service/InventoryService.java lines 1..66 of 66 package trace.demoapp.service;  import org.springfram
  - search_text({"pattern":"quantity >= level\\.quantity"}) -> java\trace\demoapp\service\InventoryService.java:51: boolean insufficient = seeded.active("b04") ? quantity >= level.qua
  - search_text({"pattern":"decrementChecked|decrement\\(","path_glob":"*.java"}) -> java\trace\demoapp\service\CheckoutService.java:27: inventory.decrement(sku, quantity); java\trace\demoapp\service\Inven
---

Root cause: In InventoryService.decrementChecked, the b04 branch uses `quantity >= level.quantity`, so ordering exactly the available stock is incorrectly judged insufficient (unconfirmed)
Evidence: GET /stock/SKU-2 returned 100000 and POST /orders quantity=100000 returned HTTP 500; log shows IllegalStateException at InventoryService.java:52 via InventoryService.decrement InventoryService.java:39 and CheckoutService.java:27; java/trace/demoapp/service/InventoryService.java:51 shows `quantity >= level.quantity` for b04 versus `quantity > level.quantity` otherwise
Verification: unconfirmed - replay tool unavailable in this phase
Suggested fix: Change `quantity >= level.quantity` to `quantity > level.quantity` at java/trace/demoapp/service/InventoryService.java:51
Regression test: Seed stock 100000, order quantity 100000 and assert success and stock becomes 0, while quantity 100001 still fails with insufficient stock
ROOT_CAUSE: trace.demoapp.service.InventoryService#decrementChecked
