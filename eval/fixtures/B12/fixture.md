# B12 - Concurrency / race (seeded, profile `b12`)

- id: B12
- category: Concurrency / race
- source: seeded (demo-app-spec.md B12)
- activation: Spring profile `b12`
- trigger: set stock of SKU-1 to 1 (`PUT /stock/SKU-1`), then fire two
  parallel `POST /orders` (SKU-1, quantity 1).
  Seeded symptom: both orders succeed for a single unit (clean: exactly
  one succeeds, the other is rejected).
- ground-truth root cause location: `trace.demoapp.service.InventoryService#decrement`
  - non-atomic check-then-act: both threads pass the availability check
    before either decrements (seeded lines marked SEEDED-B12)
- ground-truth explanation: The seeded decrement reads the level, checks
  availability, waits on a latch (the fixture's determinism device), and
  only then subtracts and saves. Two concurrent orders both observe
  quantity 1 and both succeed. The clean path uses a single atomic
  conditional update, so exactly one wins.
- determinism: latch-controlled. Under profile `b12` the seeded code
  rendezvouses the two decrementing threads between check and act, so
  the interleaving is forced, not timed. Trigger runs 3x.
- expected backward slice: both successful order placements; their
  producing InventoryService.decrement invocations on the two threads.
- passing-run availability: yes - default profile (clean) rejects the
  second order.
- verification: clean passes; seeded trigger 3x deterministic (recorded after seeding).
- Step-2 verification (2026-11-01): seeded trigger 3/3 deterministic (latch-controlled, incl. parallel orders); clean profile behaviour covered by DemoAppTest.
