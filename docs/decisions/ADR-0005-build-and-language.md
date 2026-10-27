# ADR-0005: Build tool and language — Gradle Kotlin DSL + Kotlin

## Context
The tool is JVM-heavy (agent, JDI, MCP server). Alternatives considered: Maven + Java, Gradle + Java. The feasibility plan recommended Kotlin/Java 17+.

## Decision
**Gradle with Kotlin DSL** for the build; **Kotlin** as the primary language (targeting JDK 17+ for the tool itself — TODO(verify): exact tool JDK baseline, question: do we need to run on JDK 11 hosts?).

## Consequences
- (+) Concise agent/collector code; coroutines suit event pipelines (TODO(verify): structured-concurrency approach for the collector — Phase 0 task).
- Agent module must stay dependency-light and shaded regardless of language.
- Target *instrumented* apps are a separate matrix (see [architecture.md](../architecture.md): Proposed JDK 11/17/21/25 + Java 8 note for Defects4J).
- Build/test commands remain `TODO(decide)` in CLAUDE.md until the build exists.

## Status
Accepted — 2026-10-27 (owner, Phase 1 batch).
