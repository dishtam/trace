# ADR-0016: Commit convention — Conventional Commits, one per roadmap task

## Context
AGENTS.md proposed Conventional Commits; the owner requires asking before every commit and one commit per roadmap task.

## Decision
**Conventional Commits** (`feat:`, `fix:`, `build:`, `docs:`, `test:`, `chore:`), **one commit per roadmap task** (e.g. `feat(agent): S0-2 agent v0 …`), with ADR/skill/doc updates included in the same commit as the change they document. **Ask the owner before every commit; never push without explicit instruction.**

## Consequences
- (+) History maps 1:1 to roadmap tasks; gate evidence is easy to locate.
- (−) Larger commits per task; hunk-splitting discipline needed if a task touches docs + code.

## Status
Accepted — 2026-10-28 (owner, Step 0 grill).
