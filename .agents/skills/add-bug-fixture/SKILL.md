---
name: add-bug-fixture
description: Use when seeding a bug in demo-app or adding a Defects4J case to the evaluation set. Do NOT use for fixing bugs, or for adding a bug without recording ground truth first.
---

# Add a Bug Fixture

Format + taxonomy: [docs/evaluation.md](../../../docs/evaluation.md).
Bug catalogue: [docs/demo-app-spec.md](../../../docs/demo-app-spec.md).

A fixture is only useful if its ground truth is written down before anyone runs
the tool on it. Never tune the tool against a fixture and then record the tuned
answer as "expected".

## Steps
1. Pick one category + one defect. Confirm category target count isn't already full.
2. Write `eval/fixtures/<id>/fixture.md` **first**: id, category, source (seeded|defects4j), trigger, ground-truth root cause (class/method/line once seeded), ground-truth explanation, expected backward-slice statement set (hand-computed; second reviewer where possible), passing-run availability.
3. Seed the bug (keep the clean variant runnable for `diff_runs`) or record the Defects4J checkout pointer (project, bug id, command, pinned version).
4. Verify deterministically: clean passes, seeded fails with the recorded symptom, trigger is not flaky (run it **Proposed: 3×**). Flaky trigger → fix the trigger, not the tool.
5. Confirm `eval-localization` picks it up with no harness code changes.

## Checklist
- [ ] Ground truth written before any agent (TRACE or baseline) runs the bug
- [ ] One defect only; trigger deterministic
- [ ] Clean/passing variant available or absence stated
- [ ] Category + counts updated in evaluation tracking
- [ ] Defects4J: WSL/Java 8+11 setup notes from evaluation.md followed; TZ set

## Done when
Evaluation can run the fixture end-to-end and score it against the recorded
ground truth without human reinterpretation.
