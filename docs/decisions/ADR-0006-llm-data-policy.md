# ADR-0006: LLM data policy — external API allowed, redaction on by default

## Context
Investigations send code snippets and captured values to an LLM. Values may contain secrets/PII. Alternatives: local-only (limits model quality), external with opt-in redaction (unsafe default).

## Decision
**External LLM API is allowed. Redaction is on by default** and runs in the agent before storage, so stored traces and LLM prompts both only ever see redacted summaries. A local-only mode is TODO(decide): question — is local-only a v1 requirement for any target user, or Phase 2?

## Consequences
- (+) Best available models for trace-back reasoning ([llm-strategy.md](../llm-strategy.md)).
- Redaction rules live in [security-privacy.md](../security-privacy.md); bypassing redaction requires an explicit per-run flag and is recorded in run metadata.
- (−) Residual risk: novel secret shapes that patterns miss. Mitigated by value-size caps and the "unsafe to render" marker (trace-schema.md).

## Status
Accepted — 2026-10-27 (owner, Phase 1 batch).
