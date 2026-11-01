# B19 - Wrong conditional (seeded, profile `b19`)

- id: B19
- category: Wrong conditional
- source: seeded (demo-app-spec.md B19)
- activation: Spring profile `b19`
- trigger: `GET /checkout/shipping?sku=SKU-2&quantity=2` (pre-discount
  subtotal exactly 5000; post-discount total 4500).
  Seeded symptom: shippingCents is 0 (free) instead of 500.
- ground-truth root cause location: `trace.demoapp.service.CheckoutService#shippingCents`
  - the free-shipping threshold is compared against the pre-discount
    subtotal instead of the post-discount total (seeded line marked
    SEEDED-B19)
- ground-truth explanation: Free shipping must be decided on what the
  customer actually pays after discount. Comparing the pre-discount
  subtotal (5000) against the threshold grants free shipping even
  though the discounted total (4500) is below it.
- expected backward slice: the shippingCents returned for the trigger
  request; its producing comparison in CheckoutService.shippingCents
  and the pre-discount value it read from the Product.
- passing-run availability: yes - default profile (clean) charges 500
  for the same request.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
