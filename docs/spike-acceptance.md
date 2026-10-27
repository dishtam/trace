# Phase 0 Spike — Acceptance & Go/No-Go Gate

This gate decides whether TRACE proceeds (feasibility plan: Phase 0 ≈ 2 weeks).
All thresholds are **Proposed** — owner approves/changes them in one pass (Phase 5 report).

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
| T4 | Investigation cost **≤ $0.50 per bug** (Proposed) | At Sonnet-class pricing (llm-strategy.md) this is roughly a few hundred K tokens — generous for a slice-guided investigation, impossible for repo-pasting, which is the point |
| T5 | `trace_back` p95 query **≤ 2 s** on a 1M-event run (Proposed) | Interactive-feeling drill-down; also the ADR-0008 storage decision input |

TODO(decide): are T1–T5 the right five, and is ≥20 bugs enough statistical signal for a GO? — question for owner at gate review.

## Decision rule

| Outcome | Condition | Triggers |
|---|---|---|
| **GO** | T3 met, and T1+T2 met (or waived in writing by owner) | Start Phase 1 per [roadmap.md](roadmap.md); lock storage via ADR-0008 benchmark (T5) |
| **PIVOT** | T3 met but T1 or T2 missed by ≤ 2×, or accuracy win only with `confirm_hypothesis`-style re-runs | Narrow capture (coarser default + focus-mode re-record), re-run gate on failed metric only |
| **STOP** | T3 missed, or overhead > **Proposed: 5×** at usable accuracy | Archive findings in this file; do not start Phase 1. Revisit only with a new capture hypothesis |

Every gate review records: per-bug table, medians, machine spec, capture profile, and the signed outcome at the bottom of this file. <!-- GATE RESULT: (empty until Phase 0 runs) -->
