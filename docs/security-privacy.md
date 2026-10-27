# Security & Privacy

Environment: dev + CI (ADR-0001). Data policy: ADR-0006. This doc is the
enforcement list; skill `instrumentation-safety` is the per-change checklist.

## Threat model

| Threat | Why it matters here | Mitigation |
|---|---|---|
| Debugger-as-attack-surface | A debugger driven by an agent can execute code in the debuggee. Motivation: the DebugMCP RCE disclosure (Imperva) noted in the feasibility plan — TODO(verify): exact disclosure/CVE reference at Phase 3 design time | Own JDI driver (ADR-0003); expression evaluation allow-list (§ below); sandboxed replays |
| MCP server exposure | MCP server holds traces (code + values) and a replay capability | **Localhost only by default**, per-run auth token, no network listener without explicit flag + ADR. TODO(decide): token storage/rotation |
| JDI expression evaluation | Arbitrary expression eval = arbitrary code execution in target | `confirm_hypothesis` accepts breakpoint + condition from an allow-listed expression grammar; free-form eval is TODO(decide) — default: not supported in v1 |
| Malicious / hostile target code | Instrumented app can attack the agent (hostile `toString`, huge objects, agent-aware behaviour) | Never invoke user code during capture; size/depth caps; agent is non-throwing + reentrancy-guarded (agent never traces itself) |
| Trace data at rest | Stored runs contain source + values | Local files, redacted at capture; retention/deletion § below; encryption at rest TODO(decide) — question: needed for CI artefacts in v1? |
| LLM egress | Prompts leave the machine (ADR-0006 allows, redacted) | Redaction before storage AND before prompt assembly (defence in depth); only capped tool summaries sent (ADR-0009) |

## Data policy

- **Captured:** method calls, arg/return summaries, selected values, exceptions, thread hand-offs, within allow-listed packages only.
- **Default redaction** (in-agent, before storage) — **Proposed** pattern set (rationale: cover the common secret shapes without a false-positive flood; owner tunes):
  - Field/variable names matching `password|passwd|token|secret|api[_-]?key|auth|credential|session` (case-insensitive)
  - Value shapes: email-like, card-number-like (13–19 digits, Luhn-checked TODO(verify): Luhn in-agent cost), `Bearer …`, common key prefixes — TODO(decide): final pattern list + test corpus of true/false positives
  - Annotation/package deny-lists: TODO(decide): mechanism (e.g. `@Redact`-style annotation name, package prefixes) — question: what do we honour in v1?
- Redacted values are stored as `redacted: true` with type only — the raw value is never written.
- **Retention:** TODO(decide): default retention (e.g. runs deleted with CI artefacts) + a `trace delete <run>` path from day 1. What leaves the machine: only redacted LLM prompts (ADR-0006) and whatever the user explicitly exports.
- Bypass: per-run `--no-redact`-style flag is recorded in run metadata and warned loudly. TODO(decide): flag name; never a silent default.

## Sandboxing `confirm_hypothesis` replays

- Replay runs the target in a **separate, disposable process** with: no network by default, working directory = temp copy, environment scrubbed of secrets not required for the bug, wall-clock timeout. TODO(decide): timeout value + OS-level sandbox (container/WSL) vs process-only for v1.
- The replay can only set breakpoints/conditions via `jdi-driver`; it cannot modify sources or the stored trace.
- Replay result is evidence, not truth: non-reproducible bugs report `unconfirmed` (skill `debug-investigation`).
