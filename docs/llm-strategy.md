# LLM Strategy

Constraints: ADR-0006 (external API allowed, redaction default-on), ADR-0009
(tools only, never raw graph), ADR-0002 (agent over MCP is the v1 consumer),
ADR-0010 (pluggable provider: Anthropic Claude and Meta Muse Wonder behind one
`LlmClient`; default chosen from Phase 0 results).

This file is about the LLM **inside TRACE**. The coding agent used to build
TRACE (Muse Code) is separate — see [muse-setup.md](muse-setup.md).

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

### Meta Model API (Muse models) — found 2026-10-27, not yet verified on Meta's pricing page

| Fact | Source |
|---|---|
| Muse Wonder available on the Meta Model API and in Muse Code; Muse Wonder Full for Muse Code subscribers | [Meta announcement](https://about.fb.com/news/2026/10/introducing-muse-wonder-models/), [model page](https://dev.meta.ai/models/muse-wonder) |
| Muse Wonder at $4 / $20 per M tokens (in/out) | [Meta for Developers post](https://www.facebook.com/MetaforDevelopers/posts/muse-wonder-is-available-today-on-meta-model-api-and-in-muse-code-and-muse-wonde/1552628546897475/) |
| Muse Spark 1.3 at $1.25 / $4.25 per M tokens (third-party comparison) | [Paradev comparison](https://paradev.com/compare/claude-code-vs-muse-code) |
| Cheaper "Contributor" plans/models let Meta train on submitted data | Same comparison + Muse Code docs (Contributor mode) |

Verified 2026-10-28 (S0-7, official dev.meta.ai): Meta Model API model ID for
Muse Wonder is `muse-wonder-1.0` (Standard mode only on the API; Wonder Full is
Muse Code subscription-only and is never used here). Context 1M tokens.
Pricing $4 in / $20 out per MTok (cached input $0.20). Endpoint used:
`POST https://api.meta.ai/v1/chat/completions` (Chat Completions surface),
`Authorization: Bearer $MODEL_API_KEY`, tools in `tools` with
`{type:"function", function:{name, description, parameters}}`; the model
answers with `tool_calls` on the assistant message. Anthropic verified the
same day (platform.claude.com): mid-tier model `claude-sonnet-5-5`,
$2 in / $10 out per MTok, 1M context, Messages API tool use
(`tools:[{name,description,input_schema}]`, `tool_use`/`tool_result` blocks).
**Never use a Contributor-mode model/plan for TRACE investigations**
(captured values = user code data; ADR-0006): the Meta client here only ever
calls the pay-as-you-go Model API with `muse-wonder-1.0` Standard mode.

TODO(verify): current recommended model IDs per tier, exact prices, prompt-caching
write/read multipliers and TTLs, and context limits — question: pin from the
official pricing page + prompt-caching docs on the day `mcp-server`/agent client
work starts. Third-party summaries found in search (cache reads ≈ 0.1× base input,
5-min/1-h cache writes at a premium) are **not** sufficient to design against.

## Model per step (by tier; exact IDs = TODO(verify) above)

| Step | Tier | Claude candidate | Meta candidate | Why |
|---|---|---|---|---|
| Triage (read `get_failure_point`, pick first `trace_back`) | Cheapest/fastest | Haiku-class | Muse Spark | Mechanical routing; wrong picks are cheap to redo |
| Trace-back reasoning (walk slice, read source, form hypothesis) | Mid / default | Sonnet-class | Muse Wonder | Best cost/capability for multi-step tool reasoning; this is the default investigation model |
| Hard cases / final synthesis when the default stalls | Top | Opus-class | Muse Wonder Full — TODO(verify) API availability (announced for Muse Code subscribers) | Escalation only: TODO(decide): escalation trigger (e.g. N failed hypotheses) |
| Summarisation of tool results for humans | Cheapest | Haiku-class | Muse Spark | Compression, not reasoning |

Phase 0 runs the evaluation once per provider with the mid/default tier (ADR-0010).

## Budget

- **Proposed: ≤ $0.50 per investigation** (spike-acceptance T4), per provider — rationale: forces slice-guided (not repo-pasting) behaviour; at mid-tier prices this allows a few hundred K tokens (output-heavy runs get fewer on Muse Wonder's $20/M output price).
- **Proposed: hard token cap per investigation, TODO(decide) exact number from Phase 0 measurements** — when exceeded: stop, report partial evidence + "budget exceeded", never silently continue or downgrade evidence quality.
- Use **prompt caching** for the stable prefix (investigation protocol + tool definitions + project context) once TODO(verify) confirms each provider's caching rules; per-run trace data is never part of the cached prefix.
- The baseline eval agent gets the same provider, model + budget (evaluation.md fairness).
- Tool schemas are plain JSON Schema so both providers accept them; MCP contract tests run against each provider (ADR-0010).

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

S0-5 implementation notes (2026-10-28): `target_id` (trace_back) and
`invocation_id` are the integer `event_id` / `invocation_id` values returned by
prior tool responses — IDs only, no SQL or raw graph access (ADR-0009).
Pagination cursor is an opaque offset token returned as `cursor` whenever
`truncated` is true; pass it back unchanged. "Uncaught" (get_failure_point) is
derived at schema v0.1: a `throw` whose invocation has no normal `return`
(the agent emits no `catch` events yet). Errors are `{code, message, hint}`
with codes RUN_NOT_FOUND, FAILURE_NOT_FOUND, TARGET_NOT_FOUND,
INVOCATION_NOT_FOUND, SOURCE_NOT_FOUND, INVALID_ARGUMENT, UNAVAILABLE.
`trace_back` at v0.1 walks call ancestry + exception-propagation edges;
data-flow edges join automatically once assign/field-write capture lands.

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
