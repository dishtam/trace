# B17 - Serialization / mapping (seeded, profile `b17`)

- id: B17
- category: Serialization / mapping
- source: seeded (demo-app-spec.md B17)
- activation: Spring profile `b17`
- trigger: `POST /pricing/callback` with JSON
  `{"sku":"SKU-1","totalCents":1234}`.
  Seeded symptom: the response reports acceptedTotalCents 0 — the
  payload's total never arrived (clean: 1234).
- ground-truth root cause location: `trace.demoapp.web.PricingController#priceCallback`
  - the callback payload is read under the renamed field `total_cents`
    instead of the sender's `totalCents` (seeded line marked SEEDED-B17;
    the annotation-equivalent rename for this fixture)
- ground-truth explanation: The receiver expects a differently-named
  JSON field than the sender emits, so deserialization silently yields
  the default 0. No exception: the value is lost at the mapping layer.
- expected backward slice: the acceptedTotalCents in the response; its
  producing field lookup in PricingController.priceCallback.
- passing-run availability: yes - default profile (clean) accepts 1234.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
