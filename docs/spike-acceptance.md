# Phase 0 Spike — Acceptance & Go/No-Go Gate

This gate decides whether TRACE proceeds (feasibility plan: Phase 0 ≈ 2 weeks).
Thresholds T1–T5 were **Proposed** at drafting and **Accepted by the owner on 2026-10-28 (Step 0 grill, item g)**. Numbers stay labelled Proposed in the tables for traceability; changing them now requires a new ADR.

## Scope

| In | Out |
|---|---|
| Demo Spring Boot app ([demo-app-spec.md](demo-app-spec.md)) with seeded bugs | Production/staging capture (ADR-0001) |
| Agent: call/return/throw + arg/return summaries, package allow-list, dump-on-failure ring buffer | Full local-variable capture everywhere; heap-level data-flow |
| Store on SQLite first + DuckDB comparison task (ADR-0008) | Graph DB; human UI |
| 4 MCP tools: `get_failure_point`, `trace_back`, `get_call_subtree`, `get_source` | `diff_runs`, `confirm_hypothesis` (Phase 1/3 — unless spike finishes early) |
| Evaluation on ≥ the bug set below vs grep-only baseline | Multi-language; IDE plugin |

## Metrics — exact definitions

| Metric | Definition |
|---|---|
| Runtime overhead | Wall-clock of demo-app request benchmark (load script §demo-app-spec) with agent / without agent, same machine, warmed JVM, median of **Proposed: 5 runs** — rationale: median damps CI noise without hiding regressions |
| Trace volume | MB and events of stored run data per minute of benchmark execution, at the default scoped capture profile |
| Localisation accuracy | For each bug: does the returned root-cause set contain the ground-truth location? Report **top-1 / top-5 at method level** (class+method match) and **top-1 at file level**. Per-bug rows, not only Means (evaluation.md) |
| Investigation cost | Total LLM tokens (in/out, incl. cached) and USD per bug, from API usage records; plus tool-call count |
| Time to root cause | Wall-clock from investigation start (failure point known) to submitted report, per bug |

## Proposed thresholds

| # | Threshold | Rationale |
|---|---|---|
| T1 | Scoped overhead **≤ 3×** on the demo benchmark | Above ~3×, dev-loop CI use collapses; Lightrun-class tools already draw complaints at lower overheads (plan §5) |
| T2 | Trace volume **≤ 50 MB/min** at default profile | Keeps a CI run's artefact attachable/shareable; forces allow-list discipline |
| T3 | Localisation: TRACE top-5 method accuracy **≥ baseline + 20 percentage points** on **≥ 20 bugs** (fixtures + Defects4J subset combined) | The core claim of the product; +20pp is a difference a user feels, not noise |
| T4 | Investigation cost **≤ $0.50 per bug** (Proposed) | Measured per provider (ADR-0010). At mid-tier pricing (Sonnet-class or Muse Wonder, llm-strategy.md) this is roughly a few hundred K tokens — generous for a slice-guided investigation, impossible for repo-pasting, which is the point |
| T5 | `trace_back` p95 query **≤ 2 s** on a 1M-event run (Proposed) | Interactive-feeling drill-down; also the ADR-0008 storage decision input |

TODO(decide): are T1–T5 the right five, and is ≥20 bugs enough statistical signal for a GO? — question for owner at gate review.

## Decision rule

| Outcome | Condition | Triggers |
|---|---|---|
| **GO** | T3 met, and T1+T2 met (or waived in writing by owner) | Start Phase 1 per [roadmap.md](roadmap.md); lock storage via ADR-0008 benchmark (T5) |
| **PIVOT** | T3 met but T1 or T2 missed by ≤ 2×, or accuracy win only with `confirm_hypothesis`-style re-runs | Narrow capture (coarser default + focus-mode re-record), re-run gate on failed metric only |
| **STOP** | T3 missed, or overhead > **Proposed: 5×** at usable accuracy | Archive findings in this file; do not start Phase 1. Revisit only with a new capture hypothesis |

Every gate review records: per-bug table, medians, machine spec, capture profile, and the signed outcome at the bottom of this file. <!-- GATE RESULT: (empty until Phase 0 runs) -->

## Results log

### S0-4 overhead smoke � 2026-10-28 (Week 1, no threshold judgement)

