# B03 — Wrong conditional (seeded, profile `b03`)

- id: B03
- category: Wrong conditional
- source: seeded (demo-app-spec.md B03)
- activation: Spring profile `b03`
- trigger: `GET /pricing?sku=SKU-1&quantity=5` — subtotal is exactly 5000,
  the discount threshold. Clean total: 5000 - 500 (10% discount) + 450 tax = 4950.
  Seeded symptom: discount withheld at the threshold (total 5500).
- ground-truth root cause location: `trace.demoapp.service.PricingService#priceCents`
  — discount comparison inverted (seeded line marked SEEDED-B03)
- ground-truth explanation: The bulk discount must apply when subtotal >= 5000;
  the seeded comparison applies it only below the threshold, so an order exactly
  at the threshold is overcharged. No exception: the symptom is a wrong total.
- expected backward slice: not exception-based; root cause is the return event
  of PricingService.priceCents for the trigger request, priced without discount.
- passing-run availability: yes — default profile (clean), same request = 4950.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B03/trigger.ps1). 
