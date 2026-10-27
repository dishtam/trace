# ADR-0009: The LLM sees MCP tools, never the raw graph

## Context
A recorded execution graph is far too large for a prompt (see trace volume, [spike-acceptance.md](../spike-acceptance.md)) and contains redacted-but-sensitive values. Pasting subgraphs would blow the token budget, leak data, and make answers unverifiable.

## Decision
The LLM interacts with the graph **only** through the six capped MCP tools (`get_failure_point`, `trace_back`, `get_call_subtree`, `get_source`, `diff_runs`, `confirm_hypothesis`). Tool responses are summaries carrying IDs for drill-down (contracts in [llm-strategy.md](../llm-strategy.md)). No tool returns an unbounded graph, trace dump, or full event log.

## Consequences
- (+) Context stays bounded; every LLM claim can cite event/invocation IDs.
- (+) Redaction + caps are enforced at one boundary.
- (−) Tool design becomes critical-path work (skill `mcp-tool-design`); a missing query means a new tool, not a prompt hack.
- Any exception to this ADR requires superseding it, not a one-off flag.

## Status
Accepted — 2026-10-27.
