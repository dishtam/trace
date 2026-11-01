# B20 - Exception swallowed (seeded, profile `b20`)

- id: B20
- category: Exception swallowed
- source: seeded (demo-app-spec.md B20)
- activation: Spring profile `b20`
- trigger: `POST /orders` with an unknown sku
  (`{"sku":"UNKNOWN","quantity":1}`), then inspect the application log.
  Seeded symptom: HTTP 500, and the logged exception is a bare
  "checkout failed" IllegalStateException with no cause and no mention
  of the unknown sku (clean: the original IllegalArgumentException
  "unknown sku UNKNOWN" is what propagates).
- ground-truth root cause location: `trace.demoapp.service.CheckoutService#checkout`
  - the failure is rethrown as a new exception without the original as
    cause (seeded line marked SEEDED-B20)
- ground-truth explanation: Checkout catches the pricing failure and
  throws a fresh exception that does not chain the original, destroying
  the diagnostic cause chain. The request fails either way; what is
  lost is why.
- expected backward slice: the throw at the web layer for the trigger
  request; in the seeded run the chain stops at
  CheckoutService.checkout instead of reaching PricingService's
  unknown-sku failure.
- passing-run availability: yes - default profile (clean) propagates
  the original exception with its message.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
