# TRACE — AI-Powered Execution-Graph Debugger

> **Status:** Research & feasibility plan (no code yet) · **Date:** 2026-10-27 · **Version:** 0.1
> **Scope of this document:** Feasibility, architecture and tech-stack decision record. Implementation starts only after the Phase 0 spike gates in §11 pass.

---

## 1. Executive Summary — Verdict

**Feasible, with one hard constraint that shapes the whole product:** capturing *every* method call *and* every variable value for a large Java codebase at full fidelity is not viable as an always-on mode (10–100× slowdown, GB/min of trace data — see §7). It **is** viable, and genuinely differentiating, as a **scoped, on-demand execution recorder**:

1. Record a *bounded* execution (one failing test, one request, one user-triggered debug session) with method-level + selected-variable capture.
2. Build a **dynamic execution graph** (what *actually ran*, in order, with real values) — not a static guess.
3. Run **deterministic analysis first** (dynamic slicing, exception causal chain, value provenance) to isolate the failure, and use an **LLM second** — to explain, rank and propose fixes over that reduced slice, not to grep the whole repo.

| Question | Verdict |
|---|---|
| Can we build this for Java first? | **Yes.** Mature, documented capture paths exist (JPDA/JVMTI, bytecode agents, JFR). No research breakthrough required. |
| Can we capture "all variables in real time" for large apps? | **No — not literally.** Yes for a scoped session with filtering, depth caps and sampling (§5, §7). The product must be honest about this. |
| Is the execution graph a real advantage over grep/static graphs? | **Yes, for runtime bugs.** Static graphs show what *could* happen; the execution graph shows the one path that *did* happen, with the actual bad value and its provenance. |
| Can it generalise to all major languages later? | **Yes, if** the trace format + graph model are language-neutral from day 1 and each language is a separate "probe" (§10). Effort per language varies 3–10×. |
| Biggest risk | Trace-volume / overhead blow-up and graph UI becoming unreadable at scale — both mitigated by scoping, slicing-before-visualising, and collapsing (§7, §9). |
| Recommended first build | Java agent (Byte Buddy) + local trace store + dynamic-slice engine + CLI/web graph viewer + LLM explainer. No cloud, no Neo4j, no Kafka in the MVP (§8). |

**One-line positioning:** *Unlike AI tools that grep/static-graph a codebase to guess where a bug is, TRACE records what the program actually did — calls, values, exceptions — and traces the bad value back to its origin.*

---

## 2. Problem & Differentiation

### 2.1 The pain (as stated by the founder)

- In large codebases, today's AI debugging is essentially **enhanced grep**: the model searches text, guesses candidate locations, and iterates. It has no ground truth about what executed.
- Static/functionality graphs (e.g. "graphify"-style agents that map modules, calls and features from source) help navigation, but they are **static**: every branch, every possible caller, most of which never ran for the failing input.
- When a failure occurs, the developer's real questions are dynamic: *Which path ran? What value did `x` have here? Where did that value come from? What changed it last? Which call led into the failing frame?*

### 2.2 What TRACE adds

| Capability | Grep-based AI | Static graph agent | Classic debugger (IntelliJ) | **TRACE (this plan)** |
|---|---|---|---|---|
| Knows what actually executed | No | No | Yes, but only while stepping manually | **Yes — recorded, queryable after the run** |
| Variable values over time | No | No | Current frame only | **Recorded history per variable (scoped)** |
| "Where did this bad value come from?" | Guesses | Possible paths only | Manual step-back (limited) | **Automated backward dynamic slice** |
| Works after the process exited / in CI | Partial (logs) | Yes | No | **Yes — trace file is the artefact** |
| AI explanation grounded in facts | Weak (hallucination-prone) | Medium | N/A | **Strong — LLM sees the slice, not the repo** |
| Cross-request / multi-thread causality | No | No | Hard | **Phase 2+ (trace IDs + thread-aware graph)** |

### 2.3 Non-goals (MVP)

