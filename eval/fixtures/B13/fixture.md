# B13 - Concurrency / race (seeded, profile `b13`)

- id: B13
- category: Concurrency / race
- source: seeded (demo-app-spec.md B13)
- activation: Spring profile `b13`
- trigger: place one order (SKU-1, quantity 1), then poll
  `GET /orders/{id}/confirmation` for a few seconds.
  Seeded symptom: status stays NONE (clean: CONFIRMED).
- ground-truth root cause location: `trace.demoapp.service.OrderService#placeOrder`
  - the async confirmation is submitted with a null order id instead of
    the saved order's id (seeded line marked SEEDED-B13)
- ground-truth explanation: The hand-off to the async confirmation loses
  its context: the task is submitted without the order id it must
  confirm, so it fails immediately and no confirmation is ever recorded
  for the placed order. The order itself succeeds; only the cross-thread
  context is lost.
- expected backward slice: the (empty) confirmation state for the placed
  order; the producing submission in OrderService.placeOrder and the
  failing OrderService.confirmAsync invocation.
- passing-run availability: yes - default profile (clean) confirms the
  placed order.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic; clean profile behaviour covered by DemoAppTest.
