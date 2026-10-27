# ADR-0001: Target environment for v1 — local dev + CI

## Context
Full-fidelity capture has high overhead and records sensitive values. Production capture adds privacy, overhead, and attack-surface work that would block the Phase 0 go/no-go question (does trace-back localise bugs better than grep?).

## Decision
v1 targets **local developer machines and CI only**. No staging or production capture in v1.

## Consequences
- (+) Threat model scoped to localhost; retention/redaction still required (see [security-privacy.md](../security-privacy.md)).
- (+) CI gives reproducible failing runs, ideal for `diff_runs` and evaluation.
- (−) No production-only bug claims in v1 marketing/docs.
- Revisit trigger: Phase 2+ only, with a new ADR covering sampling, redaction audit, and overhead in prod.

## Status
Accepted — 2026-10-27 (owner, Phase 1 batch).
