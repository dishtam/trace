---
name: trace-event-schema
description: Use when adding or changing a trace event type, graph node/edge type, event schema field, wire format, or storage column for TRACE recordings.
---

# Trace Event Schema

## Event types (v0 — the only producers may emit these)

| Event | Required fields (plus common fields below) |
|---|---|
| `call` | `invocationId`, `parentInvocationId?`, `class`, `method`, `signature`, `sourceRef`, `argSnapshots[]` |
| `return` | `invocationId`, `returnSnapshot XOR thrownRef`, `durationNanos` |
| `assign` | `invocationId`, `variableRef` (local/field/array-elem), `snapshot`, `sourceRef` |
| `throw` | `exceptionId`, `type`, `message`, `throwingLocation`, `causeId?` |
| `thread-switch` | `fromThreadId`, `toThreadId`, `reason` (start/join/handoff — TODO: enumerate) |

Common fields on every event: `schemaVersion, traceId, eventId` (monotonic per
session), `timestampNanos, threadId, language, sessionId`.
Snapshots: `{kind: primitive|string|enum|null|object-ref|array, value?, typeName, truncated?, objectId?}`.

## Graph types

- Nodes: `MethodInvocation`, `ValueVersion` (SSA-style: one node per assigned
  value), `Exception`, `Location`. TODO: `Thread`/`IO` nodes — not decided.
- Edges: `CALLS`, `RETURNS_TO`, `DATA_FLOW` (producer → consumer),
  `LAST_WRITER` (read → most recent write), `THROWS`, `CAUSED_BY`,
  `CONTROL_FLOW`. TODO: async hand-off edge name/semantics — not decided.

## Rules for adding a new event type

1. Schema first: add the type + fields here and bump `schemaVersion` before
   writing any producer or consumer code.
2. Language-neutral: no Java-only types/names in fields. A Python/Node
   producer must be able to emit it unchanged.
3. Bounded: every payload field has a size cap or truncation rule (see
   `instrumentation-safety`). Unbounded fields are rejected.
4. Storage: define the SQLite/DuckDB column mapping and the recursive-CTE
   slice impact in the same change. TODO: exact DDL — not decided.
5. Tests: add a round-trip test (emit → store → query) and one backward-slice
   case that uses the new event.
6. Never silently drop events. If the pipeline sheds load, emit/record the
   dropped count + reason on session end.
