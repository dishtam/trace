# ADR-0008: Storage for the spike — benchmark SQLite vs DuckDB in Phase 0

## Context
Trace-back is a backward graph walk. Candidates: SQLite, DuckDB (both embedded, SQL, recursive CTEs for slices, Parquet export path), or an embedded graph DB. Kuzu, once the obvious embedded graph candidate, was **archived upstream in Oct 2025** after the company behind it was acquired (see [stack.md](../stack.md)) — adopting it now would be building on unmaintained code. Neo4j (server) is operationally heavy for a local dev/CI tool.

## Decision
**Spike on one, decide by measurement:** Phase 0 task S0-TODO picks **SQLite first** for the spike (simplest embedded ops, recursive CTE, zero config) and benchmarks the `trace_back` recursive query + ingest rate against the same workload on DuckDB before the Phase 1 storage lock-in.
Rationale for SQLite-first: lowest integration risk for a 2-week spike; DuckDB's columnar analytics advantage matters more for aggregate queries than for per-run slice walks. TODO(decide): final storage after the benchmark — question: which engine keeps p95 `trace_back` under the Proposed budget in spike-acceptance.md at 1M events?

## Consequences
- Schema is written engine-neutrally where practical ([trace-schema.md](../trace-schema.md) DDL notes dialect deltas).
- Graph DB is **not** adopted. Revisit only with measured traversal pain + a maintained candidate.
- (−) Possible Phase 1 migration if DuckDB wins. Accepted; the event log is the source of truth, the store is derived.

## Benchmark (S0-8, 2026-10-28)

Method: synthetic 1,000,068-event run (`eval: trace.eval.StorageBench`, seed 42,
deterministic), shaped like real v0.1 captures (call/return/throw/assign over
4 threads, invocation chains up to ~70 deep, agent-shaped JSON payloads).
Identical DDL (trace-schema.md; DuckDB int64 columns mapped INTEGER → BIGINT,
see dialect delta below) and the identical `trace_back` recursive CTE on both
engines, same machine (i5-11300H, 8 GB, JDK 21.0.10, Windows). Ingest = each
engine's idiomatic bulk write with indexes created first: SQLite through
`SqliteTraceStore` (JDBC batches), DuckDB through its JDBC Appender (plain
JDBC batches on DuckDB were pathologically slow and were rejected as its
ingest path). Query timing: warmup, then timed executions cycling 20 deep
throw targets, full result fetched. Two query shapes were measured because
the first one turned out to be the real decision driver:

| Measurement (1M events, one run) | SQLite | DuckDB 2.0.0.0 |
|---|---|---|
| Ingest (events only) | 99.2 s (~10,100 ev/s) | 48.4 s (~20,700 ev/s) |
| DB file size | 413 MB | 117 MB |
| Edges build (1,000,061 edges) | 3.0 s | 5.6 s |
| `trace_back`, derived-edges CTE (shipped S0-5 SQL) | p50 118 s, p95 155 s (3 reps) | p50 5.2 s, p95 11.6 s (10 reps) |
| `trace_back`, materialised `edges` table | **p50 <1 ms, p95 2 ms** (100 reps) | p50 44 ms, p95 57 ms (100 reps) |

Slice parity between the two query shapes was asserted on both engines
(identical row counts). Raw harness output: `RESULT` lines from
`trace.eval.StorageBench` (rerun: `java -cp eval/build/install/eval/lib/*
trace.eval.StorageBench <dir> 1000000` after `:eval:installDist`).

Findings:
1. The shipped derived-edges CTE fails the T5 budget (≤ 2 s) at 1M events on
   **both** engines — catastrophically on SQLite. Its OR-join can only use
   the `(run_id, …)` index prefix, so every recursive step scans the run
   (EXPLAIN QUERY PLAN, 2026-10-28). DuckDB's parallel scans make it ~25×
   faster but still 5–10 s.
2. With the schema's sketched materialised `edges(from_event, to_event, kind)`
   table, the walk becomes indexed point lookups: SQLite p95 2 ms, DuckDB
   p95 57 ms. SQLite is ~25× faster on the actual hot path.
