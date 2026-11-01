# Step-2 freeze record (frozen 2026-11-01, BEFORE any live run)

Supersedes only the bug-set clause of eval/freeze-s0-9.md. Everything else
in that freeze (providers/models, $0.50 + 50 tool calls per investigation,
capture profile, prompts, scoring rules, T1–T5 thresholds) is unchanged and
is not restated here. Live investigations at freeze time: still 0 of any —
no provider key has been used, so no result exists that this change could
be tuned against.

Owner decision (2026-11-01): Option A — seed the remaining demo bugs to
reach the 20-bug gate set instead of running the frozen 8 with a waiver
or building the Defects4J subset. Defects4J is deferred (Phase 1 candidate);
the T3 "fixtures + Defects4J combined" wording is satisfied by 20 seeded
fixtures for this gate, and that substitution is recorded here rather than
silently.

- Bug set: B01–B20 (all seeded, demo-app-spec.md catalogue).
  Ground truth: eval/fixtures/<id>/fixture.md. The Step-2 fixtures
  (B06–B08, B11–B13, B15, B17–B20) were written before the bugs were
  seeded (add-bug-fixture skill) and before any agent ran them; each was
  verified seeded-fails 3/3 deterministic with the clean profile passing
  (DemoAppTest + per-fixture notes). B01 gains an EvalHarness trigger
  sequence in this step; it was excluded from the S0-9 eight only because
  the harness lacked that sequence.
- Category coverage: Null propagation 3 (B01, B02, B18), Wrong
  conditional 3 (B03, B04, B19), Off-by-one 2 (B05, B06), Bad state
  mutation 3 (B07, B08, B09), Exception swallowed 3 (B10, B11, B20),
  Concurrency / race 2 (B12 latch-controlled, B13), Config / env 2
  (B14, B15), Serialization / mapping 2 (B16, B17). Total 20.
- Matrix size at the frozen budgets: 20 bugs x 2 runners x 2 providers
  = 80 investigations, capped at $0.50 each ($40 maximum at the cap, one
  overshooting call per investigation aside). Supersedes the $16 figure
  quoted for the 8-bug set in the S0-9 packet.
- Scoring: ScoreMain now iterates B01–B20; missing reports still render
  as NOT RUN.

Gate implication: with 20 verified fixtures frozen before any live run,
a GO verdict is available under the pre-registered T3 rule if the
measured numbers earn it.
