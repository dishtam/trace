# Trace Schema — v0.1

Version: **0.1** (2026-10-27). Change process: skill `trace-event-schema`
(schema change + version bump + DDL + tests in one change). Storage decision:
[ADR-0008](decisions/ADR-0008-storage.md). All limits below are **Proposed** unless stated.

## Common fields (every event)

| Field | Type | Req | Meaning |
|---|---|---|---|
| `event_id` | int64 | ✓ | Unique within run; assigned by collector on ingest |
| `run_id` | string (UUID) | ✓ | One recorded execution (one app/test/request session — TODO(decide): run granularity per surface) |
| `schema_version` | string | ✓ | e.g. `0.1`; see Versioning |
| `seq` | int64 | ✓ | Monotonic sequence per thread (per-thread total order key) |
| `global_seq` | int64 | ○ | Collector ingest order; NOT a causality claim across threads |
| `timestamp_ns` | int64 | ✓ | Wall-clock nanos at capture. Clock assumptions §Ordering |
| `thread_id` | int64 | ✓ | JVM thread id; thread identity recorded by `thread-start` |
| `event_type` | enum | ✓ | One of the types below |
| `invocation_id` | int64 | ○ | Owning method invocation (absent on thread events) |
| `parent_invocation_id` | int64 | ○ | Caller invocation; null at thread entry |

Code location (on `call`, `assign`, `field-write`, `throw`, `catch`):

| Field | Type | Req | Meaning |
|---|---|---|---|
| `class_name` | string | ✓ | Fully-qualified, JVM internal or dotted — TODO(decide): pick one form |
| `method_name` | string | ✓ | Method name (`<init>` for constructors) |
| `descriptor` | string | ✓ | JVM method descriptor (disambiguates overloads) |
| `bci` | int | ○ | Bytecode index of the instruction; -1 if unavailable |
| `source_file` | string | ○ | From debug info; may be absent |
| `source_line` | int | ○ | From `LineNumberTable`; requires `-g`; -1 if absent |

## Event types

### `call`
Method entered. Fields: common + location + `this_summary` (Value, receiver; absent for static) + `arg_summaries` (Value[] in parameter order) + `capture_level` (`summary|detailed`, how this call was instrumented).

### `return`
Method exited normally. Fields: `invocation_id`, `return_summary` (Value; absent for void), `duration_ns`.

### `throw`
Exception thrown. Fields: location (throw site) + `exception_type`, `exception_message_summary` (Value-truncated string), `exception_identity` (identity hash — same exception rethrown keeps identity), `cause_event_id` (○, the `throw` of the cause).

### `catch`
Exception caught. Fields: `throw_event_id` (which throw this catches), location (handler), `invocation_id` (catching frame). Uncaught exceptions have a `throw` with no matching `catch` — that is the normal failure-point shape.

### `assign` (local variable)
Local assigned. Fields: `invocation_id`, location, `var_index` (slot), `var_name` (○, needs `-g`), `value` (Value). Emitted only at `detailed` capture level for allow-listed classes (cost — see instrumentation-safety).

### `field-write`
Field written. Fields: `invocation_id`, location, `owner_class`, `field_name`, `field_descriptor`, `target_identity` (identity hash of object written; absent for static), `value` (Value).

### `thread-start`
Thread began. Fields: `thread_id`, `thread_name`, `parent_thread_id` (○, JVM-level starter if known).

### `thread-handoff`
Causality crosses threads. Fields: `from_thread_id`, `to_thread_id` (○ at submit time — may be unknown until execution), `handoff_kind` (`executor-submit|completable-future|thread-start|TODO(decide): full enum`), `task_identity` (identity hash of Runnable/Future), `submission_invocation_id`. This event is the happens-before edge source. Coverage of async frameworks is incremental; missing handoffs must be knowable (collector marks runs `async_coverage: partial` — TODO(decide): exact marker).

### `snapshot-marker`
Ring-buffer/failure dump boundary. Fields: `marker_kind` (`dump-start|dump-end|drop-notice`), `dropped_event_count` (for drop-notice; drops are never silent), `reason` (○).

## Value representation

| Field | Type | Meaning |
|---|---|---|
| `type_name` | string | Runtime class (`int`, `java.lang.String`, `com.acme.Order`) |
| `identity_hash` | int64 ○ | Identity hash of the object; lets the same object be followed without rendering it. Absent for primitives |
| `summary` | string | Bounded rendering (below). Empty when `unsafe_to_render` |
| `redacted` | bool | True if redaction rewrote/blocked the summary (ADR-0006) |
| `unsafe_to_render` | bool | True for proxies/lazy collections/objects whose rendering can cause side effects. Summary is then type + identity only. Rule: **never call `toString()` on such values** (skill `instrumentation-safety`) |
| `truncated` | bool | True if any limit below cut the summary |

Truncation limits — **Proposed** (rationale: keep an event small enough that 1M events stay queryable; tune via Phase 0 volume metrics):
- **Proposed:** max summary chars `256` — most identifiers/values that matter for localisation fit; long strings rarely carry the root cause.
- **Proposed:** max collection/map elements rendered `8` (+ total size when known) — shows shape without payload blowup.
- **Proposed:** max rendering depth `2` — one level of fields; deeper is drill-down via JDI, not the trace.
- **Proposed:** arrays render as `[type; length=N; first 8]` same rationale as collections.

