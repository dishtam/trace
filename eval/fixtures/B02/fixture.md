# B02 — Null propagation (seeded, profile `b02`)

- id: B02
- category: Null propagation
- source: seeded (demo-app-spec.md B02)
- activation: Spring profile `b02` (see trigger.ps1; default profile is clean)
- trigger: `POST /orders {"sku":"SKU-2","quantity":1}` (SKU-2 is zero-rated).
  Expected seeded symptom: HTTP 500, NullPointerException while pricing.
- ground-truth root cause location: `trace.demoapp.service.TaxService#taxFor`
  returning null for a zero-rated product, consumed unchecked by
  `trace.demoapp.service.PricingService#priceCents` (seeded lines marked SEEDED-B02)
- ground-truth explanation: The tax lookup returns null for a zero-rated
  product instead of zero tax. The pricing code unboxes that null into a long,
  throwing NPE. Clean behaviour treats zero-rated as 0 tax.
- expected backward slice: throw in PricingService.priceCents →
  CheckoutService.checkout → OrderService.placeOrder → OrderController.place.
- passing-run availability: yes — default profile (clean), and SKU-1 under `b02`.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B02/trigger.ps1). 
