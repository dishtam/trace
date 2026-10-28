package trace.collector

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.store.SqliteTraceStore
import java.nio.file.Path

class TraceIngestTest {
    @Test fun ingestAgentFileIntoSqlite(@TempDir dir: Path) {
        val events = dir.resolve("events.jsonl").toFile()
        events.writeText("""
            {"run_id":"run-x","schema_version":"0.1","event_type":"thread-start","seq":1,"timestamp_ns":100,"thread_id":1,"payload":{"thread_name":"main"}}
            {"run_id":"run-x","schema_version":"0.1","event_type":"call","seq":2,"timestamp_ns":110,"thread_id":1,"invocation_id":1,"class_name":"com.acme.OrderService","method_name":"place","descriptor":"()V","payload":{"capture_level":"summary","arg_summaries":[]}}
            {"run_id":"run-x","schema_version":"0.1","event_type":"return","seq":3,"timestamp_ns":120,"thread_id":1,"invocation_id":1,"payload":{"duration_ns":10}}
            {"run_id":"run-x","schema_version":"0.1","event_type":"snapshot-marker","seq":4,"timestamp_ns":130,"thread_id":1,"payload":{"marker_kind":"drop-notice","dropped_event_count":5,"reason":"shutdown"}}
            {"run_id":"run-x","schema_version":"0.1","event_type":"snapshot-marker","seq":5,"timestamp_ns":140,"thread_id":1,"payload":{"marker_kind":"dump-end","dropped_event_count":5,"reason":"shutdown"}}
        """.trimIndent())
        val db = dir.resolve("trace.db").toFile()
        val summary = TraceIngest.ingest(events, db, app = "demo")
        assertEquals("run-x", summary.runId); assertEquals(5, summary.events)
        assertEquals(5, summary.dropped); assertTrue(summary.complete)
        SqliteTraceStore(db.absolutePath).use { store ->
            val rows = store.eventsOf("run-x")
            assertEquals(listOf("thread-start", "call", "return", "snapshot-marker", "snapshot-marker"),
                rows.map { it["event_type"] })
            assertEquals("place", rows[1]["method_name"])
            val info = store.runInfo("run-x")!!
            assertTrue(info.complete); assertEquals(5, info.droppedEvents)
        }
    }

    @Test fun missingDumpEndMarksRunIncomplete(@TempDir dir: Path) {
        val events = dir.resolve("events.jsonl").toFile()
        events.writeText("""{"run_id":"run-y","schema_version":"0.1","event_type":"call","seq":1,"timestamp_ns":100,"thread_id":1,"invocation_id":1,"class_name":"com.acme.A","method_name":"m","descriptor":"()V","payload":{}}""")
        val db = dir.resolve("trace.db").toFile()
        val summary = TraceIngest.ingest(events, db)
        assertFalse(summary.complete)
        SqliteTraceStore(db.absolutePath).use { assertFalse(it.runInfo("run-y")!!.complete) }
    }
}
