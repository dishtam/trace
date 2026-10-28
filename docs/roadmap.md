# Roadmap — Phase 0 to Phase 4

Source plan: feasibility study (staged plan). Gate metrics: [spike-acceptance.md](spike-acceptance.md).
Milestones ≈ 1 week each (**Proposed** cadence — rationale: weekly exit checks keep the spike honest).

## Phase 0 — Spike (go/no-go) — detailed tasks

### Week 1 — Capture + store skeleton
| Task | Deliverable | Exit check |
|---|---|---|
| S0-1 Repo + Gradle KTS skeleton (modules per architecture.md), JDK pin TODO(verify) | Building empty modules | `./gradlew build` TODO(decide) command works on Win+Linux |
| S0-2 Agent v0: method entry/exit + throw, package allow-list, ring buffer, dump-on-failure | Agent jar attaches to demo-app skeleton | Events recorded for one request |
| S0-3 Event writer → SQLite (schema v0.1 DDL), run finalisation + drop counts | Stored run | Hand-checked rows match demo execution |
| S0-4 Overhead smoke: demo load script, agent on/off | First overhead number | Number recorded (no threshold judgement yet) |

### Week 2 — Slice + MCP + first eval
| Task | Deliverable | Exit check |
|---|---|---|
| S0-5 `trace_back` (recursive CTE) + `get_failure_point`, `get_call_subtree`, `get_source` over MCP | 4 tools, capped, contract tests | Tool answers on seeded bug B01 |
| S0-6 Seed ≥ **Proposed: 8** demo bugs (subset of demo-app-spec 20) with ground truth | Fixtures per evaluation.md | Clean pass / seeded fail verified |
| S0-7 Baseline grep agent harness (same model/budget) | Baseline runner | Runs same 8 bugs, same report shape |
| S0-8 DuckDB comparison: ingest + `trace_back` p95 at synthetic 1M events | ADR-0008 data | Feeds storage decision |
| S0-9 Full gate run: ≥20 bugs target (fixtures + Defects4J subset if ready, else record shortfall), fill results table | Gate review in spike-acceptance.md | **GO / PIVOT / STOP signed** |

Dependencies: S0-2→S0-3→S0-5; S0-6 independent after demo skeleton; Defects4J WSL setup (evaluation.md) starts Week 1 in parallel (risk: slowest setup — TODO(decide): owner).

## Phase 1 — MVP (post-GO)
- M1: Agent hardening (arg/return summaries everywhere in scope, assign/field-write at detailed level, redaction + tests, overhead regression CI).
- M2: Store/graph lock-in (ADR-0008 winner), diff-ready run metadata, caps/pagination finalised from Phase 0 token data.
- M3: MCP server complete (localhost token, error codes), investigation protocol in eval at full 20-bug corpus.
- Exit: spike thresholds re-measured on the full corpus; MVP = dev+CI usable by a non-author.

## Phase 2 — Data flow + diff
- M4: value-version/data-flow edges quality (incl. TODO on field-read events from trace-schema), `diff_runs` tool.
- M5: Async hand-off coverage (executor/CompletableFuture fixtures B11–B13 green), partial-coverage marking.
- Exit: diff + async categories at parity with other taxonomy categories in eval.

## Phase 3 — JDI deep dive + confirmation
- M6: `jdi-driver` (breakpoints, allow-listed conditions), sandboxing per security-privacy.md.
- M7: `confirm_hypothesis` tool + investigation protocol requires it; study-notes from mcp-debugger/karellen (ADR-0003 TODO) closed.
- Optional M8: minimal human trace UI (Cytoscape.js or React Flow + elkjs — license check in stack.md first), only if TODO in ADR-0002 resolves yes.
- Exit: confirmed-vs-unconfirmed rate reported in eval; no un-sandboxed replay path exists.

## Phase 4 — Second language (Python)
- M9: Schema-neutrality audit against trace-schema Python mapping; `sys.monitoring` probe spike.
- M10: Python fixtures (port ≥ **Proposed: 5** bug categories) through the unchanged core/MCP.
- Exit: same investigation workflow localises Python bugs with no core changes; else schema ADR superseded with learnings.