3. DuckDB wins ingest (~2×) and size (~3.5×, columnar compression). Neither
   matters more than the slice walk for TRACE: ingest is offline
   (collector), and 413 MB per 1M events is acceptable for dev/CI runs.
4. Dialect delta found: DuckDB `INTEGER` is 32-bit (SQLite's has 64-bit
   affinity); int64 columns must be `BIGINT` on DuckDB. DuckDB also lacks
   SQLite-style rowid autoincrement, so `event_id` is written explicitly.

## Refined decision (2026-10-28)

**SQLite stays the Phase 1 engine** (the original pick; not superseded).
DuckDB is not adopted: its advantages (bulk ingest, analytics scans, file
size) do not serve the per-run backward-walk hot path, where SQLite with
materialised edges is 25× faster and already integrated. **Condition:**
`trace_back` must move from the derived-edges CTE to the materialised
`edges` table (or an equivalent indexed structure) before the Phase 0 gate —
on the derived shape neither engine meets T5. That migration is recorded
here as the storage follow-up; it touches store (edges written at ingest)
and graph (query shape), not the event schema. Revisit DuckDB only if
aggregate analytics over many runs become a product requirement.

Consequences added: (+) no storage migration before Phase 1; slice latency
has ~1000× headroom vs T5 on the edges shape. (−) store ingest is ~10k ev/s
at 1M scale and edges add an ingest-time build step (~3 s/1M events);
(−) the DuckDB ingest/size advantages are given up knowingly.

## Implementation of the condition (2026-11-01)

The materialised-edges migration required above has landed (Step 1 of the
S0-9 decision packet; not yet committed — owner review pending):

- Store: `edges(run_id, from_event, to_event, kind)` table + `idx_edges_to`,
  built at ingest inside the `insertEvents` transaction with exactly the
  predecessor rules the derived CTE computed (`SqliteTraceStore.EDGE_SQL`).
  `rebuildEdges` is idempotent; `ensureEdges` backfills DBs written before
  the table existed on first `trace_back`. Event schema unchanged — this is
  a derived table, so `schema_version` stays 0.1 (see trace-schema.md
  Storage mapping, which now carries the edges DDL).
- Graph: `TraceQueries.traceBack` walks `edges` (`INDEXED BY idx_edges_to`);
  caps, pagination, and ordering contract unchanged.

Measured on the S0-8 machine (i5-11300H, 8 GB, JDK 21.0.10), production
path (the bench's seed-42 dataset inserted through `SqliteTraceStore`,
queried through `TraceQueries.traceBack`, 100 reps over 20 deep targets):
1,000,068 events ingested in 161 s including edges (455 MB DB);
`trace_back` p50 10 ms, p95 19 ms, max 22 ms — T5 (≤2 s) met on the shipped
query with ~100× headroom. StorageBench re-run at 200k after the change:
slice parity derived-vs-edges `equal=true` on both engines, SQLite edges
p95 1 ms.

Corrections to the estimates above, found by measurement:
- Edges at ingest cost more than the bench's one-pass batch estimate
  (~3 s/1M): production ingest is now ~6,200 ev/s at 1M scale (161 s total
  vs 99 s events-only). Accepted: ingest is offline (collector); demo-scale
  runs (~14k events) are unaffected in practice.
- Two SQLite planner traps are load-bearing and recorded in code comments:
  edge derivation must force `idx_events_invocation` for the parent lookup
  (else O(n²) ingest — 122 s for one 10k chunk at 50k events), and
  `trace_back` must force `idx_edges_to` (a `(run_id, to_event)` index was
  used via its run_id prefix alone and scanned the run's edges per step;
  that index has been removed). An existence-probe `ensureEdges` matters
  for the same reason: a `COUNT(*)` variant cost ~130 ms/query at 1M.

## Status
Proposed — 2026-10-27; benchmark recorded and decision refined 2026-10-28
(S0-8); condition implemented 2026-11-01 (above). Owner acceptance to
Accepted pending.