- Not a replacement IDE debugger (no interactive stepping in MVP — record first, analyse after; stepping can come later via DAP, §10).
- Not an APM/production-monitoring product (that's OpenTelemetry/JFR territory; TRACE is a *debugging* tool, production use is a later, sampled mode).
- Not "record everything forever." Scoped recording is a feature, not a limitation — it is what makes the data analysable.

---

## 3. Core Concept — The Execution Graph

### 3.1 Definition

An **execution graph** is a directed, time-ordered graph built from one concrete run:

- **Nodes**
  - `MethodInvocation` — one actual call (not the method definition): caller, callee, args, return value, start/end time, thread, duration, outcome (return/throw).
  - `ValueEvent` — a variable/field was assigned or read with a concrete value (serialised, truncated — see §6.3).
  - `ExceptionEvent` — thrown/caught exception, linked to its throwing frame and cause chain.
  - `Frame/Location` — source location (file, line) anchoring events to code the user recognises.
  - Later: `ThreadEvent`, `IOEvent` (DB/HTTP via existing instrumentation), `LockEvent`.
- **Edges**
  - `CALLS` (invocation → child invocation), `RETURNS_TO`
  - `DATA_FLOW` (value producer → value consumer) — the edge that answers "where did this come from?"
  - `CONTROL_FLOW` (condition outcome → branch taken)
  - `THROWS`, `CAUSED_BY`, `LAST_WRITER` (a read links to the most recent write of that variable)
  - `HAPPENS_ON_THREAD`, `SPAWNS_THREAD` (Phase 2)

Static source entities (class, method, file) exist as a **separate, linked layer** so each runtime node can jump to its source definition — this is where a static graph (call graph from bytecode/source) complements, not competes with, the execution graph.

### 3.2 The three questions the graph must answer in < 1s (MVP acceptance)

1. **Failure neighbourhood:** "Show me the call path and last N events before this exception."
2. **Value provenance:** "This variable was wrong at line L — which executed statements produced/changed it?" (backward dynamic slice)
3. **Divergence:** "Where does this failing run's path first differ from a passing run?" (Phase 2 — trace diff)

If the graph cannot answer Q1–Q2 fast on a 100k-event trace, the visualisation does not matter. **Slice first, visualise the slice** (typically tens–hundreds of nodes), never render the raw million-node graph.

---

## 4. Research Findings — How to Capture Execution in Java

Four capture mechanisms were evaluated. This is the most consequential technical decision in the project.

### 4.1 Option A — JPDA: JDI / JDWP / JVMTI (the "real debugger" path)

Java Platform Debugger Architecture = **JVMTI** (native VM interface) + **JDWP** (wire protocol) + **JDI** (pure-Java debugger-side API) [1][2].

- **Strengths:** Official, stable, exactly what IntelliJ/Eclipse use; full access to frames, locals, breakpoints, exception events; no bytecode rewriting.
- **Weaknesses for TRACE:** Designed for *interactive, stop-the-world* debugging. `MethodEntry/MethodExit`, watchpoint and single-step events over JDWP suspend/resume threads per event — orders of magnitude too slow for recording every call + value on a hot path. Remote value reads (`StackFrame.getValue`) are per-call round-trips.
- **Verdict:** Use for an optional **interactive/attach mode later** (and as the DAP backend, §10). **Not** the recording engine.

### 4.2 Option B — Bytecode instrumentation via a Java agent (Byte Buddy / ASM) ⭐ Recommended for MVP

A `-javaagent` uses `java.lang.instrument.ClassFileTransformer` at class-load time to rewrite bytecode, injecting probes at method entry/exit, line, field-write and variable-store points. **JaCoCo** proves this pattern at scale for coverage; APM vendors prove it for tracing [3][4][5].

- **Strengths:** In-process, no suspend/resume, events written to a lock-free ring buffer; fine-grained control of *what* to capture (packages, methods, variables); works with any JVM app, tests, or server; Byte Buddy gives a safe, high-level API over ASM with agent support [5].
- **Weaknesses:** Full-fidelity instrumentation is expensive (see §7); needs careful handling of constructors, lambdas, synthetic/bridge methods, classloaders, and JDK classes; value serialisation can perturb the program (calling `toString()` on arbitrary objects is a classic heisenbug source — snapshot primitives/String/enums + shallow field capture by default).
- **Verdict:** **Primary recording engine for the MVP.** Start at method entry/exit + exception + argument/return capture; add variable-store capture scoped to user-selected classes.

### 4.3 Option C — Java Flight Recorder (JFR) + JDK Mission Control

JFR is the JVM's built-in, production-grade event recorder: typically **<1–2% overhead**, 160+ event types (GC, allocation, locks, I/O), and newer JDKs add method-tracing capabilities [6][7].

- **Strengths:** Near-free always-on context (thread, GC, allocation, contention) that explains *environmental* factors; production-safe; `.jfr` files are a standard artefact.
- **Weaknesses:** Not designed to record arbitrary local-variable values or a complete call/value graph; you cannot ask JFR "what was `order.total` at line 42."
- **Verdict:** **Complementary, not primary.** Run JFR alongside the agent in later phases to correlate "the code did X" with "the JVM was doing Y." Do not build the core product on it.

### 4.4 Option D — Sampling profilers (async-profiler)

async-profiler samples stacks at ~100 Hz with **<1% overhead** (no safepoint bias) and produces flame graphs [8][9].

- **Strengths:** Best overhead/accuracy trade-off for *hot-path* questions; production-proven.
- **Weaknesses:** Statistical — it will miss the exact short-lived call and has no variable values. It answers "where is time spent," not "why is this value wrong."
- **Verdict:** Not the debugger core. Possible later add-on for "why is this loop slow" performance mode. Explicitly out of MVP scope.

### 4.5 Decision

> **Recording engine = Byte Buddy agent (Option B).** JDI/JVMTI reserved for a future interactive mode; JFR as optional correlation (Phase 3); sampling explicitly rejected as the core mechanism because it cannot provide values or exact provenance.

---

## 5. Feasibility Assessment

Rated per capability. "Feasible" means buildable with known techniques by a small team; effort/risk noted honestly.

| # | Capability | Feasibility | Key constraint / evidence |
|---|---|---|---|
| F1 | Record method calls (entry/exit, args, return, timing, thread) for a scoped run | ✅ **High** | Standard agent pattern (JaCoCo/APM precedent) [3][5]. Cost is volume, not correctness (§7). |
| F2 | Capture exception + full cause chain and link to throwing frame | ✅ **High** | Exception events are low-frequency and high-value; instrument throw sites + catch entry. Do this in MVP. |
| F3 | Capture *selected* variable values (method args, returns, field writes in chosen classes) | ✅ **High (scoped)** | Bytecode `STORE`/field-write probes. Needs value-snapshot policy (§6.3). |
| F4 | Capture *every* local variable on *every* line, whole program | ❌ **Not viable as default** | Event rate explodes (millions/sec on hot loops); serialisation perturbs behaviour; storage GB/min. Offer only as a narrow filter (single class/method). |
| F5 | Backward dynamic slice: "which executed statements produced this bad value?" | ✅ **High — core differentiator** | Classic technique: walk the recorded trace backward via data/control dependencies from a slicing criterion (location + variable) [10][11][12]. Deterministic; no LLM needed for the slice itself. |
| F6 | Real-time / live graph while program runs | ⚠️ **Medium** | Streaming events to UI is easy; *keeping up* at full fidelity is not. MVP: near-real-time (buffered, 100–500 ms batches) with live tail + pause. Claim "live," deliver "near-live," measure the lag. |
| F7 | Large codebase (1M+ LOC) support | ⚠️ **Medium** | Capture is per-*executed* code, so codebase size matters less than execution volume. Static index (for jump-to-source) must be incremental. Package/class filters are mandatory UX, not optional. |
| F8 | Multi-threaded programs | ⚠️ **Medium** | Per-thread ordering is exact; cross-thread causality needs happens-before edges (thread start/join, lock, executor handoff). Phase 1: correct per-thread graphs + thread lanes in UI. Phase 2: causal edges. Never pretend a single global order exists. |
| F9 | Distributed / microservices execution graph | ⚠️ **Later (Phase 3)** | Reuse OpenTelemetry trace/span IDs for cross-service correlation instead of reinventing propagation [13]. Single-process first. |
| F10 | LLM root-cause explanation + fix suggestion | ✅ **High, if grounded** | LLM receives the *slice + values + source snippets* (small, factual), not the repo. Ungrounded repo-wide LLM debugging is the competitor's weakness — do not replicate it [14]. |
| F11 | Multi-language (Python, JS/TS, Go, .NET…) | ✅ **Architecturally yes; per-language cost varies** | Each runtime has a probe path (§10): Python `sys.monitoring` (PEP 669, low-impact, 3.12+) [15], Node V8 Inspector/CDP [16], Go `runtime/trace` + delve, .NET CLR profiling API. Budget one language at a time; Java proves the model. |
| F12 | Production always-on recording | ❌ **Out of scope (by design)** | Full-fidelity capture in prod is an APM problem (sampling/JFR/OTel solve it at <2%). TRACE's production story, if any, is *triggered scoped capture* (Lightrun/Rookout prove demand but also show 3–7%+ overhead and throttling needs) [17][18]. |

### 5.1 What "real time" honestly means here

- **In-process capture:** truly real-time (events exist the nanosecond they happen).
- **Transport + storage:** batched (ring buffer → async writer). Budget: p95 event-to-queryable < 2 s during recording.
- **UI:** streamed tail (WebSocket/SSE) with explicit "you are N events behind" indicator. If the app produces events faster than the pipeline drains, the tool must **shed or sample with a visible warning** — silently dropping events would corrupt slices and destroy trust. Correctness > completeness claims.

### 5.2 Prior art — what to learn from, not rebuild

| Tool / family | What it proves | What TRACE should take / avoid |
|---|---|---|
| IntelliJ / Eclipse debuggers (JPDA) | Interactive debugging UX users expect | Take: source-anchored navigation. Avoid: stop-the-world as the recording model. |
| JaCoCo (ASM agent) | Bytecode agents are robust at scale in CI | Take: agent packaging, classloader handling lessons. |
| OpenTelemetry + Jaeger/Tempo | Standard trace model, sampling, cross-service propagation | Take: trace/span ID compatibility for Phase 3 [13]. Avoid: span-only granularity — TRACE needs sub-span (method/value) detail. |
| Lightrun / Rookout (live debugging) | Demand for non-breaking production probes; sandboxed instrumentation, throttling | Take: rate-limit/throttle design. Avoid: claiming "zero overhead" — measured 3–7%+ under load [17][18]. |
| Undo / Microsoft Time Travel Debugging (record & replay) | Full record/replay is powerful — and heavy (MB–tens of MB/sec traces) [19][20] | Take: "trace file as artefact" idea. Avoid: full system-call/memory recording in MVP; TRACE records semantic (method/value) events, which are far smaller and directly analysable. |
| async-profiler / JFR | Low-overhead JVM observability | Take: JFR correlation later. Avoid: mistaking sampling for debugging ground truth. |
| Dynamic slicing research (Korel & Laski; Java bytecode slicing) | Value-provenance debugging is a solved *algorithm*, unsolved *product* | Take: the algorithm as the core engine [10][11]. This is TRACE's moat — productise it. |

---

## 6. Proposed Architecture (Java MVP)

```
┌───────────────────────── Target JVM ─────────────────────────┐
│  User app / test                                             │
│  ┌──────────────────────────────────────────────────────┐    │
│  │ TRACE Java Agent (-javaagent, Byte Buddy)            │    │
│  │  • ClassFileTransformer: method/throw/store probes   │    │
│  │  • Capture filters (include/exclude pkg, class, var) │    │
│  │  • Value snapshotting (safe, bounded, no toString()) │    │
│  │  • Lock-free ring buffer → async event writer        │    │
│  └──────────────┬───────────────────────────────────────┘    │
└─────────────────┼────────────────────────────────────────────┘
                  │  binary event stream (local socket / file)
┌─────────────────▼───────────── TRACE Core (separate process) ─┐
│  Ingest → Trace Store (append-only event log + indexes)       │
│  Graph Builder (invocation tree, data-flow, last-writer)      │
│  Slice Engine (backward/forward dynamic slicing, diff)        │
│  Static Indexer (bytecode/source: class→method→location map)  │
│  Query API (local HTTP/gRPC): neighbourhood, slice, search    │
└──────┬──────────────────────────────┬─────────────────────────┘
       │                              │
┌──────▼──────────┐          ┌────────▼─────────────────────────┐
│ Web Graph UI    │          │ AI Layer                         │
│ slice viewer,   │          │ 1. Deterministic findings first  │
│ timeline, value │          │ 2. LLM explainer over the slice  │
│ inspector, diff │          │ 3. Fix suggestion + test idea    │
└─────────────────┘          └──────────────────────────────────┘
       ▲                              ▲
       └──────── CLI (`trace run/record/query`) — same API ──────┘
```

**Design principles**

1. **Agent stays dumb and fast.** It emits events; it never builds graphs, never calls an LLM, never blocks the app thread. All heavy work is out-of-process.
2. **Trace file is the product artefact.** A `.trace` recording can be saved, shared, attached to a bug report, replayed in the UI, and diffed — like a `.jfr` or a TTD trace, but semantic.
3. **Deterministic before probabilistic.** Slicing, exception chains and value history are exact computations. The LLM explains and hypothesises *on top of* them and must cite event IDs.
4. **Language-neutral core.** Agent emits a versioned event schema (§6.2); graph/slice/UI/AI never see Java-specific types. This is what makes §10 cheap later.
5. **Local-first.** MVP runs fully offline on a developer machine / CI. No account, no cloud upload of code or values (privacy, §9).

### 6.1 Components

| Component | Responsibility | MVP tech (see §8) |
|---|---|---|
| `trace-agent` (Java) | Bytecode probes, filtering, snapshotting, ring buffer, stream/file output | Java 17+, Byte Buddy, `java.lang.instrument` |
| `trace-core` | Ingest, storage, graph build, slicing, query API | Kotlin or Java (Spring Boot / Ktor) *or* Rust/Go — decision in §8.3 |
| `trace-cli` | `record`, `query`, `slice`, `export` for CI and scripting | Same binary as core (CLI mode) |
| `trace-ui` (web) | Execution-graph + timeline + value inspector | TypeScript, React, Cytoscape.js (canvas) |
| `trace-ai` | Slice summarisation, root-cause ranking, fix proposals | Provider-agnostic LLM adapter (API + local model option) |
| Static indexer | Map runtime locations ↔ source/bytecode; supply code snippets to UI/AI | ASM/JDT or `javap`-level bytecode index (source optional) |

### 6.2 Event schema (v0 — language-neutral)

Every event carries: `schemaVersion, traceId, eventId (monotonic), timestampNanos, threadId, language, sessionId`.

Core event types (extensible union):

- `SESSION_STARTED` (app, args hash, JDK, filters, git commit if available)
- `METHOD_ENTERED` (invocationId, parentInvocationId, class, method, signature, sourceRef, argSnapshots[])
- `METHOD_EXITED` (invocationId, returnSnapshot | thrownExceptionRef, durationNanos)
- `VALUE_ASSIGNED` (invocationId, variableRef (local/field/array-elem), snapshot, sourceRef)
- `EXCEPTION_THROWN` (exceptionId, type, message, throwingLocation, causeId?)
- `THREAD_STARTED / THREAD_ENDED`
- `SESSION_ENDED` (event counts, dropped-event count + reason — **must never be silent**)

Snapshots are typed values: `{kind: primitive|string|enum|null|object-ref|array, value?, typeName, truncated?, objectId?}`. `objectId` is an identity handle (not a heap dump) so the same object's journey can be followed without serialising it.

### 6.3 Value-snapshot policy (critical for correctness)

- **Default capture:** primitives, `String` (truncated, e.g. 512 chars), enums, boxed numbers, `null`. These are safe and cover most debugging questions.
- **Objects:** capture type + identity + (optionally) shallow primitive/String fields, depth ≤ 2, field count capped. **Never call user `toString()`/`equals()`/`hashCode()`** — they can throw, mutate, block, or be extremely slow. Use identity hash + reflective/`Unsafe`-free field reads with a strict budget.
- **Sensitive data:** redaction rules (field-name patterns: `password`, `token`, `secret`, …) applied **in the agent, before the event leaves the process**. Values must never reach disk unredacted (§9).
- **Large collections/arrays:** size + first K elements. Full contents only when the user explicitly scopes that variable.

### 6.4 Storage & graph model (MVP)

- **Append-only event log** on disk (the source of truth) + **derived indexes** built on ingest: invocation tree, per-variable write history (for last-writer), exception index, full-text search over values/messages.
- MVP store: **embedded, file-based** — e.g. RocksDB/SQLite-class store or a custom chunked columnar file + memory-mapped indexes. Deliberately **not** Neo4j/ClickHouse yet: the MVP query pattern is "walk this trace's tree/slice," not cross-trace graph analytics. Re-evaluate when multi-trace/cross-service queries arrive (Phase 3) — Neo4j or ClickHouse are candidates, neither is needed to prove the core value.
- Graph is **derived, not stored as the primary form**: rebuildable from the event log, sliceable on demand, exportable (JSON/GraphML/DOT) for interop.

---

## 7. Performance & Scale — The Numbers That Govern the Design

Instrumentation overhead is the project's existential constraint. Published ranges (indicative, workload-dependent — Phase 0 must measure on TRACE's own benchmarks, §12):

