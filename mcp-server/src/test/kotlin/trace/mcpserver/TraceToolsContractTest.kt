package trace.mcpserver

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.store.SqliteTraceStore
import trace.store.StoredEvent
import java.nio.file.Files
import java.nio.file.Path

/** Contract tests per llm-strategy.md + testing-ci.md: shape, caps, pagination, citable IDs, structured errors. */
class TraceToolsContractTest {

    private fun fixture(dir: Path): TraceTools {
        val db = dir.resolve("t.db")
        SqliteTraceStore(db.toString()).use { s ->
            s.createRun("run-1")
            s.insertEvents("run-1", listOf(
                StoredEvent("call", 1, 10, 1, invocationId = 1, className = "acme.Main", methodName = "main", descriptor = "()V"),
                StoredEvent("call", 2, 20, 1, invocationId = 2, parentInvocationId = 1, className = "acme.OrderService", methodName = "placeOrder", descriptor = "()V"),
                StoredEvent("call", 3, 30, 1, invocationId = 3, parentInvocationId = 2, className = "acme.Pricing", methodName = "price", descriptor = "()V"),
                StoredEvent("throw", 4, 40, 1, invocationId = 3, className = "acme.Pricing", methodName = "price", descriptor = "()V",
                    payloadJson = """{"exception_type":"java.lang.NullPointerException","exception_message_summary":"product is null","exception_identity":9}"""),
            ))
            s.finalizeRun("run-1", 0, true)
        }
        val src = dir.resolve("src/acme"); Files.createDirectories(src)
        Files.writeString(src.resolve("OrderService.java"),
            "package acme;\npublic class OrderService {\n  public void placeOrder() { /* seeded line */ }\n}\n")
        return TraceTools(db.toString(), listOf(dir.resolve("src")))
    }

    private fun args(vararg pairs: Pair<String, Any>): JsonObject = buildJsonObject {
        pairs.forEach { (k, v) -> when (v) {
            is String -> put(k, v); is Int -> put(k, v); is Long -> put(k, v)
        } }
    }

    @Test fun failurePointShapeAndCaps(@TempDir dir: Path) {
        val out = fixture(dir).call("get_failure_point", args("run_id" to "run-1"))
        assertFalse(out.isError, out.body.toString())
        assertTrue(out.body.containsKey("summary")); assertTrue(out.body.containsKey("truncated"))
        val items = out.body["items"]!!.jsonArray
        assertEquals("throw", items[0].jsonObject["kind"]!!.jsonPrimitive.content)
        assertTrue(items[0].jsonObject.containsKey("event_id"))
        assertTrue(items.size <= 21) // 1 failure + at most 20 frames (contract cap)
    }

    @Test fun structuredErrors(@TempDir dir: Path) {
        val tools = fixture(dir)
        val unknownRun = tools.call("get_failure_point", args("run_id" to "nope"))
        assertTrue(unknownRun.isError)
        assertEquals("RUN_NOT_FOUND", unknownRun.body["code"]!!.jsonPrimitive.content)
        assertTrue(unknownRun.body.containsKey("message")); assertTrue(unknownRun.body.containsKey("hint"))

        val unknownTarget = tools.call("trace_back", args("run_id" to "run-1", "target_id" to 9999))
        assertEquals("TARGET_NOT_FOUND", unknownTarget.body["code"]!!.jsonPrimitive.content)

        val badDepth = tools.call("trace_back", args("run_id" to "run-1", "target_id" to 1, "max_depth" to 0))
        assertEquals("INVALID_ARGUMENT", badDepth.body["code"]!!.jsonPrimitive.content)

        val badCursor = tools.call("trace_back", args("run_id" to "run-1", "target_id" to 1, "cursor" to "!!!not-base64!!!"))
        assertEquals("INVALID_ARGUMENT", badCursor.body["code"]!!.jsonPrimitive.content)

        val missingArg = tools.call("trace_back", args("run_id" to "run-1"))
        assertEquals("INVALID_ARGUMENT", missingArg.body["code"]!!.jsonPrimitive.content)

        val unknownTool = tools.call("drop_tables", args())
        assertTrue(unknownTool.isError)
    }

    @Test fun traceBackItemsAreCitable(@TempDir dir: Path) {
        val tools = fixture(dir)
        val failure = tools.call("get_failure_point", args("run_id" to "run-1"))
        val targetId = failure.body["items"]!!.jsonArray[0].jsonObject["event_id"]!!.jsonPrimitive.long
        val slice = tools.call("trace_back", args("run_id" to "run-1", "target_id" to targetId))
        assertFalse(slice.isError, slice.body.toString())
        val wheres = slice.body["items"]!!.jsonArray.map { it.jsonObject["where"]!!.jsonPrimitive.content }
        assertEquals(listOf("acme.Pricing#price", "acme.Pricing#price", "acme.OrderService#placeOrder", "acme.Main#main"), wheres)
        // Drill-down: an ID from this response must be usable as the next target.
        val again = tools.call("trace_back", args("run_id" to "run-1",
            "target_id" to slice.body["items"]!!.jsonArray[1].jsonObject["event_id"]!!.jsonPrimitive.long))
        assertFalse(again.isError, again.body.toString())
    }

    @Test fun subtreeAndSourceContracts(@TempDir dir: Path) {
        val tools = fixture(dir)
        val subtree = tools.call("get_call_subtree", args("run_id" to "run-1", "invocation_id" to 1))
        assertFalse(subtree.isError, subtree.body.toString())
        assertEquals(3, subtree.body["items"]!!.jsonArray.size)

        val unknownInvocation = tools.call("get_call_subtree", args("run_id" to "run-1", "invocation_id" to 4242))
        assertEquals("INVOCATION_NOT_FOUND", unknownInvocation.body["code"]!!.jsonPrimitive.content)

        val source = tools.call("get_source", args("symbol_id" to "acme.OrderService#placeOrder"))
        assertFalse(source.isError, source.body.toString())
        assertTrue(source.body["items"]!!.jsonArray[0].jsonObject["snippet"]!!.jsonPrimitive.content.contains("seeded line"))

        val missing = tools.call("get_source", args("class_name" to "acme.Nope"))
        assertEquals("SOURCE_NOT_FOUND", missing.body["code"]!!.jsonPrimitive.content)

        val traversal = tools.call("get_source", args("class_name" to "../secret.Evil"))
        assertEquals("SOURCE_NOT_FOUND", traversal.body["code"]!!.jsonPrimitive.content)
    }
}
