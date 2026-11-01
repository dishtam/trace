# Phase 0 Gate Review — STOP (2026-11-01)

Decision: **STOP**, signed by the owner on 2026-11-01 (recorded in the
Muse session of that date). Rule applied as written in
[spike-acceptance.md](spike-acceptance.md): STOP triggers on
"T3 missed, or overhead > 5x at usable accuracy". T3 is missed (overhead
is not). Consequence per that document: archive findings here, do not
start Phase 1, revisit only with a new capture hypothesis.

This file is the results log spike-acceptance.md requires (per-bug
table, medians, machine spec, capture profile, signed outcome). It lives
here rather than appended to spike-acceptance.md because that file
contains invalid UTF-8 bytes that the editing tools refuse; its text is
unchanged and its thresholds were applied exactly as pre-registered.

## Runs of record

- Localisation (Step 4): 40 investigations, 20 bugs (B01–B20, seeded,
  ground truth pre-recorded in eval/fixtures/) x baseline/TRACE, Meta
  `muse-wonder-1.0`, $0.50 + 50 tool calls per investigation. Ran from
  commit 6b98a50 by the operator, ~23.5 min wall, ~$8.53 total.
  Per-bug rows: eval/results/scores.md; reports: eval/results/B*-meta-*.md;
  summary: eval/results/summary-meta.json.
- Overhead/volume (Step 5): eval/results/overhead-t1-t2.md (ABBA method,
  two executions). Storage/slice (Step 1): ADR-0008 implementation note
  (1,000,068 events, production query path).
- Machine: 11th Gen Intel Core i5-11300H @ 3.10GHz, 8 GB RAM, Windows,
  JDK 21.0.10. Capture profile: `allow=trace.demoapp`, summary level,
  ring 65536, agent -> event file -> collector -> SQLite (ADR-0014).

## Threshold verdicts

| # | Threshold (pre-registered) | Measured | Verdict |
|---|---|---|---|
| T1 | Scoped overhead ≤ 3x | 1.15x and 1.03x (two ABBA executions, saturating load) | MET |
| T2 | Trace volume ≤ 50 MB/min at default profile | 365 and 529 MB/min stored at saturating load (~24.7 events, ~14.8 KB stored per request; throughput-dependent; 45.6 MB/min at S0-4's gentle sequential load) | NOT MET |
| T3 | TRACE top-5 method accuracy ≥ baseline + 20pp on ≥ 20 bugs | Method (single-claim scoring): baseline 15/20, TRACE 15/20, delta 0pp. Class: baseline 20/20, TRACE 19/19 completed | NOT MET |
| T4 | Investigation cost ≤ $0.50 per bug | TRACE mean $0.28 (baseline $0.13); two individual overshoots (B08 $0.52, B12 $0.55) | MET on mean |
| T5 | trace_back p95 ≤ 2 s on a 1M-event run | p95 19 ms on the shipped edges query (was 155 s derived CTE) | MET |

Method note on T3: the threshold text says top-5, but both runners emit
a single claimed location, so scoring is top-1 for both sides. Recorded
for honesty; it handicaps both equally and does not change the delta.
Sensitivity note: on B04, B06, and B12 (baseline) the agents named the
private helper methods where the defects literally live
(`decrementChecked`, `sumLineTotals`) against fixtures citing the public
entry methods, scoring method=no under strict equality — again equally
on both sides, delta unchanged.

## Findings archived (the useful residue)

1. TRACE's advantage is real but narrow on this corpus: cheaper and
   faster than baseline exactly on exception-driven bugs (B01, B02,
   B18 — 5 tool calls vs 9–12), where the failure-point-first workflow
   applies. On wrong-value/config bugs in a 19-file codebase, grep
   reads the whole service layer in a handful of calls while TRACE
   spent 20–36 tool calls per bug at 2–3x the cost.
2. Non-exception failures have no entry point in the current product:
   on B12 (race) `get_failure_point` returned FAILURE_NOT_FOUND and the
   TRACE agent exhausted its 40-turn limit without a claim. Any revived
   hypothesis needs a non-exception starting point (diff/anomaly),
   not just exception slicing.
3. Volume scales per request (~14.8 KB stored/request), so MB/min
   thresholds are throughput claims in disguise: the same profile is
   45.6 MB/min at gentle load and 365–529 MB/min at saturation.
4. The agent itself is cheap (T1 ~1.0–1.2x) and the slice engine is
   fast (T5 with ~100x headroom after the materialised-edges migration,
   ADR-0008). The negative result is about localisation value in small
   codebases, not about capture or query feasibility.
5. Harness lessons worth keeping: the documented `:eval:run` path had
   never actually run (module working-dir bug, fixed), and two SQLite
   planner traps nearly invalidated the storage migration (recorded in
   ADR-0008 and code comments).

## Shortfalls recorded, not hidden

- Anthropic matrix: not run (no key provided; Meta only). Frozen as
  optional in the gate conversation; the owner signed STOP without it.
- Defects4J: never attempted; substituted by 20 seeded fixtures under
  eval/freeze-step2-20.md.
- ADR-0008 remains **Proposed**: its edges condition was implemented
  and measured, but the owner never moved it to Accepted. Under STOP
  the storage lock-in question is moot.

## What STOP means here

- No Phase 1 work starts. The roadmap's Phase 1–4 are closed unless
  the project is revived under the spike-acceptance condition:
  "Revisit only with a new capture hypothesis."
- The code, fixtures, gate evidence, and this record stay in the repo
  as the archive. A revival candidate suggested by the evidence (not
  authorised by this decision): evaluate the same graph tooling on a
  codebase large enough that reading it wholesale is not an option,
  with a non-exception entry point — that is a different claim than the
  one measured here and would need its own gate.

Owner signature: recorded via owner instruction, 2026-11-01.
Decision: STOP.
