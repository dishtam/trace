# Architecture Decision Records

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](ADR-0001-target-environment.md) | Target environment v1: local dev + CI | Accepted |
| [ADR-0002](ADR-0002-primary-consumer.md) | Primary consumer: AI agent over MCP first | Accepted |
| [ADR-0003](ADR-0003-jdi-layer.md) | JDI layer: build our own, study mcp-debugger | Accepted |
| [ADR-0004](ADR-0004-licensing.md) | License: Apache-2.0 | Accepted |
| [ADR-0005](ADR-0005-build-and-language.md) | Build: Gradle Kotlin DSL; language: Kotlin | Accepted |
| [ADR-0006](ADR-0006-llm-data-policy.md) | LLM data: external API allowed, redaction default-on | Accepted |
| [ADR-0007](ADR-0007-hybrid-capture.md) | Hybrid capture: bytecode agent + JDI, not JVMTI-first | Accepted |
| [ADR-0008](ADR-0008-storage.md) | Storage: SQLite (benchmarked vs DuckDB, S0-8; edges condition) | Proposed |
| [ADR-0009](ADR-0009-llm-tools-not-raw-graph.md) | LLM sees MCP tools, never the raw graph | Accepted |
| [ADR-0010](ADR-0010-pluggable-llm-provider.md) | Pluggable LLM provider (Claude + Muse Wonder), compared in eval | Accepted |
| [ADR-0011](ADR-0011-jdk-baseline-and-targets.md) | JDK 21 tool baseline; targets 21 → CI 17/21/25; no Java 8 targets | Accepted |
| [ADR-0012](ADR-0012-gradle-build-structure.md) | Gradle 9.x wrapper + version catalog + build-logic | Accepted |
| [ADR-0013](ADR-0013-agent-shading.md) | Agent jar: Shadow, relocate Byte Buddy + ASM | Accepted |
| [ADR-0014](ADR-0014-agent-to-storage-path.md) | Spike path: agent → event file → collector → SQLite | Accepted |
| [ADR-0015](ADR-0015-demo-app-and-load-tool.md) | Demo: Spring Boot 4.1.x/JDK 21; load = Kotlin HTTP client | Accepted |
| [ADR-0016](ADR-0016-commit-convention.md) | Conventional Commits, one per roadmap task, ask first | Accepted |

Rules: one decision per file; format Context / Decision / Consequences / Status.
Superseded ADRs stay, marked `Superseded by ADR-XXXX`. Use skill `adr`.
Decisions recorded 2026-10-27 from owner answers to Phase 1 questions.
