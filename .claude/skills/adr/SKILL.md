---
name: adr
description: Use when a design choice is made, reversed, or found to conflict with an existing ADR — write or supersede a record in docs/decisions/. Do NOT use for routine implementation choices that follow from an existing ADR.
---

# ADR

Index + rules: [docs/decisions/README.md](../../../docs/decisions/README.md).

## Steps
1. Check the index: does an ADR already cover this? If you're reversing one, supersede — never edit its Decision away.
2. Take the next number (`ADR-NNNN`), kebab-case slug.
3. Write: **Context** (forces, options considered) / **Decision** (one clear choice) / **Consequences** (+ and −, revisit trigger) / **Status** (`Proposed` until owner accepts, then `Accepted`; superseded: `Superseded by ADR-XXXX`).
4. Add the row to `docs/decisions/README.md`.
5. Update every doc/skill that stated the old choice (search for it) in the same change.
6. Unknowns inside the ADR are `TODO(decide)`/`TODO(verify)` with the question — an ADR may be `Proposed` with open TODOs, but may not present a guess as decided.

## Checklist
- [ ] Next number, no gaps/duplicates; index updated
- [ ] Options considered are named, with why they lost
- [ ] Consequences include at least one negative / cost
- [ ] Affected docs + CLAUDE.md updated in the same change
- [ ] Owner acceptance recorded before Status = Accepted (except Phase 1 batch decisions already given)
