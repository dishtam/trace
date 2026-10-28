# B04 — Wrong conditional / boundary (seeded, profile `b04`)

- id: B04
- category: Wrong conditional
- source: seeded (demo-app-spec.md B04)
- activation: Spring profile `b04`
- trigger: order exactly the available stock: read `GET /stock/SKU-2`
  (= 100000), then `POST /orders {"sku":"SKU-2","quantity":100000}`.
  Seeded symptom: HTTP 500 "insufficient stock" although stock suffices.
- ground-truth root cause location: `trace.demoapp.service.InventoryService#decrement`
  — stock check uses >= instead of > (seeded line marked SEEDED-B04)
- ground-truth explanation: Buying exactly the remaining stock must succeed;
  the seeded boundary rejects quantity == stock. Root cause is the comparison
  in the stock check, not the caller's quantity.
- expected backward slice: throw in InventoryService.decrement → CheckoutService
  → OrderService.placeOrder → OrderController.place.
- passing-run availability: yes — default profile (clean) accepts the order.
  Note: the clean trigger depletes SKU-2 stock in that app instance; the
  trigger script starts a fresh app per run.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B04/trigger.ps1). 
