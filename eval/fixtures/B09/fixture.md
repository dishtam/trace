# B09 — Bad state mutation across calls (seeded, profile `b09`)

- id: B09
- category: Bad state mutation across calls
- source: seeded (demo-app-spec.md B09, shared-service-state shape)
- activation: Spring profile `b09`
- trigger: in one app instance, first `GET /pricing?sku=SKU-1&quantity=1`
  (= 1100), then `GET /pricing?sku=SKU-2&quantity=1`.
  Seeded symptom: second response also reports 1100 (first call's total bleeds).
- ground-truth root cause location: `trace.demoapp.service.PricingService`
  singleton field holding the last computed total, reused across requests
  (seeded lines marked SEEDED-B09)
- ground-truth explanation: PricingService is a singleton; the seeded code
  caches the last total in an instance field and returns it for the next
  request regardless of sku/quantity. Root cause is the shared mutable field,
  not either request's inputs.
- expected backward slice: not exception-based; the second response's return
  event in PricingService.priceCents carries the first request's value.
- passing-run availability: yes — default profile (clean): second call = 2500
  (SKU-2 is zero-rated, so no tax is added).
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B09/trigger.ps1). 
