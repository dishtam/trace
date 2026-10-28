---
name: trace-event-schema
description: Use when adding or changing a trace event type, event field, graph node/edge type, wire format, or storage column. Do NOT use for merely querying traces or for value-capture policy (that is instrumentation-safety).
---

# Trace Event Schema

Source of truth: [docs/trace-schema.md](../../../docs/trace-schema.md) (v0.1).
Storage context: [ADR-0008](../../../docs/decisions/ADR-0008-storage.md).
Never restate the schema in this skill — edit the doc.

## Steps
1. Edit `docs/trace-schema.md` first, before any producer or consumer code: new/changed event or field, required/optional, meaning, and which graph node/edge it derives.
2. Bump `schema_version` per the versioning rules there (additive optional = minor; meaning change/removal = major).
3. Update the DDL + indexes in the same doc section, and note SQLite/DuckDB dialect impact.
4. Then update producer (`agent/`), store, and graph derivation code — in that order.
5. Update/add tests: round-trip (emit → store → query), derivation, `trace_back` CTE case using the new element.
6. Check language neutrality: a Python producer must be able to emit it (see schema doc mapping table).

## Checklist
- [ ] Schema doc updated + version bumped
- [ ] Every new payload field bounded (cap/truncation) — else reject
- [ ] DDL + indexes updated; migration/refusal behaviour for old runs stated
- [ ] Producer + store + graph + tests updated in one change
- [ ] No silent event drops introduced (drop counts + reason still recorded at session end)
- [ ] No Java-only field names/types leaked into the neutral schema
