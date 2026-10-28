# ADR-0002: Primary consumer — AI agent over MCP first

## Context
Two consumers compete for v1 scope: an LLM agent (MCP tools over the graph) and a human (IDE/web graph UI). Building both in parallel doubles surface before the core question (localisation accuracy) is answered.

## Decision
**AI agent over MCP is the primary v1 consumer.** Human UI is optional/later (architecture keeps an optional UI slot; no v1 exit criteria depend on it).

## Consequences
- (+) MCP tool contracts ([llm-strategy.md](../llm-strategy.md)) are the product API; they get contract tests first.
- (+) Evaluation can be fully automated against the baseline agent.
- (−) Human debugging experience in v1 is CLI + tool transcripts only. TODO(decide): minimal human view for Phase 1 — question: is a read-only trace browser needed for trust/debugging-the-debugger?

## Status
Accepted — 2026-10-27 (owner, Phase 1 batch).