| Mode | Typical overhead | What you get | TRACE use |
|---|---|---|---|
| JFR (built-in events) | <1–2% [6][7] | JVM events, samples | Optional correlation |
| async-profiler sampling @100 Hz | <1% [8][9] | Statistical stacks | Not core (no values) |
| Method-level bytecode instrumentation | ~5–50% (hot methods dominate) | Exact calls, timing | MVP default, **filtered** |
| Full variable/line-level instrumentation | 10–100× on hot paths | Exact values everywhere | Only scoped to a class/method on demand |
| JDI method-entry events over JDWP | Orders of magnitude (suspend/resume) | Interactive debugging | Rejected as recorder |

**Trace-volume arithmetic (plan with this, not hope):** a modest service handling one request may execute 50k–500k method calls. At ~100–200 bytes/event compressed, one request ≈ 5–100 MB unfiltered. A tight loop iterating 1M times with per-iteration variable capture produces 1M+ events from *one line*. Consequences baked into the design:

1. **Filters are the product.** Include/exclude by package/class/method, max events/sec circuit-breaker, per-method event caps, and loop throttling (e.g. capture first N + every Kth + last N iterations) are MVP features, not settings-page extras.
2. **Adaptive capture (Phase 2 idea, design for it now):** start coarse (methods + exceptions); when an exception occurs, the *already-recorded* coarse trace localises the neighbourhood — variable detail is captured on **re-run with narrowed scope** ("focus mode": re-record just this path with full values). Two-pass debugging beats one impossible pass.
3. **Budgets as exit criteria:** MVP is shippable only if, on the Phase 0 benchmark suite, scoped recording keeps a covered workload ≤ 3× slowdown and ≤ 500 MB per recorded session by default (configurable, with visible warnings when exceeded).

