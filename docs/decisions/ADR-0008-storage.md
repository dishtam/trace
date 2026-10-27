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

## Status
Proposed — 2026-10-27 (final pick pending Phase 0 benchmark).
