# B15 - Config / env (seeded, profile `b15`)

- id: B15
- category: Config / env
- source: seeded (demo-app-spec.md B15)
- activation: Spring profile `b15`
- trigger: `GET /pricing?sku=SKU-1&quantity=1` (subtotal 1000, below the
  5000 bulk-discount threshold).
  Seeded symptom: totalCents is 990 — a discount was applied anyway
  (clean: 1100, no discount below threshold).
- ground-truth root cause location: `trace.demoapp.PricingConfig#discountPolicy`
  - the profile-specific always-discount policy is installed without
    checking that its profile is active (seeded lines marked SEEDED-B15)
- ground-truth explanation: A discount policy intended only for a
  specific profile is wired in unconditionally, so every profile gets a
  discount threshold of 0. Pricing logic itself is correct; the bean
  selection ignores the profile condition.
- expected backward slice: the discounted total returned for the trigger
  request; its producing DiscountPolicy bean from
  PricingConfig.discountPolicy via PricingService.priceCents.
- passing-run availability: yes - default profile (clean) applies the
  standard 5000 threshold.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
