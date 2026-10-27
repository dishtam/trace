# Testing & CI

All numbers **Proposed** unless stated. Build exists only after Phase 0;
commands stay `TODO(decide)` in CLAUDE.md until then.

## Test pyramid per module

| Module | Tests (fast → slow) |
|---|---|
| `agent/` | Instrumentation correctness on fixture classes: probes fire, values summarised safely, lazy-proxy fixture never rendered, reentrancy guard (agent doesn't trace itself), drop-count on overflow. **Every JDK in the target matrix** for the load/transform tests (matrix below) |
| `store/` + schema | Event round-trip (emit → store → read identical), DDL/index sanity, schema-version refusal/migration behaviour, recursive `trace_back` CTE on hand-built event sets |
| `graph/` | `trace_back` / `diff_runs` / subtree on hand-built graphs with known slices; cap + pagination behaviour; missing-handoff (partial async coverage) reporting |
| `jdi-driver/` | Breakpoint/condition replay on demo-app fixtures; expression allow-list rejects out-of-grammar input; timeout → structured error |
| `mcp-server/` | Contract tests per tool: schema, caps, pagination cursor, citable IDs, error shapes (skill `mcp-tool-design`) |
| `eval/` | Smoke run on **Proposed: 3 bugs** (rationale: catches harness rot without CI cost of the full corpus) on every eval change; full corpus per evaluation.md cadence |

## Overhead regression check

- Benchmark: demo-app request load script (demo-app-spec.md), agent on vs off, same job shape as spike-acceptance overhead metric.
- Threshold: **Proposed: fail if median overhead worsens past 3× (T1) or regresses >10% relative without an ADR note** — rationale: T1 is the product budget; 10% catches slow creep.
- When: on `agent/` changes (path-filtered job) + nightly full run. TODO(decide): nightly vs per-PR for the full matrix at Phase 0 exit.

## CI plan (GitHub Actions — Proposed)

Rationale: GitHub Actions is the default OSS CI for an Apache-2.0 project; TODO(decide) if the owner hosts elsewhere.

| Job | Matrix | Gates merge? |
|---|---|---|
| Build + unit tests | Tool JDK baseline (TODO(verify)), Windows + Linux | ✓ |
| Agent JDK matrix | Target JDKs **Proposed: 11, 17, 21, 25**, Linux (Windows subset: TODO(decide)) | ✓ (Linux), TODO(decide) Windows gating |
| Defects4J eval environment | WSL-equivalent Linux job, Java 8 + 11 toolchains, cached checkout | Nightly/manual only (slow) |
| Overhead regression | Linux, demo benchmark | ✓ for `agent/` PRs |
| Docs/skills lint (links, TODO scan) | — | TODO(decide): tooling |

Caching: Gradle cache + dependency cache keyed on lockfiles; Defects4J checkout cached by pinned commit. No secrets beyond the LLM API key for eval jobs (eval jobs never run on forks with secrets — TODO(decide): eval-on-PR policy).
