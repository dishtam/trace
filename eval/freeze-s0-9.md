# S0-9 freeze record (frozen 2026-10-28, BEFORE any S0-9 live run)

Frozen per docs/evaluation.md + docs/spike-acceptance.md. Nothing here may
change after results are seen; a change requires a new run or an ADR.

- Bug set: B02, B03, B04, B05, B09, B10, B14, B16 (the S0-6 eight).
  Ground truth: eval/fixtures/<id>/fixture.md (all predate this freeze).
- Defects4J subset: NOT READY (no checkout; needs WSL + Java 8/11 toolchain
  per docs/evaluation.md). Shortfall recorded in docs/spike-acceptance.md.
- Providers/models (ADR-0010, one run set per provider):
  - meta: `muse-wonder-1.0` (Model API, pay-as-you-go Standard mode only)
  - anthropic: `claude-sonnet-5-5` (Messages API)
- Budget per investigation (owner decision at S0-7): **$0.50 and 50 tool
  calls**, enforced by BudgetTracker; prices from the verified table in
  eval Prices (Meta $4/$20, Anthropic $2/$10 per MTok, verified 2026-10-28).
- Capture profile: `allow=trace.demoapp`, summary level, ring 65536,
  agent -> event file -> collector -> SQLite (ADR-0014), fresh demo app per
  bug under only that bug's profile.
- Prompts: InvestigationRunner.SYSTEM_PROMPT (debug-investigation protocol;
  confirm/replay unavailable in Phase 0, so reports label root cause
  unconfirmed and end with `ROOT_CAUSE: <class>#<method>`), identical for
  both runners and both providers. Starting information per bug is the
  harness-recorded symptom (identical for baseline and TRACE; TRACE also
  receives the run_id). Baseline tools: search/read/list over
  demo-app/src/main only. TRACE tools: the four MCP tools (S0-5 contracts).
- Scoring (decided here, before results):
  - claimed location = the report's `ROOT_CAUSE:` line.
  - primary ground truth per bug parsed from fixture.md ("ground-truth root
    cause location", first `Class#method` cited; class-only where the
    fixture names no method).
  - method_correct: claimed class#method == primary class#method.
  - file_correct (reported as class-level hit): claimed class == primary class.
  - Every report row also carries tokens, USD, tool calls, wall time
    (accuracy-only results are invalid per spike-acceptance.md).
- Thresholds: T1-T5 exactly as in docs/spike-acceptance.md (unchanged).
- Machine: i5-11300H, 8 GB, Windows, JDK 21.0.10.

Run status at freeze time: BLOCKED — no provider API key is set on this
machine (MODEL_API_KEY / ANTHROPIC_API_KEY unset at process, user, and
machine scope). Live runs execute as soon as a key is provided:
`gradlew.bat :eval:run --args="--provider meta --runner both"` (and the same
with `--provider anthropic`), then scoring via `trace.eval.ScoreMain`.