## Ordering and causality

- **Per thread:** `seq` is a total order. All slice walks are per-thread exact.
- **Cross thread:** no global order is claimed. Causality exists only where a `thread-handoff` (or thread-start) event connects threads (happens-before). `global_seq`/`timestamp_ns` must never be presented as cross-thread causality.
- Clock: `timestamp_ns` is wall-clock for display/duration only; durations use a monotonic source in the agent (TODO(verify): agent monotonic clock API choice). Cross-process clock skew is out of scope for v1 (single JVM).

## Graph derivation

| Graph element | Derived from |
|---|---|
| Node: invocation | `call` + matching `return`/`throw` (start, end, args, outcome, thread) |
| Node: value-version | Each `assign`/`field-write`/arg/return value — SSA-style: one node per produced value |
| Edge: `call` | invocation → child invocation (`parent_invocation_id`) |
| Edge: `return` | child invocation → its return value-version |
| Edge: `data-flow` | value-version → later use (arg of a call, field read TODO(decide): field-read events? — question: needed for Phase 1 slice quality, or arg/assign flow suffices for the spike?) |
| Edge: `exception-propagation` | `throw` → catching invocation via `catch.throw_event_id`; chain via `cause_event_id` |
| Edge: `hand-off` | `thread-handoff`: submitting invocation → executing invocation across threads |

`trace_back` = walk `data-flow` + `exception-propagation` + `call` edges backward from the failure value/exception, bounded by depth/result cap.

## Storage mapping (DDL is a spec, not app code)

```sql
CREATE TABLE runs (
  run_id TEXT PRIMARY KEY, schema_version TEXT NOT NULL,
  started_at_ns INTEGER, app TEXT, capture_profile TEXT,
  async_coverage TEXT, dropped_events INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE events (
  event_id INTEGER PRIMARY KEY, run_id TEXT NOT NULL REFERENCES runs(run_id),
  event_type TEXT NOT NULL, seq INTEGER NOT NULL, global_seq INTEGER NOT NULL,
  timestamp_ns INTEGER NOT NULL, thread_id INTEGER NOT NULL,
  invocation_id INTEGER, parent_invocation_id INTEGER,
  class_name TEXT, method_name TEXT, descriptor TEXT,
  bci INTEGER, source_file TEXT, source_line INTEGER,
  payload TEXT  -- JSON: Value summaries + type-specific fields (schema-versioned)
);
CREATE INDEX idx_events_run_seq ON events(run_id, thread_id, seq);
CREATE INDEX idx_events_invocation ON events(run_id, invocation_id);
CREATE INDEX idx_events_type ON events(run_id, event_type);
-- value_versions / edges are derived tables or views over events (Phase 0 task to choose)
```

Dialect notes: recursive CTEs exist in both SQLite and DuckDB; JSON access differs — TODO(verify): single payload-JSON design vs typed columns per engine, decided with ADR-0008 benchmark. Measured 2026-10-28 (S0-8, ADR-0008): DuckDB `INTEGER` is 32-bit, so int64 columns (`event_id`, `global_seq`, `timestamp_ns`, invocation IDs) must be `BIGINT` on DuckDB; DuckDB has no SQLite-style rowid autoincrement, so `event_id` is written explicitly there. ADR-0008's benchmark also found the derived-edges CTE fails T5 at 1M events on both engines; the materialised `edges` table below is required before the Phase 0 gate.

Recursive query shape for `trace_back` (sketch; data-flow edges materialised in `edges(from_event, to_event, kind)`):

```sql
WITH RECURSIVE slice(event_id, depth) AS (
  SELECT :failure_event, 0
  UNION
  SELECT e.from_event, s.depth + 1
  FROM edges e JOIN slice s ON e.to_event = s.event_id
  WHERE e.kind IN ('data-flow','exception-propagation','call') AND s.depth < :max_depth
)
SELECT * FROM slice LIMIT :cap;
```

## Versioning and compatibility

- Additive optional fields: minor bump (`0.1 → 0.2`), old readers must ignore unknown payload keys.
- Removing/renaming fields or changing meaning: major bump (`0.x → 1.0` pre-1.0: any bump may break; state it in the changelog here).
- Every run stores `schema_version`; the store must refuse or migrate mismatched majors explicitly — never silently misread.
- New event type: skill `trace-event-schema` (this file + DDL + tests together).

## Mapping a new language onto v0.1 (Python sketch)

| Schema concept | Python (`sys.monitoring`, 3.12+) |
|---|---|
| `call` / `return` | `PY_START`/`PY_RETURN` monitoring events (TODO(verify): exact event names at implementation time) |
| `assign` | `LINE` + local snapshot diff, or `TODO(decide)`: assignment-event approach per CPython version |
| `throw`/`catch` | `RAISE`/`RERAISE`-family events (TODO(verify)) |
| `thread-handoff` | `threading`/asyncio task creation hooks (TODO(decide)) |
| Value summary | Same Value shape; `unsafe_to_render` for objects with side-effecting `__repr__` |

Java is the only v0.1 producer. The Python column proves neutrality; it is not a v1 commitment.
