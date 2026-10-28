---
name: eval-localization
description: Use when running the evaluation or baseline agent, computing localisation/overhead/cost metrics, or filling the results table. Do NOT use for a single ad-hoc debugging session (that is debug-investigation).
---

# Eval Localization

Spec: [docs/evaluation.md](../../../docs/evaluation.md). Gate:
[docs/spike-acceptance.md](../../../docs/spike-acceptance.md) (T1–T5).
Provider rules: [ADR-0010](../../../docs/decisions/ADR-0010-pluggable-llm-provider.md).

Corpora: seeded fixtures (fast — run on every relevant change) and Defects4J
(slow — run before any claim or release).

## Steps
1. Freeze the bug set, LLM provider + model, budgets, capture profile, and prompt before running. Record them with the results.
2. Run the grep-only baseline (evaluation.md spec: same provider/model/budget/prompt minus tools) and the TRACE agent per bug.
3. Score per bug: top-1/top-5 method, top-1 file vs pre-recorded ground truth; slice precision/recall for fixtures with a recorded slice; explanation correctness blind where feasible.
4. Compute overhead (p50/p95 wall time) + trace volume (spike-acceptance definitions) from the TRACE runs; tokens/$/time per bug for both agents.
5. Fill the results table in evaluation.md format — per-bug rows, then means.
6. Compare to T1–T5 and state GO/PIVOT/STOP implication. Never edit thresholds after seeing results without an ADR (`adr` skill).

## Checklist
- [ ] Same starting info, provider/model, token/time caps for both agents
- [ ] Ground truth predates the runs
- [ ] Per-bug rows published; averages not alone
- [ ] Overhead + MB/min reported alongside accuracy (accuracy-only = invalid win)
- [ ] Machine spec + capture profile recorded
- [ ] Unrun/failed bugs listed, not dropped silently
- [ ] No improvement reported that was not measured in this run; while the harness doesn't exist, report "harness not built yet", not ad-hoc numbers
