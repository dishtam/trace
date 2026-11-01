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
| B06 | Off-by-one | profile `b06` | Order total is 0; last line dropped from the sum |
| B07 | Bad state mutation | profile `b07` | Sale-price update writes the list price instead |
| B08 | Bad state mutation | profile `b08` | Cancelled order decrements stock again |
| B09 | Bad state mutation | profile `b09` | Previous total bleeds into next pricing |
| B10 | Exception swallowed | profile `b10` | NPE in web layer for missing order |
| B11 | Exception swallowed | profile `b11` | Failed async confirmation reported as success |
| B12 | Concurrency / race | profile `b12` | Two parallel orders both succeed for one unit |
| B13 | Concurrency / race | profile `b13` | Order never confirmed; id lost in async hand-off |
| B14 | Config / env | profile `b14` | Wrong property key taxes at 25% not 10% |
| B15 | Config / env | profile `b15` | Profile discount policy applies in every profile |
| B16 | Serialization / mapping | profile `b16` | Order response drops totalCents |
| B17 | Serialization / mapping | profile `b17` | Callback total lost by JSON field rename |
| B18 | Null propagation | profile `b18` | Regional pricing NPEs on wrong map key |
| B19 | Wrong conditional | profile `b19` | Free shipping decided on pre-discount subtotal |
| B20 | Exception swallowed | profile `b20` | Checkout failure rethrown without its cause |

Coverage: all 8 evaluation.md categories, 20 bugs (the full demo-app-spec
set). Concurrency/race (B12) is latch-controlled: the seeded decrement
rendezvouses the two racing threads between check and act, so its trigger
passes the 3x-deterministic bar without timing luck. The eval harness
consumes these folders via fixture.md + EvalHarness trigger sequences;
Step-2 verification for B06–B08, B11–B13, B15, B17–B20 is recorded in each
fixture.md (seeded trigger 3/3, clean behaviour in DemoAppTest).
