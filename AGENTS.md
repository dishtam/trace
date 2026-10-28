# TRACE — AI execution-graph debugger

Records an execution graph (calls, values, exceptions, thread hand-offs) from a
running Java program; an LLM traces back from failure to root cause via MCP
tools over that graph — never by grepping, never by seeing the raw graph.
Java first, Python later. Dev + CI only in v1. Apache-2.0. Kotlin + Gradle KTS.

## Docs (read before designing; decisions win over chat history)

- [docs/architecture.md](docs/architecture.md) — components, data flow, repo layout
- [docs/trace-schema.md](docs/trace-schema.md) — event schema v0.1, values, DDL, slicing
- [docs/decisions/](docs/decisions/README.md) — ADR-0001..0016 (env, consumer, JDI, license, build, data policy, hybrid capture, storage, tools-not-graph, LLM provider, JDKs, Gradle, shading, storage path, demo stack, commits)
- [docs/spike-acceptance.md](docs/spike-acceptance.md) — Phase 0 GO/PIVOT/STOP gate
- [docs/evaluation.md](docs/evaluation.md) — baseline agent, Defects4J, fixtures, scoring
- [docs/demo-app-spec.md](docs/demo-app-spec.md) — seeded-bug demo service spec (20 bugs)
- [docs/security-privacy.md](docs/security-privacy.md) — threat model, redaction, sandboxing
- [docs/llm-strategy.md](docs/llm-strategy.md) — models, budgets, MCP tool contracts
- [docs/testing-ci.md](docs/testing-ci.md) — test pyramid, overhead gate, CI matrix
- [docs/stack.md](docs/stack.md) — verified dependency status + license flags
- [docs/roadmap.md](docs/roadmap.md) — Phase 0 tasks S0-1..S0-9 → Phase 4
- [docs/muse-setup.md](docs/muse-setup.md) — developer setup for Muse Code (trust, skills, privacy, hooks, MCP)

## Repo layout

`agent/ collector/ store/ graph/ jdi-driver/ mcp-server/ eval/ demo-app/ docs/`
(details + dependency rules: architecture.md). No module cycles.

## Build / test commands

Gradle wrapper (committed). Windows and Linux are identical except the script name:

```text
gradlew.bat build            # Windows: full build + tests (agent, store, collector, demo-app; stubs compile)
./gradlew build              # Linux/macOS: same
gradlew.bat :agent:test      # agent unit + subprocess integration tests
gradlew.bat :store:test :collector:test
gradlew.bat :demo-app:test   # Spring Boot request-path test
gradlew.bat checkArchitecture  # forbidden inter-module dependency guard
gradlew.bat :demo-app:bootJar :agent:shadowJar   # artefacts for the overhead run
powershell -File demo-app/overhead.ps1            # S0-4 overhead smoke (Windows; bash equivalent: same java commands)
gradlew.bat :collector:run --args="<events.jsonl> <trace.db> demo-app"
```

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

Skills live in `.agents/skills/<name>/SKILL.md` — the only skill location. Don't
add copies under `.claude/skills/` or `.codex/skills/`: Muse Code scans those too
and would see duplicate skill IDs.

## Working with Muse Code

- **Model:** Muse Wonder for design-heavy work (schema, ADRs, agent internals); the default Muse Spark is fine for routine edits. Reasoning effort `high`; `xhigh` for schema/ADR/instrumentation design. Exact model IDs: check `/models` (see docs/muse-setup.md).
- **Before building a phase:** run `/grill` on the relevant doc (e.g. trace-schema.md, spike-acceptance.md) to settle open `TODO(decide)` items, then `/plan` per roadmap task. Record settled decisions with the `adr` skill.
- **Subagents:** read-only research children may share the checkout; any child that writes code gets worktree isolation. Two writers never touch the same module.
- **Headless/CI:** evaluation runs use `muse exec --prompt-file … --max-model-steps <n>`; gate on our own test/eval command, never on `muse exec`'s exit code alone.
- **Never run `muse init --force`** — it overwrites this file.
- **Hooks** belong in `.muse/hooks.json` (none yet — planned hooks in docs/muse-setup.md). MCP servers go in the user's `~/.config/muse/settings.json`.
- **Privacy (ADR-0006):** keep Contributor mode off for this repo.

## Commit style

Conventional Commits, **one commit per roadmap task** (ADR-0016), ADR/skill
updates in the same commit as the change they document. Ask before every
commit. Never push without explicit instruction.
