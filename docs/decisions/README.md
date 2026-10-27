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
| [ADR-0008](ADR-0008-storage.md) | Storage for spike: TODO(decide) SQLite vs DuckDB benchmark | Proposed |
| [ADR-0009](ADR-0009-llm-tools-not-raw-graph.md) | LLM sees MCP tools, never the raw graph | Accepted |

Rules: one decision per file; format Context / Decision / Consequences / Status.
Superseded ADRs stay, marked `Superseded by ADR-XXXX`. Use skill `adr`.
Decisions recorded 2026-10-27 from owner answers to Phase 1 questions.
