# ADR-0003: JDI layer — build our own, study existing MCP debuggers

## Context
Options: build our own JDI driver; extend debugmcp/mcp-debugger (has a Java/JDI adapter per feasibility plan); fork karellen-jdb-mcp. External projects carry license, maintenance, and security unknowns. TODO(verify): license + maintenance status of both projects — question: are they Apache-compatible and actively maintained at adoption time?

## Decision
**Build our own JDI driver** (`jdi-driver/`) for on-demand deep dives and `confirm_hypothesis`. Study mcp-debugger and karellen-jdb-mcp as design references only; copy no code until TODO(verify) above is answered and recorded here.

## Consequences
- (+) Full control of expression-evaluation sandboxing (see [security-privacy.md](../security-privacy.md)); no inherited RCE surface.
- (−) More Phase 3 work than extending an existing adapter.
- JDI is never the recorder (100–1000×-class stepping cost, feasibility plan).

## Status
Accepted — 2026-10-27 (owner, Phase 1 batch). Reference-study TODO remains open.
