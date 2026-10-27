# LLM Strategy

Constraints: ADR-0006 (external API allowed, redaction default-on), ADR-0009
(tools only, never raw graph), ADR-0002 (agent over MCP is the v1 consumer).

> **Verification note (2026-10-27):** the `claude-api` skill is **not available
> in this environment**, so model/pricing facts below come from web search of
> anthropic.com. Treat every model ID and price as TODO(verify) against the
> official pricing/docs pages at implementation time — do not code against
> this table unverified.

## What search verified (official anthropic.com sources)

| Fact | Source |
|---|---|
| Opus-class: `claude-opus-4-8` at $5 / $25 per M tokens (in/out); a newer Opus 5 announcement at the same $5/$25 | [Opus 4.8](https://www.anthropic.com/news/claude-opus-4-8), [Opus 5](https://www.anthropic.com/news/claude-opus-5) |
| Sonnet-class: Sonnet 4.6 at $3 / $15 per M tokens, model id pattern `claude-sonnet-*` | [Sonnet 4.6](https://www.anthropic.com/news/claude-sonnet-4-6) |
| Fable 5 at $10 / $50 per M tokens (`claude-fable-5`) | [Fable 5](https://www.anthropic.com/news/claude-fable-5-mythos-5) |
| Anthropic publishes a dated list-price sheet (e.g. 2026-09-28) covering model + caching tiers | [Anthropic List Prices 2026-09-28 (PDF)](https://www-cdn.anthropic.com/files/4zrzovbb/website/f237fd89990be52c0f683eb5c9d19bc17667f10c.pdf) |

TODO(verify): current recommended model IDs per tier, exact prices, prompt-caching
write/read multipliers and TTLs, and context limits — question: pin from the
official pricing page + prompt-caching docs on the day `mcp-server`/agent client
work starts. Third-party summaries found in search (cache reads ≈ 0.1× base input,
5-min/1-h cache writes at a premium) are **not** sufficient to design against.

## Model per step (by tier; exact IDs = TODO(verify) above)

| Step | Tier | Why |
|---|---|---|
| Triage (read `get_failure_point`, pick first `trace_back`) | Cheapest/fastest tier (Haiku-class) | Mechanical routing; wrong picks are cheap to redo |
| Trace-back reasoning (walk slice, read source, form hypothesis) | Mid tier (Sonnet-class) | Best cost/capability for multi-step tool reasoning; this is the default investigation model |
| Hard cases / final synthesis when mid-tier stalls | Top tier (Opus-class) | Escalation only: TODO(decide): escalation trigger (e.g. N failed hypotheses) |
| Summarisation of tool results for humans | Cheapest tier | Compression, not reasoning |

## Budget

- **Proposed: ≤ $0.50 per investigation** (spike-acceptance T4) — rationale: forces slice-guided (not repo-pasting) behaviour; at mid-tier prices this allows a few hundred K tokens.
- **Proposed: hard token cap per investigation, TODO(decide) exact number from Phase 0 measurements** — when exceeded: stop, report partial evidence + "budget exceeded", never silently continue or downgrade evidence quality.
- Use **prompt caching** for the stable prefix (investigation protocol + tool definitions + project context) once TODO(verify) confirms current caching rules; per-run trace data is never part of the cached prefix.
- The baseline eval agent gets the same model + budget (evaluation.md fairness).

## MCP tool contracts

Caps are **Proposed** (rationale: one tool response must stay a small fraction
of the investigation budget; tune from Phase 0 token measurements). Every
response: `{summary, items[] with ids, truncated, cursor?}`. Errors:
`{code, message, hint}`.

| Tool | Purpose | Input | Output shape | Proposed cap |
|---|---|---|---|---|
| `get_failure_point` | Start: what failed | `{run_id}` | exception, location, top frames, bad value summary | 1 failure + **Proposed: 20 frames** — the failure neighbourhood, not the whole stack history |
| `trace_back` | Backward slice from a value/exception | `{run_id, target_id, max_depth?}` | ordered value-version/statement list with event IDs | **Proposed: 100 items/page** — slices that matter are tens of items; larger means "narrow the target" |
| `get_call_subtree` | Expand one invocation | `{run_id, invocation_id, depth?}` | invocation tree summaries | **Proposed: 200 nodes/page** |
| `get_source` | Source for a symbol/location | `{symbol_id or class+method}` | snippet + location | **Proposed: 120 lines** — one method plus context; files are drilled into, not dumped |
| `diff_runs` | Pass vs fail first divergence | `{pass_run_id, fail_run_id}` | first divergent invocation + context | **Proposed: 50 items** around divergence |
| `confirm_hypothesis` | Replay verification via JDI | `{run_id, location, condition}` | `confirmed|refuted|unreproducible` + evidence event IDs | Single verdict + evidence; sandboxed (security-privacy.md) |

Example (illustrative shape only):
```json
→ trace_back {"run_id":"r-123","target_id":"exc-9","max_depth":20}
← {"summary":"11 events produced the null tax value",
    "items":[{"event_id":61204,"kind":"return","where":"TaxRepository.find:57","value":"null"},
              {"event_id":61231,"kind":"assign","where":"TaxService:41","value":"tax=null"}],
    "truncated":false}
```

## Investigation protocol & final report

Protocol = skill `debug-investigation`: failure point → trace back → one
hypothesis (event-ID-cited) → `confirm_hypothesis` → report. No root-cause
claim without verification or an explicit `unconfirmed` label.

Final report format (always):
```text
Root cause: <statement + class/method/line> (or: Unconfirmed hypothesis)
Evidence: event IDs + locations, in causal order
Verification: confirmed | refuted | unreproducible (+ replay evidence IDs)
Suggested fix: <smallest change>   Regression test: <idea>
Cost: tokens / $ / tool calls
```
