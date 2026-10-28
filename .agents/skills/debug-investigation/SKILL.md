---
name: debug-investigation
description: Use when investigating a failure with TRACE MCP tools — tracing back from a failure point, explaining a wrong value, or producing a root-cause report. Do NOT use for benchmark runs (eval-localization) or for free-form codebase exploration without a recorded run.
---

# Debug Investigation

Protocol source: [docs/llm-strategy.md](../../../docs/llm-strategy.md).
Hard constraint: [ADR-0009](../../../docs/decisions/ADR-0009-llm-tools-not-raw-graph.md) — tools only, never raw graph/trace dumps.

## Steps
1. `get_failure_point` — state in one line: what failed, where, bad value/exception.
2. `trace_back` from that target. Walk the returned slice; drill in only via `get_call_subtree` / `get_source` on implicated nodes. No repo-wide grep as a substitute for the slice. If a passing run exists, `diff_runs` first-divergence beats speculation.
3. Form **one** hypothesis citing event/invocation IDs. Two fits? List both + what evidence separates them.
4. `confirm_hypothesis` (sandboxed JDI replay). Impossible to replay → label **unconfirmed**, say why.
5. Report in the llm-strategy format: root cause (or unconfirmed hypothesis) · evidence chain (IDs, causal order) · verification result · suggested fix · regression test · cost.

## Checklist
- [ ] Every factual claim cites a tool-returned ID/location
- [ ] No value/call/location stated that no tool returned
- [ ] Truncated/capped tool responses acknowledged (what couldn't be seen)
- [ ] Root cause claimed only with verification, else labelled unconfirmed — never from narrative plausibility alone
- [ ] Facts (tool output) separated from inference in the report
