# ADR-0004: License — Apache-2.0

## Context
Licensing intent constrains every dependency choice (copyleft deps, embedded DBs, graph libraries) and whether evaluation artefacts can be published.

## Decision
The project is **open source under Apache-2.0**.

## Consequences
- (+) Permissive + patent grant; enterprise-friendly for a debugger that runs inside user JVMs.
- Every dependency in [stack.md](../stack.md) must be Apache-compatible; flagged there. TODO(verify): per-dep license at pinned version — question: any GPL/LGPL/SSPL in the transitive set?
- (−) No copyleft protection against closed forks. Accepted.

## Status
Accepted — 2026-10-27 (owner, Phase 1 batch).
