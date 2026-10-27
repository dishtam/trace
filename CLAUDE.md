# TRACE — AI execution-graph debugger

Records an execution graph (calls, values, exceptions, thread hand-offs) from a
running Java program; an LLM traces back from failure to root cause via MCP
tools over that graph — never by grepping, never by seeing the raw graph.
Java first, Python later. Dev + CI only in v1. Apache-2.0. Kotlin + Gradle KTS.

## Docs (read before designing; decisions win over chat history)

- [docs/architecture.md](docs/architecture.md) — components, data flow, repo layout
- [docs/trace-schema.md](docs/trace-schema.md) — event schema v0.1, values, DDL, slicing
- [docs/decisions/](docs/decisions/README.md) — ADR-0001..0009 (env, consumer, JDI, license, build, data policy, hybrid capture, storage, tools-not-graph)
- [docs/spike-acceptance.md](docs/spike-acceptance.md) — Phase 0 GO/PIVOT/STOP gate
- [docs/evaluation.md](docs/evaluation.md) — baseline agent, Defects4J, fixtures, scoring
- [docs/demo-app-spec.md](docs/demo-app-spec.md) — seeded-bug demo service spec (20 bugs)
- [docs/security-privacy.md](docs/security-privacy.md) — threat model, redaction, sandboxing
- [docs/llm-strategy.md](docs/llm-strategy.md) — models, budgets, MCP tool contracts
- [docs/testing-ci.md](docs/testing-ci.md) — test pyramid, overhead gate, CI matrix
- [docs/stack.md](docs/stack.md) — verified dependency status + license flags
- [docs/roadmap.md](docs/roadmap.md) — Phase 0 tasks S0-1..S0-9 → Phase 4

## Repo layout

`agent/ collector/ store/ graph/ jdi-driver/ mcp-server/ eval/ demo-app/ docs/`
(details + dependency rules: architecture.md). No module cycles.

## Build / test commands

TODO(decide) — no build exists yet. Add real commands here in the same change
that creates the Gradle build; delete this line then.

## Non-negotiable rules

1. **LLM never sees the raw graph** — only capped MCP tool responses (ADR-0009).
2. **No root-cause claim without verification** (`confirm_hypothesis` or explicit "unconfirmed").
3. **Instrumentation-safety checklist on every `agent/` change** (skill). Never stringify lazy proxies/unsafe values; redaction on by default, in-agent.
4. **Overhead budget:** scoped capture stays within spike-acceptance T1/T2; measured, not asserted.
5. **Unknowns are `TODO(decide)` / `TODO(verify)` with the question.** Never invent versions, prices, caps, or facts.
6. Ask before: commits, pushes, installs, settings/hooks changes, anything outward-facing.

## Which skill for which task

| Task | Skill |
|---|---|
| Event/field/schema change | `trace-event-schema` |
| Anything in `agent/` | `instrumentation-safety` (mandatory) |
| Seeding a bug / Defects4J case | `add-bug-fixture` |
| Running/reporting evaluation | `eval-localization` |
| Investigating a failure (LLM) | `debug-investigation` |
| Adding/changing an MCP tool | `mcp-tool-design` |
| Making/reversing a design choice | `adr` |

Skills live in `.claude/skills/<name>/SKILL.md` (mirrored guidance may exist in `.agents/skills/` — `.claude/` is canonical per owner).

## Commit style

TODO(decide): commit convention — question: Conventional Commits? (Proposed:
Conventional Commits, one logical change per commit, ADR/skill updates in the
same commit as the decision/change they document.) Do not commit unless asked.
