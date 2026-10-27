---
name: add-bug-fixture
description: Use when seeding a known bug into the demo Spring Boot app or adding a case to the evaluation fixture set for TRACE localisation benchmarks.
---

# Add a Bug Fixture

A fixture is only useful if its ground truth is written down before anyone
runs the tool on it. Never tune the tool against a fixture and then record
the tuned answer as "expected".

## Steps

1. Pick **one** bug class (one defect per fixture): null propagation,
   off-by-one, wrong cache key, swallowed exception, bad value merge, race.
   TODO: fixture ID naming scheme — not decided; use `fixture-<bug-class>-<n>` until ratified.
2. Seed the bug in the demo Spring Boot app on a dedicated branch/commit so
   the clean version stays runnable for `diff_runs` (passing run = clean,
   failing run = seeded).
3. Write the failing test/input that triggers it deterministically. Flaky
   triggers are rejected — fix the trigger, not the tool.
4. Record the **expected root cause** in the fixture entry: throwing/failing
   location, the statement(s) that produced the bad value, and the expected
   backward-slice statement set (hand-computed, reviewed by a second person
   where possible).
5. Register the fixture in the evaluation set (TODO: registry file/format —
   not decided) with: id, bug class, trigger command, expected slice,
   expected root-cause statement.
6. Verify: clean version passes, seeded version fails with the recorded
   symptom, and `eval-localization` picks the fixture up without code changes.

## Done when

Evaluation can run the fixture end-to-end and score slice precision/recall
against the recorded ground truth without human reinterpretation.
