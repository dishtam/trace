# Seeded fixtures (demo-app)

One folder per bug: `fixture.md` (ground truth, written before any tool runs
the bug) + `trigger.ps1` (starts a fresh demo app under the bug's Spring
profile, asserts the symptom, stops the app). Shared helpers:
[trigger-lib.ps1](trigger-lib.ps1). Bugs are profile-gated via
`trace.demoapp.SeededBugs`; the default profile is always clean, so fixtures
never interfere. Clean variant for every bug = run without its profile.

| ID | Category | Activation | Symptom |
|---|---|---|---|
| B01 | Null propagation | profile `b01` | NPE in OrderService on unknown SKU |
| B02 | Null propagation | profile `b02` | NPE unboxing null tax (zero-rated SKU-2) |
| B03 | Wrong conditional | profile `b03` | Discount withheld exactly at threshold |
| B04 | Wrong conditional | profile `b04` | Buying exactly the stock rejected |
| B05 | Off-by-one | profile `b05` | First order missing from page 0 |
| B09 | Bad state mutation | profile `b09` | Previous total bleeds into next pricing |
| B10 | Exception swallowed | profile `b10` | NPE in web layer for missing order |
| B14 | Config / env | profile `b14` | Wrong property key taxes at 25% not 10% |
| B16 | Serialization / mapping | profile `b16` | Order response drops totalCents |

Coverage: 7 of 8 evaluation.md categories. Concurrency/race is intentionally
absent: a race trigger cannot pass the 3x-deterministic bar without flakiness;
add it only with a latch-controlled fixture (TODO for S0-6 follow-up / Phase 2).
The eval harness (S0-7) consumes these folders as-is; no harness code was
needed to pick them up beyond reading fixture.md + running trigger.ps1.
