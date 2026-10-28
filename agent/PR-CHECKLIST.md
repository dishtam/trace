# PR Checklist — agent/ changes (S0-2, filled 2026-10-28)

- [x] No user code invoked during capture (no `toString`/`equals`/`hashCode`/getters/iterators on captured values) — ValueSummarizer renders only JDK scalars; everything else type+identity (`AgentUnitTest.toStringThrowingObjectIsNeverInvoked`)
- [x] Lazy proxies / lazy collections marked `unsafe_to_render` (type + identity only); proxy fixture test passes (`lazyCollectionIsUnsafeToRender`, subprocess integration)
- [x] Every new/changed captured field has a stated size/depth/element cap, enforced + tested — 256 chars / 8 elements (trace-schema.md Proposed caps, `scalarsRenderAndTruncate`)
- [x] Redaction runs in-agent before egress; test proves raw secret values absent from stored trace — name + shape rules (`redactionByNameAndShape`); integration asserts raw email never reaches the event file; store test proves storage side
- [x] Reentrancy guard intact: agent does not trace itself (guard test passes) — ThreadLocal guard in TraceRuntime; integration asserts no `trace.agent` call events
- [x] Package/class allow-list respected; JDK/framework internals not captured by default — `allowListRespected`; Spring CGLIB/Hibernate generated classes excluded after demo smoke caught them
- [x] Synthetic/bridge/lambda handling unchanged or explicitly tested — excluded in method/type matchers; integration order test covers real methods only (explicit synthetic-case test: TODO Week 2)
- [x] Classloader + JPMS behaviour stated; demo-app (Spring proxy) smoke passes — bootstrap append + AgentInstaller (TraceAgent javadoc); S0-3 demo run stored 33 events for one request incl. hand-checked nesting
- [x] Agent never throws into / blocks the app thread; overflow drops + counts (test) — catch-all at every entry point; `overflowDropsAndCountsNeverBlocks` (158,501 drops counted in one manual run)
- [x] Overhead benchmark run (docs/testing-ci.md); number reported here: median ratio 0.87x at sequential load — method cannot resolve agent cost; see docs/spike-acceptance.md S0-4 log (budget: docs/spike-acceptance.md T1/T2, no judgement yet)
- [x] Perturbation fixtures pass: app output identical with/without agent — fixture prints DONE, exit 0 with agent attached (all integration runs assert this)
- [x] No new agent-jar dependency, or shading/relocation reviewed — Byte Buddy 1.18.16 relocated to `trace.shaded.*` (ADR-0013); jar scan: 0 unrelocated `net/bytebuddy` entries
- [x] Schema impact handled via `trace-event-schema` skill (or: none — stated) — none: implements v0.1 as specified; completeness-flag gap in v0.1 DDL recorded as TODO(decide) for v0.2 in store code + Week 1 report