---

## 8. Tech Stack Recommendation

### 8.1 Java MVP stack

| Layer | Recommendation | Why | Alternative / note |
|---|---|---|---|
| Agent language | **Java 17+** (bytecode target 8+ for the *target* app where possible) | Agent must load in the target JVM; widest app compatibility | Kotlin for agent internals is fine but adds runtime weight — prefer plain Java in-agent |
| Bytecode engine | **Byte Buddy** (+ ASM underneath) | Safe API, first-class agent support, active maintenance [5] | Raw ASM when Byte Buddy overhead/abstraction hurts; Javassist not recommended for new work |
| Agent transport | Local TCP socket or file, binary (length-prefixed, optionally compressed batches) | Zero deps in target process; file mode doubles as CI artefact | gRPC is overkill inside one machine; revisit for remote (Phase 3) |
| Core service | **Kotlin + Ktor** *or* **Java 21 + Spring Boot** — pick by team familiarity | JVM team reuses agent knowledge; excellent concurrency (virtual threads in Java 21) | Go/Rust core gives a single static binary — attractive for distribution; decide at Phase 0 exit (§8.3) |
| Event store (MVP) | **Embedded append-only log + SQLite/RocksDB indexes** | Offline, zero-ops, file is shareable | Neo4j/ClickHouse deferred to Phase 3 cross-trace analytics |
| Static index | ASM-based bytecode scan + optional source attach | Works without source (jars), better with it | Eclipse JDT if full Java source parsing is later needed |
| UI | **TypeScript + React + Cytoscape.js** (Canvas/WebGL) | Graph rendering at 10k+ nodes needs canvas, not SVG/DOM; Cytoscape has layout + filtering ecosystem | vis-network (simpler, weaker at scale); D3 (maximum control, maximum effort) |
| CLI | Picocli (JVM) or matching the core language | `trace record -- mvn test -Dtest=…`, `trace slice …` for CI | CLI is a first-class surface, not an afterthought — CI is a primary use case |
| LLM layer | Provider-agnostic adapter; structured JSON in/out; **local-model option** (e.g. Ollama-class) | Code/values are sensitive; users must be able to run fully local; provider pricing/models change fast | Never hard-wire one vendor's SDK into the core |
| Build / packaging | Gradle (agent + core), agent as a single shaded jar | `-javaagent:trace-agent.jar` must be one file with no classpath conflicts (shade/relocate deps) | Maven equally viable — follow team norm |
| Testing | JUnit 5 + Testcontainers where needed; dedicated **fixture apps** with known bugs (§12) | The product's correctness *is* its test suite: slices must match hand-computed ground truth | — |

