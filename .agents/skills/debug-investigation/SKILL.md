---
name: debug-investigation
description: Use when the LLM investigates a failure with TRACE tools: tracing back from a failure point, forming a root-cause hypothesis, or answering why a value was wrong.
---

# Debug Investigation

Follow this order. Do not skip to a conclusion.

## Method

1. **Start at the failure.** Call `get_failure_point`. State, in one line:
   what failed, where (file/line/invocation), and the bad value or exception.
2. **Trace back, don't search.** Call `trace_back` on the bad value/exception.
   Walk `DATA_FLOW`/`LAST_WRITER` edges. Use `get_call_subtree` only to
   expand a node the slice already implicates; use `get_source` to read the
   implicated statements. No repo-wide grep as a substitute for the slice.
3. **Form one hypothesis.** "Statement S produced value V because …" —
   citing the event/invocation IDs from steps 1–2. If two hypotheses fit,
   list both and say what evidence would separate them.
4. **Confirm before claiming.** Call `confirm_hypothesis` (JDI replay with
   breakpoint + condition) or, if replay is impossible (non-reproducible,
   external state), say so explicitly and label the result *unconfirmed*.
5. **Report** in this shape: root cause (or *unconfirmed hypothesis*) ·
   evidence (IDs + locations) · confirmation result · suggested fix ·
   regression-test idea. Separate facts (tool output) from inference.

## Hard rules

- Never claim a root cause from narrative plausibility alone.
- Never state a value, call, or location that no tool returned.
- If a tool response is truncated/capped, say what you could not see.
- When a passing run exists, prefer `diff_runs` first-divergence over
  speculation about "suspicious" code.
