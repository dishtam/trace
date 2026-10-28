package trace.eval

import trace.collector.TraceIngest
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path

/** What both runners start from: identical symptom evidence (evaluation.md fairness). */
data class BugCase(
    val id: String,
    val category: String,
    val triggerText: String,
    val symptom: String,
    val runId: String?,
    val dbPath: Path?,
)

/**
 * Reproduces one seeded bug under the agent and stores the run. The demo app is
 * started fresh per bug with only that bug's profile (fixtures are isolated by
 * construction). Baseline and TRACE runners receive the same symptom text;
 * TRACE additionally receives the run_id.
 */
class EvalHarness(
    private val repoRoot: Path,
    private val workDir: Path,
    private var port: Int = 18311,
) {
    private val http = HttpClient.newHttpClient()

    fun capture(bugId: String): BugCase {
        val fixture = FixtureDoc.load(repoRoot.resolve("eval/fixtures/$bugId/fixture.md"))
        val demoJar = repoRoot.resolve("demo-app/build/libs/demo-app.jar")
        val agentJar = repoRoot.resolve("agent/build/libs/agent.jar")
        require(Files.isRegularFile(demoJar)) { "demo jar missing; run: gradlew.bat :demo-app:bootJar" }
        require(Files.isRegularFile(agentJar)) { "agent jar missing; run: gradlew.bat :agent:shadowJar" }
        Files.createDirectories(workDir)
        val events = workDir.resolve("$bugId-events.jsonl")
        val triggerFile = workDir.resolve("$bugId.trigger")
        val appLog = workDir.resolve("$bugId-app.log")
        Files.deleteIfExists(events); Files.deleteIfExists(triggerFile)
        val myPort = port++
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val process = ProcessBuilder(
            java,
            "-javaagent:$agentJar=allow=trace.demoapp;out=$events;trigger=$triggerFile",
            "-jar", demoJar.toString(),
            "--server.port=$myPort", "--spring.profiles.active=${bugId.lowercase()}",
        ).redirectErrorStream(true).redirectOutput(appLog.toFile()).start()
        try {
            waitForHealth(myPort)
            val observed = runTrigger(bugId, myPort)
            val stack = extractStack(appLog)
            val symptom = buildString {
                appendLine("Observed behaviour (recorded just now, identical for every investigator):")
                observed.forEach { appendLine(it) }
                if (stack.isNotBlank()) appendLine("Failing output (application log):\n$stack")
            }
            Files.createFile(triggerFile) // ask the agent to dump (dump-end) before we stop the app
            Thread.sleep(1500)
            val db = workDir.resolve("$bugId.db")
            Files.deleteIfExists(db)
            val summary = TraceIngest.ingest(events.toFile(), db.toFile(), app = "demo-app")
            return BugCase(bugId, fixture.category, fixture.triggerText, symptom, summary.runId, db)
        } finally {
            process.destroyForcibly()
        }
    }

    private fun runTrigger(bugId: String, port: Int): List<String> {
        val base = "http://localhost:$port"
        fun req(method: String, path: String, body: String? = null): Pair<Int, String> {
            val builder = HttpRequest.newBuilder(URI.create(base + path))
            if (body != null) builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
            else builder.method(method, HttpRequest.BodyPublishers.noBody())
            val response = try {
                http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                return 0 to "request failed: ${e.message}"
            }
            return response.statusCode() to response.body().take(300)
        }
        fun line(method: String, path: String, result: Pair<Int, String>) =
            "$method $path -> HTTP ${result.first} ${result.second.replace("\n", " ")}"
        return when (bugId) {
            "B02" -> listOf(line("POST", "/orders", req("POST", "/orders", """{"sku":"SKU-2","quantity":1}""")))
            "B03" -> listOf(line("GET", "/pricing?sku=SKU-1&quantity=5", req("GET", "/pricing?sku=SKU-1&quantity=5")))
            "B04" -> {
                val stock = req("GET", "/stock/SKU-2")
                val qty = Regex(""""available":(\d+)""").find(stock.second)?.groupValues?.get(1) ?: "0"
                listOf(line("GET", "/stock/SKU-2", stock),
                    line("POST", "/orders (quantity=$qty, exactly the stock)", req("POST", "/orders", """{"sku":"SKU-2","quantity":$qty}""")))
            }
            "B05" -> listOf(
                line("POST", "/orders", req("POST", "/orders", """{"sku":"SKU-1","quantity":1}""")),
                line("GET", "/orders?page=0&size=10", req("GET", "/orders?page=0&size=10")))
            "B09" -> listOf(
                line("GET", "/pricing?sku=SKU-1&quantity=1", req("GET", "/pricing?sku=SKU-1&quantity=1")),
                line("GET", "/pricing?sku=SKU-2&quantity=1", req("GET", "/pricing?sku=SKU-2&quantity=1")))
            "B10" -> listOf(line("GET", "/orders/9999", req("GET", "/orders/9999")))
            "B14" -> listOf(line("GET", "/pricing?sku=SKU-1&quantity=1", req("GET", "/pricing?sku=SKU-1&quantity=1")))
            "B16" -> listOf(line("POST", "/orders", req("POST", "/orders", """{"sku":"SKU-1","quantity":1}""")))
            else -> throw IllegalArgumentException("no trigger sequence for $bugId in the S0-6 set")
        }
    }

    private fun waitForHealth(port: Int) {
        val deadline = System.currentTimeMillis() + 90_000
        while (System.currentTimeMillis() < deadline) {
            try {
                val r = http.send(HttpRequest.newBuilder(URI.create("http://localhost:$port/stock/SKU-1")).build(),
                    HttpResponse.BodyHandlers.ofString())
                if (r.statusCode() == 200) return
            } catch (e: Exception) { /* not up yet */ }
            Thread.sleep(500)
        }
        throw IllegalStateException("demo app did not start on port $port")
    }

    private fun extractStack(log: Path): String {
        if (!Files.isRegularFile(log)) return ""
        val lines = Files.readAllLines(log)
        val start = lines.indexOfLast { it.contains("Exception") || it.contains("ERROR") }
        if (start < 0) return ""
        return lines.drop(start).filter { it.contains("Exception") || it.trim().startsWith("at trace.demoapp") }
            .take(20).joinToString("\n")
    }
}

/** Reads ONLY the non-ground-truth sections of a fixture (id, category, trigger). */
object FixtureDoc {
    data class Doc(val category: String, val triggerText: String)

    fun load(fixtureMd: Path): Doc {
        val lines = Files.readAllLines(fixtureMd)
        val category = lines.firstOrNull { it.trim().startsWith("- category:") }
            ?.substringAfter(":")?.trim() ?: "unknown"
        val trigger = StringBuilder()
        var inTrigger = false
        for (line in lines) {
            when {
                line.trim().startsWith("- trigger:") -> {
                    inTrigger = true
                    trigger.appendLine(line.trim().removePrefix("- trigger:").trim())
                }
                inTrigger && line.startsWith("  ") -> trigger.appendLine(line.trim())
                inTrigger -> inTrigger = false
            }
            // Hard stop: nothing past the trigger block is ever read into a prompt.
            if (inTrigger.not() && trigger.isNotEmpty() && line.contains("ground-truth")) break
        }
        val text = trigger.toString().trim()
        require(text.isNotBlank()) { "no trigger section in $fixtureMd" }
        require(!text.contains("ground-truth", ignoreCase = true)) { "refusing to leak ground truth from $fixtureMd" }
        return Doc(category, text)
    }
}