### 8.2 What NOT to buy/build yet

Kafka, Kubernetes, Neo4j, a cloud backend, a VS Code extension, and multi-language probes are all **Phase 2–3**. Each is a distraction until one Java user can go from "failing test" to "here is the exact statement that produced the bad value" in under a minute.

### 8.3 Open stack decision (resolve at Phase 0 exit)

**Core language: JVM (Kotlin/Java) vs Go/Rust.** JVM = one language across agent+core, easier hiring from the Java-debugger world, heavier distribution. Go/Rust = single binary, lower memory for million-event processing, second language for the team. *Recommendation: start JVM for speed of iteration; keep the event schema language-neutral so the core can be replaced without touching agents or UI.*

---

## 9. Risks, Safety & Trust

| Risk | Impact | Mitigation (in plan, not aspirational) |
|---|---|---|
| Instrumentation changes program behaviour (heisenbugs) | Users lose trust; bugs vanish under the debugger | No user-code invocation during capture (§6.3); capture-budget circuit breakers; document perturbation risks; "observer effect" test fixtures in §12 |
| Secrets/PII recorded into trace files | Security incident; enterprise rejection | In-agent redaction before egress; redaction audit log; trace-file encryption at rest (Phase 2); explicit `--redact` patterns; local-first default |
| Trace explosion fills disk / OOMs the core | Tool crashes the thing it debugs | Hard caps (events, bytes, duration) with graceful stop + visible SessionEnded reason; streaming ingest never loads a whole trace into RAM |
| Graph UI unreadable at scale | Core value invisible | Slice-first UX (§3.2); collapse-by-package/method; timeline + list views alongside graph; graph is one view, not the only view |
| LLM hallucinates a root cause | Wrong fix, lost trust | LLM output must cite event IDs from the slice; deterministic findings displayed separately from hypotheses; confidence + "evidence" panel; never auto-apply fixes in MVP |
| Framework magic (Spring proxies, reflection, lambdas, Kotlin coroutines) | Missing/confusing graph edges | Fixture apps per framework (§12); document known blind spots per release; synthetic-method filtering with an "show internals" toggle |
| Classloader / JPMS / native-image edge cases | Agent fails to load on real apps | Compatibility matrix in CI (Spring Boot, plain Java, Kotlin, Scala smoke tests); GraalVM native-image explicitly out of scope until later |
| Scope creep to "all languages, production, IDE" at once | Nothing ships | Phased gates (§11); each phase has a kill/continue criterion |

