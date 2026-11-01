package trace.eval

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.store.SqliteTraceStore
import trace.store.StoredEvent
import java.nio.file.Path

class ExternalCompareTest {
    @Test fun resolveRunIdFromDbAndExplicit(@TempDir dir: Path) {
        val db = dir.resolve("trace.db")
        SqliteTraceStore(db.toString()).use { store ->
            store.createRun("run-external")
            store.insertEvents("run-external", listOf(
                StoredEvent("call", 1, 10, 1, invocationId = 1, className = "com.acme.App", methodName = "main", descriptor = "()V")))
            store.finalizeRun("run-external", 0, true)
        }
        assertEquals("run-external", ExternalCompare.resolveRunId(db, null))
        assertEquals("given", ExternalCompare.resolveRunId(db, "given"))
    }
}
