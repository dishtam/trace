package trace.eval

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * S0-7 runners. Both investigators share provider, model, budget, prompt and
 * starting symptom; only their tools differ (evaluation.md fairness).
 *   baseline: grep/read/list over demo-app sources (BaselineTools)
 *   trace:    the four MCP tools over the recorded run (TraceToolsMcp)
 *
 * Usage (from repo root):
 *   gradlew.bat :eval:run --args="--provider meta --bugs B02,B03 --runner both"
 * Keys come from the environment only: MODEL_API_KEY (meta) / ANTHROPIC_API_KEY.
 * Reports land in eval/results/<bug>-<provider>-<runner>.md plus summary.json.
 */
fun main(args: Array<String>) {
    val options = parseArgs(args)
    val providerName = options["provider"] ?: "meta"
    val bugs = (options["bugs"] ?: "B02,B03,B04,B05,B09,B10,B14,B16").split(",").map { it.trim() }
    val runnerSelection = options["runner"] ?: "both"
    val repoRoot = Path.of(options["repo"] ?: ".").toAbsolutePath().normalize()
    val outDir = repoRoot.resolve(options["out"] ?: "eval/results")
    Files.createDirectories(outDir)

    val client: LlmClient = when (providerName) {
        "meta" -> MetaClient.fromEnvironment()
        "anthropic" -> AnthropicClient.fromEnvironment()
        else -> throw IllegalArgumentException("unknown provider: $providerName (meta|anthropic)")
    }
    println("provider=${client.provider} model=${client.model} bugs=$bugs runner=$runnerSelection")

    val harness = EvalHarness(repoRoot, outDir.resolve("work"))
    val summaries = mutableListOf<JsonObject>()

    for (bug in bugs) {
        val bugCase = harness.capture(bug)
        println("[$bug] captured run=${bugCase.runId}")
        val startingInfo = buildString {
            appendLine("Bug report ($bug, category: ${bugCase.category}).")
            appendLine("How it was triggered: ${bugCase.triggerText}")
            append(bugCase.symptom)
        }
        if (runnerSelection in listOf("both", "baseline")) {
            val result = BaselineTools(repoRoot.resolve("demo-app/src/main")).use { tools ->
                InvestigationRunner(client, tools).investigate(startingInfo)
            }
            writeReport(outDir, bug, "baseline", client, result)
            summaries += summary(bug, "baseline", client, result)
            println("[$bug] baseline done: ${result.budget.summary()} claimed=${result.claimedRootCause}")
        }
        if (runnerSelection in listOf("both", "trace")) {
            val traceInfo = startingInfo + "\nA recording of this exact failure exists as run_id=${bugCase.runId}. " +
                "Investigate it with your tools. Your first step is get_failure_point for that run."
            val db = bugCase.dbPath ?: throw IllegalStateException("no db for $bug")
            val result = TraceToolsMcp(db, listOf(repoRoot.resolve("demo-app/src/main/java")),
                TraceToolsMcp.currentRuntimeClasspath()).use { tools ->
                InvestigationRunner(client, tools).investigate(traceInfo)
            }
            writeReport(outDir, bug, "trace", client, result)
            summaries += summary(bug, "trace", client, result)
            println("[$bug] trace done: ${result.budget.summary()} claimed=${result.claimedRootCause}")
        }
    }
    Files.writeString(outDir.resolve("summary.json"),
        Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), JsonArray(summaries)))
    println("reports in $outDir")
}

private fun writeReport(outDir: Path, bug: String, runner: String, client: LlmClient, result: InvestigationResult) {
    val header = buildString {
        appendLine("---")
        appendLine("bug: $bug")
        appendLine("runner: $runner")
        appendLine("provider: ${client.provider}")
        appendLine("model: ${client.model}")
        appendLine("claimed_root_cause: ${result.claimedRootCause ?: "(none parsed)"}")
        appendLine("budget: ${result.budget.summary()}")
        appendLine("budget_exceeded: ${result.budgetExceeded}")
        appendLine("wall_ms: ${result.wallMillis}")
        appendLine("tool_calls_log:")
        result.toolCallsLog.forEach { appendLine("  - $it") }
        appendLine("---")
        appendLine()
    }
    Files.writeString(outDir.resolve("$bug-${client.provider}-$runner.md"), header + result.report + "\n")
}

private fun summary(bug: String, runner: String, client: LlmClient, result: InvestigationResult): JsonObject =
    buildJsonObject {
        put("bug", bug); put("runner", runner)
        put("provider", client.provider); put("model", client.model)
        put("claimed_root_cause", result.claimedRootCause?.let { JsonPrimitive(it) } ?: JsonNull)
        put("tokens_in", result.budget.usage.inputTokens)
        put("tokens_out", result.budget.usage.outputTokens)
        put("cost_usd", result.budget.costUsd)
        put("tool_calls", result.budget.toolCalls)
        put("wall_ms", result.wallMillis)
        put("budget_exceeded", result.budgetExceeded)
    }

private fun parseArgs(args: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        if (args[i].startsWith("--") && i + 1 < args.size) { map[args[i].removePrefix("--")] = args[i + 1]; i += 2 }
        else i++
    }
    return map
}