---

## 10. Multi-Language Roadmap (after Java proves the model)

The language-neutral event schema (§6.2) + out-of-process core means each new language is **one new probe + a static indexer**, reusing storage, slicing, UI and AI.

| Order | Language | Probe path | Expected difficulty | Notes |
|---|---|---|---|---|
| 1 | **Java** (MVP) | Byte Buddy agent | — | Proves schema, slicing, UI |
| 2 | **Python** | `sys.monitoring` (PEP 669, CPython 3.12+) — designed for low-impact monitoring [15]; fallback `sys.settrace` (10×+ slower, legacy) | Medium | Dynamic typing makes value snapshots easy but type info weak; huge AI-tooling demand |
| 3 | **JS/TS (Node)** | V8 Inspector / Chrome DevTools Protocol [16]; or bytecode-level via instrumentation libs | Medium | Async causality (promises, event loop) is the hard part — thread model becomes "task" model |
| 4 | **Go** | `runtime/trace` + custom instrumentation / delve (DAP) | Medium-High | Compiled + goroutines; value capture needs DWARF-level work |
| 5 | **.NET (C#)** | CLR Profiling API / ReJIT; DAP via netcoredbg | Medium-High | Strong tooling culture (JetBrains/VS) = higher UX bar |
| — | Rust/C/C++ | LLVM instrumentation / rr-style recording | High | Defer; native record-replay (rr, Undo) already strong here [19] |

**IDE surface:** expose TRACE sessions through the **Debug Adapter Protocol (DAP)** [21] and/or a VS Code extension *after* the web UI validates the experience — DAP gives IntelliJ/VS Code/Eclipse reusability from one adapter instead of per-IDE plugins.

---

## 11. Phased Delivery Plan (gated — each phase must pass its exit criteria)

### Phase 0 — Feasibility spike (2–4 weeks) 🚩 *Start here*

- Byte Buddy agent prototype: method entry/exit + exception capture on 2 fixture apps (plain Java + one Spring Boot service).
- Measure: overhead vs uninstrumented, events/sec, bytes/session, behaviour-perturbation checks.
- Hand-build the backward-slice walk on one captured trace (script, not product) for one known bug.
- **Exit criteria:** ≤3× slowdown on scoped recording of the fixtures; slice for the known bug matches hand-computed ground truth; go/no-go on core language (§8.3).
- **Kill criterion:** if scoped method+value capture cannot stay ≤5× on realistic fixtures, pivot the design to JDI-triggered + re-run "focus mode" (§7.2) before writing product code.

### Phase 1 — Java MVP (6–10 weeks after Phase 0)

- Productised agent (filters, snapshot policy, redaction, caps), trace store + graph builder, backward slice engine, CLI (`record/slice/query/export`), web UI (slice view, timeline, value inspector), LLM explainer over slices.
- Fixture-bug benchmark suite (§12) green in CI.
- **Exit criteria:** a new user goes failing-test → root-cause slice in <5 min using only docs; the §3.2 questions answer in <1 s on a 100k-event trace.

### Phase 2 — Depth (next quarter)

- Forward slicing + trace diff (passing vs failing run), cross-thread causal edges, "focus mode" re-recording, IntelliJ/VS Code surface (DAP or plugin), trace-file encryption + sharing.
- **Exit criteria:** diff view pinpoints first divergence on the multi-thread/diff fixtures.

### Phase 3 — Breadth (following quarters)

- Python probe (reusing core/UI/AI), OpenTelemetry correlation (import OTel trace IDs; optional JFR sidecar), cross-service graph, multi-trace analytics store evaluation (Neo4j/ClickHouse) only if real queries demand it.
- **Exit criteria:** same failing-bug workflow demonstrated end-to-end in Python with no core changes.

### Explicitly deferred / rejected

- ❌ Full record-replay (rr/TTD-style) — heavy, and unnecessary for value-provenance questions.
- ❌ Always-on production full-fidelity recording — APM sampling solves a different problem.
- ❌ Auto-fix application — suggestions only, human applies, until trust is earned with data.

---

## 12. Validation Plan (how we'll know it's true, not just built)

1. **Fixture-bug corpus (the ground truth):** 15–20 small Java programs/services, each with *one known injected bug* (null propagation, off-by-one, wrong-cache-key, race, swallowed exception, bad merge of values…). For each: hand-written expected slice (statement set). **Metric: slice precision/recall vs ground truth — target ≥90% precision on the corpus before MVP ships.**
2. **Overhead benchmarks:** JMH-style harness + one realistic Spring Boot load test, instrumented vs not. Report p50/p95 overhead and events/sec for each capture profile (methods-only / +args / +scoped vars). Publish the numbers in-repo; do not market overhead claims not reproduced here.
3. **Perturbation tests:** fixtures whose behaviour depends on `toString()`, timing, and identity hashes — assert identical program output with and without the agent (within documented timing tolerance).
4. **Scale tests:** synthetic traces at 100k / 1M / 10M events; slice + neighbourhood queries must meet §3.2 latency budgets; UI must stay interactive (slice view, collapsed graph) at 1M events.
5. **Real-code trial:** run TRACE on one real open-source project's failing test (pick a known historical bug with a fix commit as ground truth). **Metric: does the slice contain the fix-commit's changed statements?**
6. **LLM evaluation (separate from deterministic metrics):** on the fixture corpus, score "explanation cites the true root-cause statement" with and without the slice provided. Hypothesis to falsify: slice-grounded ≫ repo-only. If grounding doesn't measurably help, simplify the AI layer.

---

## 13. Open Questions & Decisions Needed (founder input)

| # | Question | Recommended default | Needed by |
|---|---|---|---|
| Q1 | First user: solo devs (CLI/CI, open-source) or enterprise teams (IDE, compliance)? | **Solo/small-team, local-first, open-core** — fastest feedback, privacy story is simple | Phase 0 end |
| Q2 | Core language: JVM vs Go/Rust (§8.3)? | JVM to start | Phase 0 exit |
| Q3 | Licence / openness: open-source agent+core, or closed product? | Open agent + schema (adoption/trust), differentiation in AI/UI | Phase 1 start |
| Q4 | Which Java versions/frameworks are "supported" on day 1? | Java 17/21, plain + Spring Boot fixtures; publish a compat matrix, expand by demand | Phase 1 |
| Q5 | LLM: cloud API, local models, or both? | Both, provider-agnostic, local-first default for values-bearing prompts | Phase 1 |
| Q6 | Name/trademark: is "TRACE" clear to use? (generic word, crowded space) | Keep as working title; run a trademark/product-name check before any public launch | Before public release |

---

## 14. Sources

Research consulted for this plan (accessed 2026-10-27). Product/benchmark claims in §5/§7 should be re-verified on TRACE's own fixtures in Phase 0 (§12).

1. Oracle — Java Platform Debugger Architecture (JPDA): https://docs.oracle.com/en/java/javase/11/docs/specs/jpda/jpda.html
2. Oracle — Java Debug Wire Protocol (JDWP) spec: https://docs.oracle.com/en/java/javase/27/docs/specs/jdwp/jdwp-spec.html
3. JaCoCo — Implementation design (bytecode instrumentation agent): http://www.jacoco.org/jacoco/trunk/doc/implementation.html
4. Baeldung — An Intro to the Java Debug Interface (JDI): https://www.baeldung.com/java-debug-interface
5. Byte Buddy — runtime code generation & Java agents: https://bytebuddy.net/
6. Java Flight Recorder (JFR) — low-overhead JVM event recording (~<1–2%): https://www.javacodegeeks.com/2025/02/profiling-java-apps-with-flight-recorder-mission-control.html
7. JFR continuous profiling overhead discussion: https://copyprogramming.com/howto/profiling-a-running-java-application-in-command-line
8. async-profiler — CPU/wall-clock sampling overhead (<1% @100 Hz): https://www.aicassindra.com/blogs/java/java_async_profiler.html
9. Sampling vs instrumentation overhead comparison: https://oneuptime.com/blog/post/2026-01-30-low-overhead-profiling/view
10. Dynamic program slicing (foundations) — Purdue/CS notes: https://www.cs.purdue.edu/homes/xyzhang/spring07/Papers/ESE06.pdf
11. Dynamic slicing on Java bytecode traces (ACM TOPLAS): https://dl.acm.org/doi/10.1145/1330017.1330021
12. The Debugging Book — Tracking Failure Origins (slicing & dependencies): https://www.debuggingbook.org/html/Slicer.html
13. OpenTelemetry — Java instrumentation ecosystem: https://opentelemetry.io/docs/languages/java/instrumentation
14. Program Slicing in the Era of Large Language Models (arXiv): https://arxiv.org/html/2409.12369v1
15. PEP 669 — Low Impact Monitoring for CPython (`sys.monitoring`): https://peps.python.org/pep-0669/
16. Node.js — V8 Inspector / Debugger documentation: https://nodejs.org/download/nightly/v26.0.0-nightly20260120ba05a66774/docs/api/debugger.html
17. Lightrun — live debugging & sandboxed instrumentation: https://docs.lightrun.com/autonomous-debugger/overview/ (see also https://lightrun.com/blog/live-debugging-techniques/)
18. Production debuggers benchmark (Lightrun/Rookout overhead measurements): https://medium.com/@kaya/production-debuggers-2022-benchmark-results-part2-7f8e9411e882
19. Undo — Reverse/Time-Travel Debugging whitepaper: https://undo.io/resources/reverse-debugging-whitepaper
20. Microsoft — Time Travel Debugging (trace recording & replay): https://devblogs.microsoft.com/oldnewthing/?p=97645
21. Debug Adapter Protocol (DAP) — specification & overview: https://microsoft.github.io/debug-adapter-protocol/specification.html

---

## Appendix A — Worked Example (what success looks like)

> A Spring Boot checkout service intermittently throws `NullPointerException` at `CheckoutService.java:118` (`total.add(tax)` — `tax` is null) only for zero-rated products.
>
> 1. Developer runs: `trace record --include com.shop.checkout -- mvn test -Dtest=CheckoutIT#zeroRated`
> 2. TRACE records 84,213 events in 6 s (2.1× slowdown). UI opens on the exception node.
> 3. One click — **"Where did `tax` come from?"** — the slice engine walks back: `tax` was assigned at `TaxCalculator.java:57` from `rateRepository.find(rateId)` returning null at event #61,204, because `rateId` was `0` from `Product.java:33` for zero-rated items — 11 events total, shown as a highlighted path in the graph with real values at each step.
> 4. The LLM explainer receives *those 11 events + 3 source snippets* and responds: root cause (missing zero-rate row → null tax), evidence (event IDs), and a suggested guard + regression test.
>
> No grep. No guessing. The answer is a *fact about what ran*, explained in English.

## Appendix B — Glossary

- **Execution graph** — graph of what one concrete run actually did (§3). Distinct from a *call graph* (static, all possible calls).
- **Dynamic slice** — the set of executed statements that actually influenced a chosen variable at a chosen point, computed from a recorded trace [10][12].
- **Probe / agent** — per-language component that runs with the target program and emits events.
- **Snapshot** — a bounded, safe copy of a value at a moment in time (§6.3).
- **Focus mode** — re-recording a narrowed scope at higher fidelity after a coarse first pass (§7).




