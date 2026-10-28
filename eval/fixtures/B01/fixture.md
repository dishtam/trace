# B01 — Null propagation (seeded)

- id: B01
- category: Null propagation
- source: seeded (demo-app-spec.md B01)
- activation: Spring profile `b01` (retro-gated in S0-6 so all seeded bugs share
  one activation model; the default profile is the clean app). Originally seeded
  ungated in S0-5; the S0-5 exit-check evidence was captured in that form.
- trigger: `POST /orders {"sku":"UNKNOWN-SKU","quantity":1}` against the demo app
  running with profile `b01` (see trigger.ps1). Expected symptom: HTTP 500, NullPointerException.
- ground-truth root cause location: `trace.demoapp.service.OrderService#placeOrder`,
  seeded dereference at OrderService.java:24-25 (`SEEDED-B01` comment)
- ground-truth explanation: OrderService looks the product up for an order,
  receives null for an unknown SKU (repository has no such product), and
  dereferences the result without a null check. The NPE is thrown in
  OrderService before pricing/checkout runs. The fix is to handle the absent
  product at that lookup (reject the order), not to catch the NPE downstream.
- expected backward slice (hand-computed): the throw event in
  OrderService.placeOrder → its call event → OrderController.place
  (parent invocation). TaxService/PricingService must NOT appear: they never run.
- passing-run availability: yes — same request with sku SKU-1 succeeds (clean
  behaviour for known SKUs is unchanged; the clean variant of the seeded hunk
  is its revert, recorded in git history of the seeding change).
- verification: clean variant passes; seeded trigger run 3x (deterministic 500/NPE).
  Verified 2026-10-28: 3x HTTP 500, stored trace shows NullPointerException
  ("product" is null) thrown in OrderService.placeOrder, known-SKU order still 200.

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B01/trigger.ps1). 
