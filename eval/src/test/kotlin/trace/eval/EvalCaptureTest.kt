package trace.eval

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.store.SqliteTraceStore
import java.nio.file.Files
import java.nio.file.Path

/**
 * Capture-half integration test (no LLM, no keys): one seeded bug is
 * reproduced under the real agent and stored, and the symptom text given to
 * both runners is produced. The LLM half is covered by the contract tests
 * with fake transports; live runs are operator-invoked (keys in env only).
 */
class EvalCaptureTest {
    @Test fun captureB16ProducesRunAndSymptom(@TempDir dir: Path) {
        val repo = Path.of("..").toAbsolutePath().normalize()
        val jar = repo.resolve("demo-app/build/libs/demo-app.jar")
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(jar),
            "demo bootJar not built; run gradlew.bat :demo-app:bootJar :agent:shadowJar")
        val harness = EvalHarness(repo, dir.resolve("work"), port = 18411)
        val bug = harness.capture("B16")
        assertNotNull(bug.runId)
        assertTrue(bug.symptom.contains("totalCents") || bug.symptom.contains("HTTP 200"), bug.symptom)
        assertFalse(bug.symptom.contains("ground-truth", ignoreCase = true), bug.symptom)
        SqliteTraceStore(bug.dbPath.toString()).use { store ->
            assertTrue(store.runExists(bug.runId!!))
            val calls = store.eventsOf(bug.runId!!, "call")
            assertTrue(calls.any { it["method_name"] == "place" }, calls.toString())
        }
    }
}
