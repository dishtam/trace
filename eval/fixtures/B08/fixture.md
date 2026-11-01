# B08 - Bad state mutation (seeded, profile `b08`)

- id: B08
- category: Bad state mutation across calls
- source: seeded (demo-app-spec.md B08)
- activation: Spring profile `b08`
- trigger: note stock of SKU-1, place one order (quantity 1), cancel it
  via `POST /orders/{id}/cancel`, read stock again.
  Seeded symptom: stock is initial-2 (clean: back at initial — placing
  took 1, cancelling restored 1).
- ground-truth root cause location: `trace.demoapp.service.OrderService#cancel`
  - stock mutation is applied before/regardless of the cancellation
    instead of a restock (seeded line marked SEEDED-B08)
- ground-truth explanation: Cancel decrements the stock again instead of
  restoring the placed quantity: the mutation runs on the cancel path
  with the wrong sign/order, so a cancelled order consumes stock twice.
- expected backward slice: the final StockLevel value read by
  GET /stock/SKU-1; its producing writes are the placement decrement and
  the second decrement in OrderService.cancel.
- passing-run availability: yes - default profile (clean) restores stock
  on cancel.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
