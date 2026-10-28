package trace.store

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*

class SqliteTraceStoreTest {

    private fun allEventTypes(): List<StoredEvent> = listOf(
        StoredEvent("call", 1, 100, 1, invocationId = 1, className = "com.acme.OrderService",
            methodName = "place", descriptor = "(Ljava/lang/String;)V", sourceFile = "OrderService.java", sourceLine = 42,
            payloadJson = """{"capture_level":"summary","arg_summaries":[{"type_name":"java.lang.String","summary":"SKU-1","redacted":false,"unsafe_to_render":false,"truncated":false}]}"""),
        StoredEvent("return", 2, 200, 1, invocationId = 1, payloadJson = """{"duration_ns":100}"""),
        StoredEvent("throw", 3, 300, 1, invocationId = 2, className = "com.acme.OrderService",
            methodName = "fail", descriptor = "()V",
            payloadJson = """{"exception_type":"java.lang.IllegalStateException","exception_message_summary":"boom","exception_identity":7}"""),
        StoredEvent("catch", 4, 400, 1, invocationId = 1, className = "com.acme.OrderService",
            methodName = "place", descriptor = "(Ljava/lang/String;)V", payloadJson = """{"throw_event_id":3}"""),
        StoredEvent("assign", 5, 500, 1, invocationId = 1, className = "com.acme.OrderService",
            methodName = "place", descriptor = "(Ljava/lang/String;)V",
            payloadJson = """{"var_index":1,"var_name":"x","value":{"type_name":"int","summary":"3","redacted":false,"unsafe_to_render":false,"truncated":false}}"""),
        StoredEvent("field-write", 6, 600, 1, invocationId = 1, className = "com.acme.OrderService",
            methodName = "place", descriptor = "(Ljava/lang/String;)V",
            payloadJson = """{"owner_class":"com.acme.Order","field_name":"total","field_descriptor":"I","value":{"type_name":"int","summary":"3","redacted":false,"unsafe_to_render":false,"truncated":false}}"""),
        StoredEvent("thread-start", 7, 700, 1, payloadJson = """{"thread_name":"main"}"""),
        StoredEvent("thread-handoff", 8, 800, 1, invocationId = 1,
            payloadJson = """{"from_thread_id":1,"handoff_kind":"executor-submit","task_identity":9,"submission_invocation_id":1}"""),
        StoredEvent("snapshot-marker", 9, 900, 1, payloadJson = """{"marker_kind":"dump-end","dropped_event_count":0}"""),
    )

    @Test fun roundTripEveryEventType(@TempDir dir: Path) {
        val db = dir.resolve("trace.db").toString()
        SqliteTraceStore(db).use { store ->
            store.createRun("run-1", startedAtNs = 1000, app = "demo", captureProfile = "summary")
            store.insertEvents("run-1", allEventTypes())
            store.finalizeRun("run-1", droppedEvents = 0, complete = true)
            val stored = store.eventsOf("run-1")
            assertEquals(allEventTypes().map { it.eventType }, stored.map { it["event_type"] })
            val call = stored.first { it["event_type"] == "call" }
            assertEquals("com.acme.OrderService", call["class_name"])
            assertEquals("place", call["method_name"])
            assertTrue((call["payload"] as String).contains("SKU-1"))
            val info = store.runInfo("run-1")!!
            assertTrue(info.complete); assertEquals(0, info.droppedEvents)
        }
        // Reopen: data survives, DDL is idempotent.
        SqliteTraceStore(db).use { assertEquals(9, it.eventsOf("run-1").size) }
    }

    @Test fun incompleteRunIsFlaggedAndDropCountsPersist(@TempDir dir: Path) {
        SqliteTraceStore(dir.resolve("trace.db").toString()).use { store ->
            store.createRun("run-2")
            store.insertEvents("run-2", listOf(allEventTypes().first()))
            store.finalizeRun("run-2", droppedEvents = 42, complete = false)
            val info = store.runInfo("run-2")!!
            assertFalse(info.complete)
            assertEquals(42, info.droppedEvents)
            assertEquals("partial", info.asyncCoverage)
        }
    }

    @Test fun redactedSecretNeverReachesStorage(@TempDir dir: Path) {
        SqliteTraceStore(dir.resolve("trace.db").toString()).use { store ->
            store.createRun("run-3")
            store.insertEvents("run-3", listOf(StoredEvent("call", 1, 100, 1, invocationId = 1,
                className = "com.acme.Login", methodName = "login", descriptor = "(Ljava/lang/String;)V",
                payloadJson = """{"arg_summaries":[{"type_name":"java.lang.String","summary":"","redacted":true,"unsafe_to_render":false,"truncated":false}]}""")))
            val payload = store.eventsOf("run-3").single()["payload"] as String
            assertTrue(payload.contains("\"redacted\":true"))
            assertFalse(payload.contains("hunter2"))
        }
    }
}

