package trace.eval

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ScoreMainTest {
    @Test fun groundTruthParsing(@TempDir dir: Path) {
        val fixture = dir.resolve("fixture.md")
        Files.writeString(fixture, "- ground-truth root cause location: `trace.demoapp.service.TaxService#taxFor` returning null")
        val truth = ScoreMain.groundTruth(fixture)
        assertEquals("trace.demoapp.service.TaxService", truth.className)
        assertEquals("taxFor", truth.method)
    }

    @Test fun realFixturesParse() {
        val fixtures = Path.of("../eval/fixtures")
        // At minimum the exception-shaped fixtures cite a method-level truth.
        val b04 = ScoreMain.groundTruth(fixtures.resolve("B04/fixture.md"))
        assertTrue(b04.className.endsWith("InventoryService"), b04.toString())
        assertEquals("decrement", b04.method)
        // Every fixture in the 20-bug set must parse to a trace.demoapp class.
        // Method-level truth is required for the Step-2 fixtures; older ones
        // (e.g. B09) legitimately cite a class/field only (freeze-s0-9.md).
        val methodLevel = listOf("B06", "B07", "B08", "B11", "B12", "B13", "B15", "B17", "B18", "B19", "B20")
        for (i in 1..20) {
            val id = "B%02d".format(i)
            val truth = ScoreMain.groundTruth(fixtures.resolve("$id/fixture.md"))
            assertTrue(truth.className.startsWith("trace.demoapp."), "$id -> $truth")
            if (id in methodLevel) assertNotNull(truth.method, "$id must cite a method-level ground truth")
        }
    }

    @Test fun scoringAndNotRunRows(@TempDir dir: Path) {
        val results = dir.resolve("results"); val fixtures = dir.resolve("fixtures")
        Files.createDirectories(results); Files.createDirectories(fixtures.resolve("B02"))
        Files.writeString(fixtures.resolve("B02/fixture.md"),
            "- ground-truth root cause location: `trace.demoapp.service.TaxService#taxFor`")
        for (bug in (1..20).map { "B%02d".format(it) } - "B02") {
            Files.createDirectories(fixtures.resolve(bug))
            Files.writeString(fixtures.resolve("$bug/fixture.md"),
                "- ground-truth root cause location: `trace.demoapp.service.TaxService#taxFor`")
        }
        Files.writeString(results.resolve("B02-meta-trace.md"), """
            ---
            bug: B02
            runner: trace
            provider: meta
            model: muse-wonder-1.0
            claimed_root_cause: trace.demoapp.service.TaxService#taxFor
            budget: tokens_in=100 tokens_out=50 cost_usd=0.0014 tool_calls=4
            budget_exceeded: false
            wall_ms: 1234
            ---
            Root cause: fake (unconfirmed)
            ROOT_CAUSE: trace.demoapp.service.TaxService#taxFor
        """.trimIndent())
        val rows = ScoreMain.collect(results, fixtures)
        val scored = rows.single { it.bug == "B02" && it.provider == "meta" && it.runner == "trace" }
        assertEquals(true, scored.methodCorrect)
        assertEquals(0.0014, scored.costUsd!!, 0.00001)
        val notRun = rows.single { it.bug == "B02" && it.provider == "anthropic" && it.runner == "baseline" }
        assertNull(notRun.claimed)
        val markdown = ScoreMain.renderMarkdown(rows)
        assertTrue(markdown.contains("NOT RUN"))
        assertTrue(markdown.contains("no completed investigations"))
    }
}
