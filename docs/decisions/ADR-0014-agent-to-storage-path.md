# ADR-0014: Agent → storage path for the spike — file, then collector

## Context
Architecture.md places `collector/` in its own process writing to `store/`, with the agent emitting an event stream ("local socket / file" — ambiguous). Options for Week 1: (i) agent writes an event file, collector ingests it into SQLite; (ii) agent streams to collector over a local socket; (iii) agent writes SQLite in-process (rejected: JDBC + DB work inside the target JVM, violates the process boundary and the fail-safe rule).

## Decision
**Spike: agent appends schema-v0.1 events to a local event file; `collector/` reads that file and writes SQLite** (post-run for S0-3; tailing/incremental ingest may follow without schema change). Local-socket streaming is a Phase 1 upgrade behind the same event format.

## Consequences
- (+) No wire protocol, framing, or reconnect logic in Week 1; the event file is a debuggable artefact and doubles as the CI attachment.
- (+) Agent stays dependency-light (no JDBC, no socket client beyond file I/O).
- (−) No live streaming in the spike; "real-time" UI claims wait for Phase 1.
- File format (binary vs JSONL) is an S0-2/S0-3 implementation choice recorded in trace-schema.md if it affects the schema; it must preserve drop counts and run finalisation.

## Status
Accepted — 2026-10-28 (owner, Step 0 grill).
