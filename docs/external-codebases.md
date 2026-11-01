# Connecting TRACE to an external codebase

One command records a failing run from a Java project that is not part of
this repo, stores it, and hands you a ready MCP server configuration for
investigation. Implemented in the `connect` module
(`trace.connect.ConnectMain`); capture uses the existing agent, collector,
store/graph, and MCP server unchanged.

## Quick start

```powershell
# Build the pieces once (from the trace repo):
.\gradlew.bat build :agent:shadowJar :mcp-server:installDist

# Record a failing Gradle/Maven test:
.\gradlew.bat :connect:run --args="--project C:/code/myapp --test com.example.OrderTest"

# Record a runnable jar:
.\gradlew.bat :connect:run --args="--project C:/code/myapp --jar build/libs/app.jar"

# Record any shell command (agent injected via JAVA_TOOL_OPTIONS):
.\gradlew.bat :connect:run --args="--project C:/code/myapp --command \"mvn -Dtest=OrderTest test\""
```

Artefacts land in `<project>/.trace/` (`events.jsonl`, `trace.db`, plus the
generated Gradle init script in test mode). Override with `--out <dir>`.
Connect prints the run id, the ingested event count, the failure point it
found, and the exact MCP server command for the recorded database. With
`--serve` it then serves that database on stdio itself (use the
installDist launcher for real MCP clients; Gradle's console is not a clean
stdio channel).

## How capture is attached, per mode

| Mode | Injection | Notes |
|---|---|---|
| `--test` on Gradle | Generated init script adds `-javaagent` to every `Test` task | Target build files untouched; works with an existing daemon; projects without a wrapper borrow the trace repo's |
| `--test` on Maven | `JAVA_TOOL_OPTIONS=-javaagent:...` for the Maven process | Surefire forks inherit it; the project's own `argLine` is preserved |
| `--jar` | `-javaagent` prepended to the `java -jar` line | Extra app arguments pass through after `--` |
| `--command` | `JAVA_TOOL_OPTIONS` for the shell command | Universal fallback |

The target's exit code never fails connect — a failing test is the expected
input. Connect fails only when nothing was captured (empty event file),
with the allow-list as the first thing to check.

## Options

- `--allow <pkg[,pkg...]>` — package prefixes to instrument. When omitted,
  connect infers the dominant package prefix (first two segments) from the
  project's `package` declarations and prints what it chose. Narrow this to
  the modules on the failing path in large projects; it is the volume
  control (~15 KB stored per instrumented request-equivalent).
- `--capacity <n>` — agent ring size (default 262144).
- `--agent <path>` — agent jar override. By default connect walks upward
  from the working directory for `agent/build/libs/agent.jar`.
- `--out <dir>` — artefact directory (default `<project>/.trace`).

## Investigating the recording

Register the printed MCP server command in your client and investigate the
printed run id. Protocol (skill `debug-investigation`): `get_failure_point`
→ `trace_back` → `get_call_subtree` / `get_source` → one hypothesis citing
event ids, labelled **unconfirmed** (this build has no `confirm_hypothesis`
or `diff_runs`). Source roots are auto-detected (`src/main/java`,
`src/main/kotlin`) and passed to the server so `get_source` shows real code.

For a fair grep comparison: same model, same starting symptom (failing
test output), same budget, same report shape — minus the MCP tools.

## Limits (measured, not aspirational)

- Java/JVM bytecode targets on JDK 17/21/25; compile the target with debug
  info (`-g`, the Gradle/Maven default) for line numbers.
- Capture is call/return/throw with argument/return summaries. Local
  assignments and field writes are not recorded, so slices walk calls and
  exceptions, not value provenance; non-exception failures (races, silent
  wrong values) have no failure point and must start from
  `get_call_subtree`.
- Multi-module Gradle builds: the init script instruments every `Test`
  task in every module; `--tests` selects the test. Event volume follows
  the allow-list, so start narrow.
- The demo-app evaluation harness (`eval/`) remains demo-specific; for
  external corpora, capture is this command and scoring is against
  pre-recorded ground truth (e.g. the fix commit's class#method).

Verified 2026-11-01 against a scratch external Gradle project
(JUnit failure in `com.acme.billing`): connect captured the run, ingested
9 events, and reported the failure at
`com.acme.billing.TaxLookup#taxCents` — the seeded defect location.
