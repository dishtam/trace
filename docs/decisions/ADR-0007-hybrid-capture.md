# ADR-0007: Hybrid capture — bytecode agent (always-on) + JDI (on-demand); not JVMTI-first

## Context
Three capture paths (feasibility plan): (A) JDI/JDWP — no code change, evaluates expressions, but a round-trip per event; stepping everything is orders of magnitude too slow. (B) Java agent + ByteBuddy/ASM — in-process, filterable, captures args/returns/locals/field writes; needs `-g` for local names; value-rendering can cause side effects. (C) JVMTI native agent — lowest-level control, but a native build per platform and the highest maintenance burden.

## Decision
**Hybrid B + A.** (B) runs the cheap always-on trace (method entry/exit, arg/return summaries, exceptions; ring buffer; dump on failure). (A) is invoked on demand to deepen a suspect region and to run `confirm_hypothesis` replays. (C) JVMTI is rejected as the starting point; revisit only if (B) measurably cannot capture something required (record the measurement, then supersede this ADR).

## Consequences
- (+) Recorder cost stays in the low-multiples range for scoped capture (Proposed budget in [spike-acceptance.md](../spike-acceptance.md)).
- (+) Iterative deepening by re-execution: coarse run → suspect region → detailed run. Weak for non-deterministic/concurrency bugs — stated, not hidden.
- (−) Two capture implementations to keep semantically identical against one schema ([trace-schema.md](../trace-schema.md)).
- Value capture must be side-effect-free (skill `instrumentation-safety`).

## Status
Accepted — 2026-10-27.
