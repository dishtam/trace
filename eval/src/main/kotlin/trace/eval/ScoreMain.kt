package trace.eval

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.streams.asSequence

/**
 * S0-9 scoring (rules frozen in eval/freeze-s0-9.md before any live run):
 * claimed location = a report's ROOT_CAUSE line; ground truth = the first
 * Class#method cited in the fixture's "ground-truth root cause location"
 * (class-only where the fixture names no method). Emits the per-bug results
 * table (evaluation.md format) plus means per provider/runner. Bugs with no
 * report are listed as NOT RUN, never dropped silently.
 *
 * Usage: ScoreMainKt [resultsDir] [fixturesDir]  (defaults eval/results eval/fixtures)
 */
object ScoreMain {
    private val BUGS = listOf("B02", "B03", "B04", "B05", "B09", "B10", "B14", "B16")
    private val PROVIDERS = listOf("meta", "anthropic")
    private val RUNNERS = listOf("baseline", "trace")

    data class GroundTruth(val className: String, val method: String?)
    data class Row(
        val bug: String, val provider: String, val runner: String, val claimed: String?,
        val methodCorrect: Boolean?, val classCorrect: Boolean?,
        val tokensIn: Long?, val tokensOut: Long?, val costUsd: Double?,
        val toolCalls: Int?, val wallMs: Long?,
    )

    fun groundTruth(fixtureMd: Path): GroundTruth {
        val text = Files.readString(fixtureMd)
        val section = text.substringAfter("ground-truth root cause location:", missingDelimiterValue = "")
        require(section.isNotBlank()) { "no ground-truth location in $fixtureMd" }
        val cited = Regex("""(trace\.demoapp\.[A-Za-z0-9_.]+)(?:#([A-Za-z0-9_]+))?""").find(section)
            ?: Regex("""`?([A-Z][A-Za-z0-9_]*)(?:#([A-Za-z0-9_]+))?`?""").find(section)
            ?: throw IllegalArgumentException("no class cited in $fixtureMd")
        val fqcn = cited.groupValues[1]
        return GroundTruth(fqcn, cited.groupValues[2].ifBlank { null })
    }

    fun scoreReport(report: Path, truth: GroundTruth): Triple<String?, Boolean?, Boolean?> {
        val header = Files.readAllLines(report).take(20).joinToString("\n")
        val claimed = Regex("""claimed_root_cause:\s*(.+)""").find(header)?.groupValues?.get(1)?.trim()
            ?.takeIf { it != "(none parsed)" }
            ?: Regex("""ROOT_CAUSE:\s*(\S+)""").find(Files.readString(report))?.groupValues?.get(1)
        if (claimed == null) return Triple(null, false, false)
        val claimedClass = claimed.substringBefore('#')
        val claimedMethod = claimed.substringAfter('#', "")
        // Class-level hit: same simple class name (fixtures cite FQCNs; models may shorten).
        val classCorrect = claimedClass.substringAfterLast('.') == truth.className.substringAfterLast('.')
        val methodCorrect = if (truth.method == null) null
            else classCorrect && claimedMethod == truth.method
        return Triple(claimed, methodCorrect, classCorrect)
    }

    private fun headerValue(report: Path, key: String): String? {
        val header = Files.readAllLines(report).take(20)
        return header.firstOrNull { it.startsWith("$key:") }?.substringAfter(":")?.trim()
    }

    fun collect(resultsDir: Path, fixturesDir: Path): List<Row> {
        val rows = mutableListOf<Row>()
        for (bug in BUGS) {
            val truth = groundTruth(fixturesDir.resolve("$bug/fixture.md"))
            for (provider in PROVIDERS) for (runner in RUNNERS) {
                val report = resultsDir.resolve("$bug-$provider-$runner.md")
                if (!Files.isRegularFile(report)) {
                    rows += Row(bug, provider, runner, null, null, null, null, null, null, null, null)
                    continue
                }
                val (claimed, methodCorrect, classCorrect) = scoreReport(report, truth)
                val budget = headerValue(report, "budget") ?: ""
                fun num(pattern: String): Long? = Regex(pattern).find(budget)?.groupValues?.get(1)?.toLong()
                rows += Row(bug, provider, runner, claimed, methodCorrect, classCorrect,
                    num("""tokens_in=(\d+)"""), num("""tokens_out=(\d+)"""),
                    Regex("""cost_usd=([\d.]+)""").find(budget)?.groupValues?.get(1)?.toDouble(),
                    num("""tool_calls=(\d+)""")?.toInt(), headerValue(report, "wall_ms")?.toLong())
            }
        }
        return rows
    }

    fun renderMarkdown(rows: List<Row>): String {
        val out = StringBuilder()
        out.appendLine("| Bug | Provider | Runner | Claimed root cause | Method | Class | Tokens in/out | USD | Tool calls | Wall |")
        out.appendLine("|---|---|---|---|---|---|---|---|---|---|")
        for (row in rows) {
            fun mark(v: Boolean?) = when (v) { true -> "yes"; false -> "no"; null -> "n/a" }
            out.appendLine("| ${row.bug} | ${row.provider} | ${row.runner} | ${row.claimed ?: "NOT RUN"} | " +
                "${if (row.claimed == null) "-" else mark(row.methodCorrect)} | ${if (row.claimed == null) "-" else mark(row.classCorrect)} | " +
                "${row.tokensIn ?: "-"}/${row.tokensOut ?: "-"} | ${row.costUsd?.let { "%.4f".format(it) } ?: "-"} | " +
                "${row.toolCalls ?: "-"} | ${row.wallMs?.let { "${it}ms" } ?: "-"} |")
        }
        out.appendLine()
        for (provider in PROVIDERS) for (runner in RUNNERS) {
            val group = rows.filter { it.provider == provider && it.runner == runner && it.claimed != null }
            if (group.isEmpty()) {
                out.appendLine("- $provider/$runner: no completed investigations (0/${BUGS.size} run)")
            } else {
                val methodHits = group.count { it.methodCorrect == true }
                val classHits = group.count { it.classCorrect == true }
                val meanCost = group.mapNotNull { it.costUsd }.average()
                out.appendLine("- $provider/$runner: method ${methodHits}/${group.size}, class ${classHits}/${group.size}, " +
                    "mean USD ${"%.4f".format(meanCost)} over ${group.size} completed investigations")
            }
        }
        return out.toString()
    }

    @JvmStatic fun main(args: Array<String>) {
        val resultsDir = Path.of(args.getOrElse(0) { "eval/results" })
        val fixturesDir = Path.of(args.getOrElse(1) { "eval/fixtures" })
        val rows = collect(resultsDir, fixturesDir)
        val markdown = renderMarkdown(rows)
        println(markdown)
        Files.writeString(resultsDir.resolve("scores.md"), markdown)
    }
}
