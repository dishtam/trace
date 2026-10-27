---
name: eval-localization
description: Use when running or reporting the TRACE benchmark: Defects4J plus seeded fixtures, overhead, trace size, or localisation accuracy versus the grep-only baseline.
---

# Eval Localisation

## What to run

1. **Corpus A — seeded fixtures** (skill `add-bug-fixture`): fast, run on
   every relevant change.
2. **Corpus B — Defects4J**: real Java bugs; slower, run before releases /
   claims. TODO: Defects4J subset and version pin — not decided.
3. **Baseline:** the same bugs given to a grep-only agent (search + source
   reading, no trace tools). Same model, same time/token budget where
   applicable. A result without the baseline is not a result.

## Metrics to report (always all four)

| Metric | Definition |
|---|---|
| Localisation accuracy | Expected root-cause statement is in the returned slice/answer (per corpus; report per-bug, not only the mean) |
| Slice precision / recall | Returned slice statements vs hand-recorded ground-truth slice (fixtures) |
| Overhead | Instrumented vs uninstrumented wall time (p50/p95) on the same workload |
| Trace size | Bytes (and events) per recorded minute / per run, per capture profile |

## Rules

- Publish per-bug rows; averages alone hide regressions.
- Compare like with like: same capture profile, same machine class, same
  warm-up. State all three.
- If accuracy improves but overhead or trace size regresses past the
  `instrumentation-safety` budget, report that as a failure, not a win.
- TODO: benchmark harness command/location — not decided. Until it exists,
  report "harness not built yet" instead of ad-hoc numbers.
- Never report an improvement that was not measured in this run.
