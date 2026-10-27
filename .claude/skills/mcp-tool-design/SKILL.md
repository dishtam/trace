---
name: mcp-tool-design
description: Use when adding, renaming, or changing an MCP tool, its input/output schema, response cap, or pagination. Do NOT use for calling existing tools to investigate a bug (debug-investigation).
---

# MCP Tool Design

Contracts live in [docs/llm-strategy.md](../../../docs/llm-strategy.md).
Constraint: [ADR-0009](../../../docs/decisions/ADR-0009-llm-tools-not-raw-graph.md).

## Steps for a new/changed tool
1. State the one question the tool answers. One tool = one question; verb-first `snake_case`.
2. Define input schema: IDs from prior responses only. No raw SQL, no free-form graph queries, no paths outside the project.
3. Define output shape: summary + items with citable event/invocation IDs + source locations.
4. Set a hard response cap + pagination (`truncated` + cursor). Propose the number with rationale in llm-strategy.md — never ship uncapped.
5. Define structured errors `{code, message, hint}`; distinguish not-found / too-large-narrow-scope / unavailable.
6. Update [docs/llm-strategy.md](../../../docs/llm-strategy.md) contract table in the same change.
7. Add contract tests (schema, cap, pagination, IDs, errors) per testing-ci.md.
8. Safety: read-only by default. Any executing tool (like `confirm_hypothesis`) needs the security-privacy.md sandbox design first.

## Checklist
- [ ] Contract updated in llm-strategy.md (same change)
- [ ] Cap + pagination implemented and tested; no unbounded response path
- [ ] Every factual output item citable by ID
- [ ] CLAUDE.md tool list still accurate (update if renamed/added)
- [ ] `debug-investigation` can actually use it (a protocol step references it, or none needed — stated)
