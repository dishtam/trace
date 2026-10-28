---
name: instrumentation-safety
description: MUST use for any change under agent/ or to value capture, snapshotting, capture filters, or in-process event writing. Do NOT use for collector/store/UI work that never runs inside the target JVM.
---

# Instrumentation Safety

References: [docs/security-privacy.md](../../../docs/security-privacy.md),
[docs/trace-schema.md](../../../docs/trace-schema.md) (Value section),
[docs/spike-acceptance.md](../../../docs/spike-acceptance.md) (T1/T2 budgets).
Full PR checklist: [PR-CHECKLIST.md](PR-CHECKLIST.md) in this folder — attach it to every agent PR.
If an item cannot be satisfied, stop and flag it — do not ship and note it later.

## Rules (all mandatory)
1. **Side-effect-free capture:** never call user code to render a value — no `toString()`, `equals()`, `hashCode()`, getters, iterators. Lazy proxies / lazy collections and anything that can trigger loading is `unsafe_to_render`: type + identity hash only.
2. **Truncation:** enforce the schema caps (chars, collection elements, depth). Every new captured field names its cap before it ships.
3. **Redaction on by default, in-agent:** name/value pattern rules from security-privacy.md run before the event leaves the process. Test proves the raw value never reaches storage.
4. **Reentrancy guard:** the agent must not trace itself (guard flag around capture + event writing; agent classes excluded from instrumentation).
5. **Allow-lists:** capture only allow-listed packages/classes. JDK/framework internals off by default; synthetic/bridge/lambda filtered.
6. **Classloader / JPMS:** new instrumentation must state its classloader behaviour and JPMS needs; test on the demo app (Spring proxies included).
7. **Fail-safe:** agent code never throws into the app, never blocks the app thread; ring-buffer overflow = drop + count, recorded.
8. **Overhead check:** run the testing-ci.md overhead benchmark; report the number. Past T1/T2 or unexplained regression = not mergeable.
9. **Perturbation:** app output identical with/without the agent on the `toString()`/timing/identity-sensitive fixtures.
10. **Dependencies:** no new deps in the agent jar without shading/relocation review.

## Checklist
Use [PR-CHECKLIST.md](PR-CHECKLIST.md) verbatim in the PR description.
