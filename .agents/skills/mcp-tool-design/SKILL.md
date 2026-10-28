---
name: mcp-tool-design
description: Use when adding, renaming, or changing an MCP tool, its input/output schema, response cap, or pagination. Do NOT use for calling existing tools to investigate a bug (debug-investigation).
---

# MCP Tool Design

Contracts live in [docs/llm-strategy.md](../../../docs/llm-strategy.md).
Constraint: [ADR-0009](../../../docs/decisions/ADR-0009-llm-tools-not-raw-graph.md).

Current tools: `get_failure_point`, `trace_back`, `get_call_subtree`,
`get_source`, `diff_runs`, `confirm_hypothesis`.

## Steps for a new/changed tool
1. State the one question the tool answers. One tool = one question; verb-first `snake_case`.
2. Define input schema: IDs from prior responses only. No raw SQL, no free-form graph queries, no paths outside the project. Every argument typed; required ones minimal, optional ones defaulted.
3. Define output shape: summary + items with citable event/invocation IDs + source locations.
4. Set a hard response cap + pagination (`truncated` + cursor). Propose the number with rationale in llm-strategy.md — never ship uncapped.
5. Define structured errors `{code, message, hint}`; distinguish not-found / too-large-narrow-scope / unavailable.
6. Update [docs/llm-strategy.md](../../../docs/llm-strategy.md) contract table in the same change.
7. Add contract tests (schema, cap, pagination, IDs, errors) per testing-ci.md.
8. Safety: read-only by default. Any executing tool (like `confirm_hypothesis`) needs the security-privacy.md sandbox design first.
9. Provider-neutral: tool schemas must work with every LLM provider in [ADR-0010](../../../docs/decisions/ADR-0010-pluggable-llm-provider.md) — no vendor-specific schema features.

## Checklist
- [ ] Contract updated in llm-strategy.md (same change)
- [ ] Cap + pagination implemented and tested; no unbounded response path
- [ ] Every factual output item citable by ID
- [ ] Tool list here and in AGENTS.md still accurate (update if renamed/added)
- [ ] `debug-investigation` can actually use it (a protocol step references it, or none needed — stated)
