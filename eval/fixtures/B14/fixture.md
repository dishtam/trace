# B14 — Config / env (seeded, profile `b14`)

- id: B14
- category: Config / env
- source: seeded (demo-app-spec.md B14)
- activation: Spring profile `b14`
- trigger: `GET /pricing?sku=SKU-1&quantity=1`.
  Clean total: 1100 (10% tax from pricing.tax.percent).
  Seeded symptom: total 1250 (25% applied instead).
- ground-truth root cause location: `trace.demoapp.service.TaxService` —
  reads the wrong property key (pricing.vat.percent, absent, falls back to 25)
  instead of pricing.tax.percent (seeded line marked SEEDED-B14)
- ground-truth explanation: The configured tax rate is 10%
  (application.properties pricing.tax.percent=10). The seeded code binds a
  differently-named key, misses the configured value, and silently uses its
  25% fallback. No exception: every total is quietly over-taxed.
- expected backward slice: not exception-based; root cause is the return of
  TaxService for the trigger request, computed from the wrong key.
- passing-run availability: yes — default profile (clean) returns 1100.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B14/trigger.ps1). 
