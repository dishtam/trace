# ADR-0010: Pluggable LLM provider for the product

## Context
TRACE's investigation agent calls an LLM through MCP tools (ADR-0002, ADR-0009). The docs first assumed Anthropic Claude models. The project is now built with Meta's Muse Code / Muse Wonder, and Muse Wonder is also callable as a product LLM through the Meta Model API. Which provider localizes bugs best per dollar is an empirical question the Phase 0 evaluation can answer. Options:
- Single provider (Claude only): simplest, but locks the result to one vendor and leaves the question untested.
- Single provider (Muse Wonder only): same lock-in, other vendor.
- Pluggable provider behind one interface: a little more code, and the evaluation can compare providers.

Note: the coding agent used to *build* TRACE (Muse Code) is independent of the LLM *inside* TRACE. This ADR is only about the latter.

## Decision
**The investigation and baseline agents talk to the LLM through one provider-neutral `LlmClient` interface** (in `mcp-server/`, reused by `eval/`). Initial providers: Anthropic (Claude) and Meta Model API (Muse Wonder). Provider + model are run configuration, recorded in every run's metadata.
- Tool definitions stay provider-neutral (plain JSON Schema; no vendor-only schema features).
- Evaluation runs per provider; the grep-only baseline uses **the same provider and model** as the TRACE run it is compared to (evaluation.md fairness).
- Redaction (ADR-0006) runs before any provider call, whichever provider is used.
- TODO(decide): default provider for v1, question — set from Phase 0 results (accuracy, cost per bug, latency) rather than up front.

## Consequences
- (+) Phase 0 can compare Claude and Muse Wonder on the same bugs; no vendor lock-in.
- (+) Provider pricing/model changes become config changes, not code changes.
- (−) Two clients to maintain; tool-calling behaviour differs per provider, so contract tests must run against each.
- (−) Prompt caching and other cost features differ per provider; budgets (llm-strategy.md) are tracked per provider.
- Revisit if one provider wins clearly on every T1–T5 metric across two evaluation runs.

## Status
Accepted — 2026-10-28 (owner, Step 0 grill, item g).
