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
    }

    @Test fun scoringAndNotRunRows(@TempDir dir: Path) {
        val results = dir.resolve("results"); val fixtures = dir.resolve("fixtures")
        Files.createDirectories(results); Files.createDirectories(fixtures.resolve("B02"))
        Files.writeString(fixtures.resolve("B02/fixture.md"),
            "- ground-truth root cause location: `trace.demoapp.service.TaxService#taxFor`")
        for (bug in listOf("B03", "B04", "B05", "B09", "B10", "B14", "B16")) {
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
