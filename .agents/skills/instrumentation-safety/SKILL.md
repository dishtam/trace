---
name: instrumentation-safety
description: Use when changing the Java agent, bytecode instrumentation, value capture/snapshotting, capture filters, or anything running inside the target JVM.
---

# Instrumentation Safety

Check every item before merging an instrumentation change. If an item cannot
be satisfied, stop and flag it — do not ship and note it later.

## Checklist

- [ ] **Side-effect safe capture:** never call user code to render a value —
  no `toString()`, `equals()`, `hashCode()`, getters, or iterators on captured
  objects (lazy proxies can trigger DB loads; `toString()` can throw/mutate).
  Capture primitives, `String`, enums, boxed numbers as-is; objects as
  type + identity + shallow fields only.
- [ ] **Size limits:** strings truncated (TODO: cap, default proposal 512
  chars — not decided), collections/arrays as size + first K elements,
  object depth ≤ 2, field count capped. Every new captured field names its cap.
- [ ] **Redaction:** field/variable names matching `password|token|secret|key|auth|credential`
  (TODO: final pattern list — not decided) are redacted **in the agent**,
  before the event leaves the process. Add a test proving the raw value is
  absent from storage.
- [ ] **Allow-list:** capture only inside configured package/class allow-lists.
  JDK/framework internals excluded by default; synthetic/bridge/lambda
  methods filtered (with an opt-in "show internals" flag).
- [ ] **Overhead budget:** change is benchmarked on the fixture suite
  (`eval-localization`). Scoped recording must stay within the Phase 0 budget
  (TODO: ratify ≤3× slowdown / ≤500 MB per session from plan §7). Report the
  number in the PR; no number, no merge.
- [ ] **Never block or fail the app:** agent code is non-throwing, bounded-time,
  and writes to a lock-free ring buffer. On overflow: drop + count, never
  block the app thread, never throw into user code.
- [ ] **Perturbation test:** app output is identical with/without the agent on
  the `toString()`/timing/identity-sensitive fixtures.
- [ ] **No new deps in the agent jar** without shading/relocation review.
