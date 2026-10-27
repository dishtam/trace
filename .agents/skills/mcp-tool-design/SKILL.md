---
name: mcp-tool-design
description: Use when adding, renaming, or changing a TRACE MCP tool, its arguments, its response shape, pagination, or response-size caps.
---

# MCP Tool Design

## Current tools (do not rename without updating CLAUDE.md + this skill)

`get_failure_point`, `trace_back`, `get_call_subtree`, `get_source`,
`diff_runs`, `confirm_hypothesis`

## Rules

1. **Naming:** `snake_case`, verb-first, named by the question answered
   (`trace_back`, not `graph_query_2`). One tool = one question.
2. **Arguments:** IDs from prior tool responses (`invocationId`, `valueId`,
   `traceId`), never raw SQL, file paths outside the project, or free-form
   graph queries. Every argument is typed, required ones minimal, optional
   ones defaulted and documented here when added.
   TODO: exact JSON schemas per tool — not decided.
3. **Response caps (hard):** every list response is paginated with an
   explicit `truncated: true` + cursor when capped. No unbounded subtrees,
   slices, or source files.
   TODO: numeric caps (nodes/events/chars per response) — not decided;
   propose them from `eval-localization` context-size measurements, do not
   guess them into code.
4. **Citable output:** every factual item carries its `eventId`/`invocationId`
   and source location so the LLM can cite it (see `debug-investigation`).
5. **Errors:** structured `{code, message, hint}`; "not found", "trace too
   large — narrow the scope", and "replay unavailable" are distinct codes.
   TODO: error code list — not decided.
6. **Safety:** tools are read-only except `confirm_hypothesis` (re-executes
   the target). That tool must require an explicit target + condition and
   run the debuggee sandboxed (TODO: sandbox design — not decided; a
   debugger driven by an agent is an attack surface).
7. Any new tool needs: schema here, a cap, a citable ID in output, and one
   `debug-investigation` step that uses it. Otherwise it does not ship.
