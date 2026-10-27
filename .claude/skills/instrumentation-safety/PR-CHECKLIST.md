# PR Checklist — agent/ changes (attach to PR description)

- [ ] No user code invoked during capture (no `toString`/`equals`/`hashCode`/getters/iterators on captured values)
- [ ] Lazy proxies / lazy collections marked `unsafe_to_render` (type + identity only); proxy fixture test passes
- [ ] Every new/changed captured field has a stated size/depth/element cap, enforced + tested
- [ ] Redaction runs in-agent before egress; test proves raw secret values absent from stored trace
- [ ] Reentrancy guard intact: agent does not trace itself (guard test passes)
- [ ] Package/class allow-list respected; JDK/framework internals not captured by default
- [ ] Synthetic/bridge/lambda handling unchanged or explicitly tested
- [ ] Classloader + JPMS behaviour stated; demo-app (Spring proxy) smoke passes
- [ ] Agent never throws into / blocks the app thread; overflow drops + counts (test)
- [ ] Overhead benchmark run (docs/testing-ci.md); number reported here: ____ (budget: docs/spike-acceptance.md T1/T2)
- [ ] Perturbation fixtures pass: app output identical with/without agent
- [ ] Schema impact handled via `trace-event-schema` skill (or: none — stated)
