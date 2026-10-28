# B05 — Off-by-one (seeded, profile `b05`)

- id: B05
- category: Off-by-one
- source: seeded (demo-app-spec.md B05)
- activation: Spring profile `b05`
- trigger: place one order (SKU-1), then `GET /orders?page=0&size=10`.
  Seeded symptom: the just-placed order is missing from page 0.
- ground-truth root cause location: `trace.demoapp.service.OrderService#listOrders`
  — page offset computed as page*size + 1 (seeded line marked SEEDED-B05)
- ground-truth explanation: Pagination must start at page*size; the seeded +1
  skips the first element of every page, so page 0 loses its first order.
  No exception: the symptom is a silently incomplete list.
- expected backward slice: not exception-based; the return of
  OrderService.listOrders for the trigger request omits the first order.
- passing-run availability: yes — default profile (clean) lists the order.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B05/trigger.ps1). 