- Machine: 11th Gen Intel Core i5-11300H @ 3.10GHz, 8 GB RAM, Windows, JDK 21.0.10 LTS.
- Method (this doc's definition): demo load script (`demo-app/overhead.ps1`, sequential Kotlin/Java HTTP client per ADR-0015 � actually Java `LoadClient`), warmed JVM (30 warmup requests), median of 5 runs x 300 requests, fresh app JVM per phase.
- Capture profile: `allow=trace.demoapp`, summary level, ring 65536, event file (ADR-0014).
- Raw runs (ms per 300 requests): off = 2398, 1974, 2333, 1597, 1957 (median **1974**); on = 2726, 2063, 1710, 1531, 1420 (median **1710**).
- Ratio (median on / median off): **0.87x** � below 1, i.e. this sequential single-client method cannot resolve agent overhead at this load; variance (1420-2726 ms) exceeds any plausible agent cost. Recorded as measured; **no T1 judgement**. A server-saturating load (parallel clients) is needed for a T1-grade number: TODO(decide) before the Phase 0 gate.
- Trace volume (agent-on phase): 14,301 events, 7.337 MB event file over ~9.7 s of benchmark execution = **~88,900 events/min, ~45.6 MB/min** (event-file bytes; stored SQLite size will differ). **No T2 judgement.**



## S0-9 decision packet (2026-10-28) � FOR OWNER SIGNATURE

Freeze: eval/freeze-s0-9.md (bug set B02,B03,B04,B05,B09,B10,B14,B16;
providers meta `muse-wonder-1.0` + anthropic `claude-sonnet-5-5`; $0.50 +
50 tool calls per investigation; capture profile and prompts as built in
S0-7). Thresholds below are unchanged from the pre-registered text above.

### Run status (shortfalls, named per the rules)

- **Live investigations: 0 of 32 run.** No provider API key is set on this
  machine (`MODEL_API_KEY`, `ANTHROPIC_API_KEY` unset at process, user, and
  machine scope). TRACE vs baseline therefore has no localisation, cost, or
  report data. Per-bug table: `eval/results/scores.md` (every row NOT RUN,
  produced by `trace.eval.ScoreMain`).
- **Defects4J subset: not run.** No checkout exists; it needs the WSL +
  Java 8/11 setup in docs/evaluation.md (roadmap S0-2 parallel owner item,
  unstaffed). Recorded as a shortfall, not silently dropped.
- The harness, capture pipeline, scoring, and both provider clients are
  built and contract-tested (S0-7); only the metered API calls are missing.

### Threshold verdicts on the evidence that exists

| Gate | Verdict | Evidence |
|---|---|---|
| T1 overhead | UNMEASURED | S0-4: median ratio 0.87x at sequential load; method cannot resolve agent cost (spike-acceptance results log). Needs a saturating load + the paired T3 runs (absent). |
| T2 tooling value | UNMEASURED | Trace volume recorded (~45.6 MB/min, ~88,900 ev/min, S0-4). The comparison size (equivalent logs + jstack + source packaging) was never measured; the =2x smaller clause cannot be judged. |
| T3 localisation | UNMEASURED | 0/32 investigations (no API keys). |
| T4 cost | UNMEASURED | Same as T3. Budget mechanics verified with fake transports only. |
| T5 slice latency | MET, conditional | S0-8 (ADR-0008): materialised-edges `trace_back` p95 2 ms at 1M events (SQLite), far under 2 s. The currently shipped derived-edges query is p95 155 s at 1M and FAILS; the criterion is met only after the edges migration recorded as the ADR-0008 condition. |

Criteria fully met today: 1 of 5 (T5, conditional). Applying the
pre-registered mapping mechanically to completed criteria alone would land
in STOP territory (=1), but 4 of 5 inputs are missing evidence, not
negative evidence; signing STOP on absent T3 data would be a measurement
error of the opposite kind.

### Recommendation (unsigned)

**PIVOT** � narrow the claim and finish the measurement:
1. Land the materialised-edges migration (ADR-0008 condition) so T5 holds
   on the shipped query path, not only on the benchmark shape.
2. Provide one or both provider keys, then run the frozen S0-7/S0-9 matrix
   (8 bugs x 2 runners x providers) without changing any frozen item.
3. Add the saturating-load T1 measurement and the T2 comparison artefact.
Re-decide GO/PIVOT/STOP on the completed table. Estimated remaining spend
for step 2 is capped at $0.50 x 32 = $16 by the frozen budgets.

Owner signature: ____________________  Date: __________
