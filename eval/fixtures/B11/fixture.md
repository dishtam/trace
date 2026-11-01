# B11 - Exception swallowed (seeded, profile `b11`)

- id: B11
- category: Exception swallowed
- source: seeded (demo-app-spec.md B11)
- activation: Spring profile `b11`
- trigger: `POST /orders/9999/confirm` for a non-existent order.
  Seeded symptom: HTTP 200 "SUBMITTED" — the async confirmation fails
  (no such order) but nobody observes the failure (clean: HTTP 500).
- ground-truth root cause location: `trace.demoapp.service.OrderService#confirm`
  - the CompletableFuture returned by the async confirmation is never
    joined/checked (seeded line marked SEEDED-B11)
- ground-truth explanation: The confirmation runs asynchronously and
  completes exceptionally for a missing order. The submission site
  returns success without observing the Future, so the failure is
  silently lost. The clean path joins the Future and surfaces the error.
- expected backward slice: not exception-surfaced (that is the defect);
  the failing async task is OrderService.confirmAsync for order 9999,
  submitted from OrderService.confirm.
- passing-run availability: yes - default profile (clean) returns 500
  for the same trigger.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
