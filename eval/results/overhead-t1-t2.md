# Step 5 — T1 overhead + T2 trace volume (2026-11-01)

Machine: 11th Gen Intel Core i5-11300H @ 3.10GHz, 8 GB RAM, Windows,
JDK 21.0.10. Capture profile: `allow=trace.demoapp`, summary level,
ring 65536, agent -> event file -> collector -> SQLite (ADR-0014).
Code state: feature/trace at the Step-4 evidence commit (5cc1fb9) plus
this step's LoadClient/overhead.ps1 changes (see git log).

## Method

Supersedes the S0-4 sequential shape, which could not resolve agent cost
(0.87x; variance above any plausible agent cost) and the first parallel
two-phase attempt (1.05x with a monotonic run-to-run speedup in both
phases). Frozen method for this measurement, recorded here before the
gate judgement (it is implemented in demo-app/overhead.ps1):

- Load: demo request mix (place-order / pricing / stock) via LoadClient
  at 16 parallel clients, 1500 requests per run.
- Design: ABBA phase order (off, on, on, off), fresh app JVM per phase,
  cancelling linear warmup/drift. Per phase: warmup 2 x 1500 requests,
  then 5 measured runs. T1 figure per phase = median of its 5 runs;
  reported ratio = mean(on phase medians) / mean(off phase medians).
- T2: volume from the first agent-on phase, warmup events excluded.
  Stored size = the collector-ingested SQLite DB for that phase's event
  file, scaled by the measured-runs share of its events, divided by the
  summed measured run time.

## Results

Run 1:
- Phase medians: off 2749, 2520 ms; on 3667, 2412 ms.
- T1 ratio: **1.15x**. Events (measured runs): 185,001 over 0.303 min
  (609,793 events/min, ~24.7 events/request).
- Event file 300.58 MB/min; **stored 364.95 MB/min** (~14.8 KB/request).

Run 2:
- Phase medians: off 2221, 2065 ms; on 2364, 2044 ms.
- T1 ratio: **1.03x**. Events: 185,000 over 0.209 min (883,125/min).
- Event file 435.12 MB/min; **stored 528.88 MB/min**.

Single-run medians on this shared 8 GB machine vary by tens of percent
between phases; the ABBA replication across two independent executions
(1.15x, 1.03x) is the evidence, not any single median.

## Verdicts

- **T1 (scoped overhead ≤ 3x): MET.** Agent-on costs roughly 1.0–1.2x at
  saturating load — an order of magnitude inside the threshold, and far
  from the >5x STOP trigger.
- **T2 (trace volume ≤ 50 MB/min at default profile): NOT MET** at the
  saturating benchmark load: stored volume is 365–529 MB/min, 7–11x the
  threshold. Per-request volume is stable (~24.7 events, ~14.8 KB
  stored per request); MB/min therefore scales with request throughput.
  At S0-4's gentle sequential load the same profile produced
  45.6 MB/min (event-file bytes), inside the threshold. The threshold as
  written is judged against benchmark execution, and the saturating
  benchmark is the T1-grade reference — so on the recorded method this
  is a miss, with the throughput dependence stated for the gate review.
