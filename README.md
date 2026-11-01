# TRACE — AI execution-graph debugger

TRACE records an execution graph (calls, returns, exceptions) from a running
Java program. An LLM then traces back from the failure to the root cause
through capped MCP tools over that graph — never by grepping the codebase,
and never by seeing the raw graph.

- **Status:** Phase 0 spike. Dev + CI only (no production use). Apache-2.0.
- **Stack:** Kotlin + Gradle Kotlin DSL. Byte Buddy agent, SQLite store,
  MCP Kotlin SDK server.
- **Design docs win over this file:** start with
  [docs/architecture.md](docs/architecture.md),
  [docs/trace-schema.md](docs/trace-schema.md), and the
  [ADRs](docs/decisions/README.md).

## How it fits together

```text
target JVM ──(agent: -javaagent)──▶ events.jsonl ──(collector)──▶ trace.db (SQLite)
                                                                       │
                                          source roots ──▶ MCP server (stdio)
                                                                       │
                                              4 capped tools ──▶ LLM investigator
```

| Module | Role |
|---|---|
| `agent/` | Bytecode agent (`-javaagent`). Records call/return/throw events with safe, redacted value summaries into a ring buffer; never throws into the target app. |
| `collector/` | Ingests `events.jsonl` into the store and assigns run/event IDs. |
| `store/` + `graph/` | SQLite persistence, graph derivation, backward slices, source index. |
| `mcp-server/` | Stdio MCP server exposing the 4 investigation tools (below). |
| `connect/` | One-command capture from an **external** Java codebase (see [Connecting to an external codebase](#2-connect-to-an-external-codebase)). |
| `demo-app/` | Seeded-bug Spring Boot service used by the evaluation harness. |
| `eval/` | Baseline (grep) vs TRACE (MCP) investigator comparison harness. |
| `jdi-driver/` | On-demand JDI deep dive (stub in this build; no `confirm_hypothesis` yet). |

## Prerequisites

| Requirement | Detail |
|---|---|
| JDK 21 | To build and run the TRACE tools themselves (Gradle toolchain 21). Verified locally on JDK 21.0.10. |
| Target app JDK | 17, 21, or 25. Compile the target with debug info (`-g`, the Gradle/Maven default) so line numbers are recorded. |
| OS | Windows or Linux. Commands below show Windows (`gradlew.bat`, PowerShell quoting); on Linux/macOS use `./gradlew` and single quotes — the arguments are identical. |
| Git | Only to clone this repo. The target project needs no TRACE files added to it. |

No other installs: the Gradle wrapper is committed and downloads its own
distribution and dependencies from Maven Central on first run.

## Build once

From the repo root:

```powershell
.\gradlew.bat build :agent:shadowJar :mcp-server:installDist
```

This compiles every module, runs the tests, produces the shaded agent jar
at `agent/build/libs/agent.jar`, and installs the MCP server launcher under
`mcp-server/build/install/mcp-server/bin/`. Later captures only need this
rebuild when you change TRACE itself.

## 1. Try it on the bundled demo app

```powershell
# Run the seeded-bug Spring Boot service (http://localhost:8080):
.\gradlew.bat :demo-app:bootRun

# Full build + tests, architecture guard included:
.\gradlew.bat build
.\gradlew.bat checkArchitecture
```

To record and investigate demo failures end-to-end (both the grep baseline
and the TRACE investigator), use the eval harness — it needs a model API key
in the environment (`MODEL_API_KEY` for `meta`, `ANTHROPIC_API_KEY` for
`anthropic`):

```powershell
$env:MODEL_API_KEY = "<key>"
.\gradlew.bat :eval:run --args="--provider meta --bugs B02 --runner both"
# Reports land in eval/results/.
```

## 2. Connect to an external codebase

One command records a failing run from a Java project that lives **outside**
this repo, stores it, prints the failure point, and hands you the exact MCP
server command for the recording. The target's build files are never
modified. Full reference: [docs/external-codebases.md](docs/external-codebases.md).

```powershell
# Gradle or Maven test (build system auto-detected from the project dir):
.\gradlew.bat :connect:run --args="--project C:/code/myapp --test com.example.OrderTest"

# Runnable jar (extra app arguments go after --):
.\gradlew.bat :connect:run --args="--project C:/code/myapp --jar build/libs/app.jar -- --spring.profiles.active=prod"

# Any shell command (agent injected via JAVA_TOOL_OPTIONS — universal fallback):
.\gradlew.bat :connect:run --args="--project C:/code/myapp --command `"mvn -Dtest=OrderTest test`""
```

How the agent is attached, per mode:

| Mode | Injection | Notes |
|---|---|---|
| `--test` on Gradle | Generated init script adds `-javaagent` to every `Test` task | Target build files untouched; works with an existing daemon; projects without a wrapper borrow the TRACE repo's |
| `--test` on Maven | `JAVA_TOOL_OPTIONS=-javaagent:…` for the Maven process | Surefire forks inherit it; the project's own `argLine` is preserved |
| `--jar` | `-javaagent` prepended to the `java -jar` line | App arguments pass through after `--` |
| `--command` | `JAVA_TOOL_OPTIONS` for the shell command | Works for any JVM launcher |

The target's exit code never fails connect — a failing test is the expected
input. Connect fails only when nothing was captured (empty event file); the
first thing to check then is the allow-list.

### Connect options

| Option | Meaning |
|---|---|
| `--project <dir>` | **Required.** Root of the external codebase. |
| `--test <selector>` / `--jar <path>` / `--command <cmd>` | **Exactly one** run mode (see table above). |
| `--allow <pkg[,pkg…]>` | Package prefixes to instrument. When omitted, connect infers the dominant prefix (first two segments) from the project's `package` declarations and prints what it chose. This is the volume control — narrow it to the modules on the failing path in large projects. |
| `--out <dir>` | Artefact directory (default `<project>/.trace/`). |
| `--capacity <n>` | Agent ring-buffer size (default `262144`). |
| `--agent <path>` | Agent jar override. By default connect walks upward from the working directory for `agent/build/libs/agent.jar`. |
| `--serve` | After recording, serve the database on stdio itself. For real MCP clients prefer the `installDist` launcher below — Gradle's console is not a clean stdio channel. |

Artefacts land in `<project>/.trace/`:

```text
<project>/.trace/
  events.jsonl                  raw agent event stream (schema v0.1)
  trace.db                      SQLite store — the file the MCP server opens
  trace-connect.init.gradle     generated Gradle init script (test mode only)
```

Connect prints the run id, ingested event count, drop count, the failure
point it found, and the ready-to-paste MCP server command.

## 3. Investigate the recording with an LLM

### Start the MCP server

Use the launcher printed by connect (paths will differ on your machine):

```powershell
.\mcp-server\build\install\mcp-server\bin\mcp-server.bat C:\code\myapp\.trace\trace.db C:\code\myapp\src\main\java
```

Usage: `mcp-server <trace.db> [sourceRoot …]`. Source roots may also come
from the `TRACE_SOURCE_ROOTS` environment variable (path-separated). They
let `get_source` show real code from the external project; connect
auto-detects `src/main/java` and `src/main/kotlin` roots for you.

### Register it in your MCP client

Register that command as a stdio MCP server in your client and restart the
client session. Generic shape (adapt the key/format to your client):

```json
{
  "mcpServers": {
    "trace": {
      "command": "C:\\path\\to\\trace\\mcp-server\\build\\install\\mcp-server\\bin\\mcp-server.bat",
      "args": ["C:\\code\\myapp\\.trace\\trace.db", "C:\\code\\myapp\\src\\main\\java"]
    }
  }
}
```

Then ask the client to investigate the run id printed by connect.

### The four tools (the only view the LLM gets)

| Tool | Input | Returns (capped) |
|---|---|---|
| `get_failure_point` | `run_id` | The uncaught exception, its location, and up to 20 call frames with citable event/invocation IDs. |
| `trace_back` | `run_id`, `target_id` (event ID), `max_depth` 1–50 (default 20), `cursor` | Backward slice from an event, 100 per page. |
| `get_call_subtree` | `run_id`, `invocation_id`, `depth` 0–10 (default 3), `cursor` | Invocation subtree, 200 nodes per page. |
| `get_source` | `class_name` (+ optional `method_name`) or `symbol_id` | Source snippet, max 120 lines, from the configured source roots. |

Errors are structured `{code, message, hint}` (e.g. `RUN_NOT_FOUND`,
`FAILURE_NOT_FOUND`, `SOURCE_NOT_FOUND`) — never a raw graph dump.

### Investigation protocol

1. `get_failure_point(run_id)` → exception and throwing location.
2. `trace_back` from that event → the events that produced it.
3. `get_call_subtree` / `get_source` for the implicated statements.
4. Form **one** hypothesis citing event IDs.

> **No root-cause claim without verification.** This build has no
> `confirm_hypothesis` or `diff_runs`, so every root cause must be labelled
> **unconfirmed**. See the `debug-investigation` skill in
> [.agents/skills/](.agents/skills/).

## Manual pipeline (advanced, no `connect`)

The same pieces can be run by hand — this is what `connect` automates:

```powershell
# 1. Attach the agent directly (allow is required; out defaults to trace-events.jsonl):
java "-javaagent:agent/build/libs/agent.jar=allow=com.example;out=C:/code/myapp/.trace/events.jsonl;capacity=262144" -jar C:/code/myapp/build/libs/app.jar

# 2. Ingest the event file into SQLite:
.\gradlew.bat :collector:run --args="C:/code/myapp/.trace/events.jsonl C:/code/myapp/.trace/trace.db myapp"

# 3. Serve the database over MCP (as in step 3 above):
.\mcp-server\build\install\mcp-server\bin\mcp-server.bat C:\code\myapp\.trace\trace.db C:\code\myapp\src\main\java
```

Agent arguments are `;`-separated `key=value`: `allow` (required,
comma-separated package prefixes), `out`, `capacity` (ring size, rounded up
to a power of two), `trigger` (file whose appearance requests a dump), and
`redact` (default `true` — keep it on; redaction happens in-agent).

## Optional: side-by-side evaluation on an external recording

Compare the grep baseline against the TRACE investigator on the same
external failure (same model, symptom, and budget for both). Needs a model
API key in the environment, as in §1:

```powershell
.\gradlew.bat :eval:run --args="--external --project C:/code/myapp --db C:/code/myapp/.trace/trace.db --symptom-file C:/code/myapp/.trace/symptom.txt --provider meta"
# Reports land in <project>/.trace/compare/ (override with --out).
# --run-id, --source-root, --runner, --budget-usd, --max-tool-calls, --max-turns are optional.
```

The symptom file is the failing-test output both investigators start from;
it must not contain the ground truth (the harness rejects it if it does).

## Limits (measured, not aspirational)

- Java/JVM bytecode targets on JDK 17/21/25, compiled with debug info.
- Capture is call/return/throw with argument/return summaries. Local
  assignments and field writes are **not** recorded, so slices walk calls
  and exceptions, not value provenance.
- Non-exception failures (races, silent wrong values) have no failure
  point — start those investigations from `get_call_subtree`.
- Multi-module Gradle builds: the init script instruments every `Test`
  task in every module; `--test` selects the test. Event volume follows
  the allow-list, so start narrow (~15 KB stored per instrumented
  request-equivalent).
- Dev + CI only in v1; the LLM never sees the raw graph, only the four
  capped tools above.

## Project layout

```text
agent/       bytecode agent (ByteBuddy), capture, redaction, ring buffer
collector/   event ingest, run/session management
store/       SQLite persistence, indexes
graph/       graph derivation, slice queries, static source index
jdi-driver/  JDI deep-dive (stub in this build)
mcp-server/  the 4 MCP tools over graph/ (stdio)
connect/     external-codebase capture (this README, §2)
eval/        evaluation harness, baseline + TRACE investigators, fixtures
demo-app/    seeded-bug demo service (20 bugs, eval fixture)
docs/        architecture, schema, ADRs, evaluation, roadmap
```

Modules may only depend on modules listed before them — no cycles
(enforced by `gradlew.bat checkArchitecture`).

## Tests

```powershell
.\gradlew.bat build                          # full build + all tests + architecture guard
.\gradlew.bat :agent:test                    # agent unit + subprocess integration tests
.\gradlew.bat :store:test :collector:test
.\gradlew.bat :connect:test :mcp-server:test
.\gradlew.bat :demo-app:test                 # Spring Boot request-path test
```

## Documentation

| Doc | What it covers |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Components, data flow, repo layout |
| [docs/trace-schema.md](docs/trace-schema.md) | Event schema v0.1, values, DDL, slicing |
| [docs/external-codebases.md](docs/external-codebases.md) | `connect` reference (modes, options, limits) |
| [docs/decisions/](docs/decisions/README.md) | ADR-0001…0016 — every settled design choice |
| [docs/spike-acceptance.md](docs/spike-acceptance.md) | Phase 0 GO/PIVOT/STOP gate |
| [docs/evaluation.md](docs/evaluation.md) | Baseline agent, Defects4J, fixtures, scoring |
| [docs/demo-app-spec.md](docs/demo-app-spec.md) | Seeded-bug demo service spec (20 bugs) |
| [docs/security-privacy.md](docs/security-privacy.md) | Threat model, redaction, sandboxing |
| [docs/llm-strategy.md](docs/llm-strategy.md) | Models, budgets, MCP tool contracts |
| [docs/testing-ci.md](docs/testing-ci.md) | Test pyramid, overhead gate, CI matrix |
| [docs/roadmap.md](docs/roadmap.md) | Phase 0 tasks → Phase 4 |

## Contributing notes

- Conventional Commits, one commit per roadmap task (ADR-0016). Ask before
  committing or pushing.
- Any change under `agent/` must pass the `instrumentation-safety`
  checklist ([.agents/skills/](.agents/skills/)): never stringify lazy
  proxies or unsafe values; redaction stays on by default, in-agent.
- Unknowns are recorded as `TODO(decide)` / `TODO(verify)` with the
  question — never invented versions, prices, or caps.

## License

Apache-2.0.
