# B18 - Null propagation (seeded, profile `b18`)

- id: B18
- category: Null propagation
- source: seeded (demo-app-spec.md B18)
- activation: Spring profile `b18`
- trigger: `GET /pricing/regional?sku=SKU-1&quantity=1&region=EU`.
  Seeded symptom: HTTP 500 (NullPointerException) instead of a priced
  response (clean: HTTP 200).
- ground-truth root cause location: `trace.demoapp.service.PricingService#priceRegionalCents`
  - the regional-rate map is looked up by sku instead of region, returns
    null, and the null is unboxed (seeded line marked SEEDED-B18)
- ground-truth explanation: The tax-rate lookup uses the wrong key
  (product sku rather than region), so the map returns null. Unboxing
  that null into the tax computation throws NPE in the pricing layer.
- expected backward slice: the throw in
  PricingService.priceRegionalCents; its producing map lookup by the
  wrong key in the same method.
- passing-run availability: yes - default profile (clean) prices the
  regional request.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
