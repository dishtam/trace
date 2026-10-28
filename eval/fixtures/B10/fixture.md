# B10 — Exception swallowed (seeded, profile `b10`)

- id: B10
- category: Exception swallowed
- source: seeded (demo-app-spec.md B10)
- activation: Spring profile `b10`
- trigger: `GET /orders/9999` (no such order).
  Seeded symptom: HTTP 500 NullPointerException raised in the web layer.
  Clean symptom: HTTP 500 NoSuchElementException from the service (also an
  error — the difference is exception type and where it is thrown).
- ground-truth root cause location: `trace.demoapp.service.OrderService#get`
  — repository failure caught and null returned (seeded block marked SEEDED-B10);
  the NPE surfaces later in `trace.demoapp.web.OrderController#getOrder`.
- ground-truth explanation: The service swallows the lookup failure and returns
  null; the controller dereferences it. Root cause is the catch-and-return-null
  in the service, not the controller's dereference.
- expected backward slice: throw (NPE) in OrderController.getOrder → its call →
  OrderService.get return of null. The swallowed NoSuchElementException itself
  never appears as an event — that absence is the point of the fixture.
- passing-run availability: stated absence — no passing variant for a missing
  order; the clean variant differs by exception type/location (above).
- verification: clean behaviour recorded; seeded trigger 3x deterministic.

- S0-6 verification (2026-10-28): seeded trigger 3/3 deterministic, clean profile check passed (harness in TEMP, commands: eval/fixtures/B10/trigger.ps1). 
