# B16 — Serialization / mapping (seeded, profile `b16`)

- id: B16
- category: Serialization / mapping
- source: seeded (demo-app-spec.md B16)
- activation: Spring profile `b16`
- trigger: `POST /orders {"sku":"SKU-1","quantity":1}`.
  Seeded symptom: HTTP 200 but the JSON response has no `totalCents` field.
  Clean response: {"id":…,"totalCents":1100,"status":"PLACED"}.
- ground-truth root cause location: `trace.demoapp.web.OrderController#place`
  — response mapping drops the totalCents field (seeded block marked SEEDED-B16)
- ground-truth explanation: The order is priced and stored correctly; the API
  mapping that builds the response simply never copies totalCents. Root cause
  is in the web-layer mapping, not in pricing or persistence.
- expected backward slice: not exception-based; CheckoutService/OrderService
  events show the correct total while the HTTP response omits it — the
  divergence between trace and response is the signal.
- passing-run availability: yes — default profile (clean) includes totalCents.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B16/trigger.ps1). 
