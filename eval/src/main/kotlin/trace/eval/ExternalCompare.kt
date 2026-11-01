package trace.eval

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import trace.store.SqliteTraceStore
import java.nio.file.Files
import java.nio.file.Path

/**
 * Side-by-side comparison on an EXTERNAL recording (produced by
 * `trace connect` from the connect module), reusing the exact gate
 * investigators: same provider/model, budget ($0.50 + 50 calls via
 * BudgetTracker), prompt, and starting symptom for both runners.
 * Baseline gets grep/read/list over the project's main sources; TRACE
 * gets the four MCP tools over the recorded run. Ground truth is the
 * operator's (e.g. a fix commit's class#method), recorded separately
 * before running — this class never sees it.
 *
 * Usage (from the trace repo):
 *   gradlew.bat :eval:run --args="--external --project C:/code/app --db C:/code/app/.trace/trace.db --symptom-file C:/code/app/.trace/symptom.txt --provider meta"
 * --run-id is optional (first run in the DB is used), --source-root
 * defaults to <project>/src/main/java, --out defaults to <project>/.trace/compare.
 */
object ExternalCompare {

    internal fun resolveRunId(db: Path, requested: String?): String {
        if (requested != null) return requested
        SqliteTraceStore(db.toString()).use { store ->
            val rows = store.jdbcQuery("SELECT run_id FROM runs ORDER BY started_at_ns LIMIT 1")
            require(rows.isNotEmpty()) { "no runs in $db" }
            return rows.single()["run_id"] as String
        }
    }

    fun run(args: Array<String>) {
        val tokens = args.filter { it != "--external" }.toTypedArray()
        val options = parseArgs(tokens)
        val project = Path.of(options["project"] ?: throw IllegalArgumentException("--project is required"))
            .toAbsolutePath().normalize()
        val db = Path.of(options["db"] ?: throw IllegalArgumentException("--db is required"))
        val symptomFile = Path.of(options["symptom-file"] ?: throw IllegalArgumentException("--symptom-file is required"))
        val sourceRoot = options["source-root"]?.let { Path.of(it) } ?: project.resolve("src/main/java")
        val outDir = options["out"]?.let { Path.of(it) } ?: project.resolve(".trace/compare")
        val runnerSelection = options["runner"] ?: "both"
        Files.createDirectories(outDir)
        val runId = resolveRunId(db, options["run-id"])
        val client: LlmClient = when (val provider = options["provider"] ?: "meta") {
            "meta" -> MetaClient.fromEnvironment()
            "anthropic" -> AnthropicClient.fromEnvironment()
            else -> throw IllegalArgumentException("unknown provider: $provider (meta|anthropic)")
        }
        val startingInfo = Files.readString(symptomFile)
        require(!startingInfo.contains("ground-truth", ignoreCase = true)) {
            "symptom file must not contain ground truth"
        }
        // External corpora burn the gate-tuned $0.50 cap quickly (large files,
        // large tool responses). These knobs apply identically to both
        // runners; the gate defaults ($0.50 / 50 calls / 40 turns) are the
        // defaults here too, so any change is explicit in the run output.
        val budgetCapUsd = options["budget-usd"]?.toDouble() ?: 0.50
        val maxToolCalls = options["max-tool-calls"]?.toInt() ?: 50
        val maxTurns = options["max-turns"]?.toInt() ?: 40
        println("provider=${client.provider} model=${client.model} project=$project run=$runId runner=$runnerSelection" +
            " budget_usd=$budgetCapUsd max_tool_calls=$maxToolCalls max_turns=$maxTurns")

        val summaries = mutableListOf<JsonObject>()
        if (runnerSelection in listOf("both", "baseline")) {
            val result = BaselineTools(sourceRoot).use { tools ->
                InvestigationRunner(client, tools, maxTurns, budgetCapUsd, maxToolCalls).investigate(startingInfo)
            }
            writeReport(outDir, "external", "baseline", client, result)
            summaries += summary("external", "baseline", client, result)
            println("[external] baseline done: ${result.budget.summary()} claimed=${result.claimedRootCause}")
        }
        if (runnerSelection in listOf("both", "trace")) {
            val traceInfo = startingInfo +
                "\nA recording of this exact failure exists as run_id=$runId. " +
                "Investigate it with your tools. Your first step is get_failure_point for that run."
            val result = TraceToolsMcp(db, listOf(sourceRoot), TraceToolsMcp.currentRuntimeClasspath()).use { tools ->
                InvestigationRunner(client, tools, maxTurns, budgetCapUsd, maxToolCalls).investigate(traceInfo)
            }
            writeReport(outDir, "external", "trace", client, result)
            summaries += summary("external", "trace", client, result)
            println("[external] trace done: ${result.budget.summary()} claimed=${result.claimedRootCause}")
        }
        Files.writeString(outDir.resolve("summary.json"),
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), JsonArray(summaries)))
        println("reports in $outDir")
    }
}

// writeReport/summaryJson live in EvalMain.kt (internal, shared).
