# Evaluation

Method: skill `eval-localization`. Gate: [spike-acceptance.md](spike-acceptance.md).
Principle: TRACE is only better if it beats a **fair** grep-only baseline on measured bugs.

## Baseline agent (grep-only) — exact spec

- Same model, same token budget, same investigation prompt as the TRACE agent, **minus** TRACE tools and any trace data.
- Allowed tools: `grep`/search, file read, directory list, plus the bug's **stack trace / failing test output** (that's what a real grep-based workflow starts from).
- No web, no build/test execution beyond reading the failing output. TODO(decide): may the baseline run the failing test? — question: allowing it is more realistic but costs time comparability.
- It must output the same report shape (root-cause location + explanation) so scoring is identical.

Fairness rules: same bug set, same starting information, same per-bug token/time caps, blind scoring against ground truth recorded **before** either agent runs. Any prompt change applies to both agents or is recorded as a variable.

## Bug sources

### (a) Demo-app seeded bugs
From [demo-app-spec.md](demo-app-spec.md) §Seeded bugs. Fast, deterministic, run on every eval.

### (b) Defects4J subset
- TODO(decide): subset (projects + bug IDs, ≥ enough to reach the ≥20-bug gate with fixtures) — question: which projects' build tooling works on our CI?
- Setup on Windows: Defects4J is a Perl framework. Verified requirements (web search, 2026-10-27):
  - Framework (current) runs on **Java 11**, needs **Perl ≥ 5.0.12**, Git ≥ 1.9, and Perl modules via `cpanm`; the *bugs/projects* compile with **Java 8** — sources: [Defects4J developer guide (DeepWiki mirror of rjust/defects4j)](https://deepwiki.com/rjust/defects4j/8-developer-guide), [rjust/defects4j](https://github.com/rjust/defects4j). TODO(verify): read the upstream README directly at setup time and pin the Defects4J version.
  - On Windows, run it inside **WSL2 (Ubuntu)** — native Windows Perl/Git-bash is TODO(verify) at best; do not budget time for it. Set `TZ=America/Los_Angeles` (framework generates/executes tests in that timezone, per README via mirrors above).
  - Java 8 JDK only for compiling/running corpus projects; our agent's support for instrumenting Java 8 targets is a separate TODO(decide) in architecture.md.
- `TZ=America/Los_Angeles`, pinned Defects4J commit, and the exact bug-ID list go in the fixture registry when chosen.

## Fixture format (one folder per bug)

```text
eval/fixtures/<id>/
  fixture.md      id, category, trigger, ground truth (below)
  trigger.*       command/script that reproduces the failure
  (Defects4J cases: pointer file with project, bug id, checkout command)
```

`fixture.md` must contain: `id`, `category` (taxonomy below), `source` (seeded|defects4j), failing test/trigger, **ground-truth root-cause location (class / method / line)**, **ground-truth explanation** (2–4 sentences), passing-run availability (for `diff_runs`). Ground truth is written before any agent runs the bug (skill `add-bug-fixture`).

## Bug taxonomy & target counts

**Proposed:** target per category below — rationale: ≥2 per category catches category-blindness without making the spike corpus the project.

| Category | Examples | Proposed target |
|---|---|---|
| Null propagation | missing null check, null from repository/map | 3 |
| Wrong conditional | inverted/ boundary condition | 3 |
| Off-by-one | loop bound, pagination, substring | 2 |
| Bad state mutation across calls | wrong field updated, shared mutable state | 3 |
| Exception swallowed | catch-and-ignore, lost cause chain | 2 |
| Concurrency / race | visibility, check-then-act, async handoff bug | 2 |
| Config / env | wrong property, profile-specific value | 2 |
| Serialization / mapping | field renamed/dropped, wrong DTO mapping | 3 |
| **Total** | | **20** |

TODO(decide): split of the 20 between seeded fixtures and Defects4J for the gate — question: how many Defects4J bugs can Phase 0 realistically check out and run?

## Scoring procedure

1. Run baseline and TRACE agent per bug, same caps. Record tokens, $, wall-clock, tool calls.
2. Score localisation (top-1/top-5 method, top-1 file) + explanation correctness (ground-truth match, human-scored blind where feasible — TODO(decide): scorer + rubric owner).
3. Record overhead + trace volume from the TRACE runs (spike-acceptance definitions).
4. Fill the results table; compare against T1–T5. No threshold edits after seeing results without an ADR.

## Results table template

| Bug | Cat | Baseline top-1/5 (method) | TRACE top-1/5 (method) | TRACE top-1 file | Tokens (base → TRACE) | $ TRACE | Time (base → TRACE) | Overhead | MB/min |
|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | |
