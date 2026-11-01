# B06 - Off-by-one (seeded, profile `b06`)

- id: B06
- category: Off-by-one
- source: seeded (demo-app-spec.md B06)
- activation: Spring profile `b06`
- trigger: place one order (SKU-1, quantity 1), read its totalCents.
  Seeded symptom: totalCents is 0 instead of 1100 — the order's only
  line total is dropped.
- ground-truth root cause location: `trace.demoapp.service.CheckoutService#checkout`
  - line-sum loop bound is size-1 (seeded line marked SEEDED-B06)
- ground-truth explanation: Checkout sums the order's line totals with a
  loop bounded by size-1, so the last line never contributes. With the
  demo's one-line orders the total collapses to 0; no exception is thrown.
- expected backward slice: the return of CheckoutService.checkout for the
  trigger order; its producing line-sum loop in the same method.
- passing-run availability: yes - default profile (clean) totals 1100.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
