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

## Status
Proposed — 2026-10-27; benchmark recorded and decision refined 2026-10-28
(S0-8). Owner acceptance to Accepted pending (asked with the S0-8 commit).
