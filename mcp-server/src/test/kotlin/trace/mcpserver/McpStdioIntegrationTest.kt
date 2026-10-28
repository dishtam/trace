package trace.mcpserver

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.store.SqliteTraceStore
import trace.store.StoredEvent
import java.io.File
import java.nio.file.Path

/**
 * Exit-check shape for S0-5 over the real MCP protocol (stdio): a B01-shaped run
 * (NPE in OrderService.placeOrder, ground truth in eval/fixtures/B01/fixture.md)
 * must be answerable end to end. The live capture from the real agent is verified
 * separately in the S0-5 report; this test keeps the protocol honest in CI.
 */
class McpStdioIntegrationTest {

    @Test fun toolsAnswerOnB01ShapedRun(@TempDir dir: Path) = runBlocking {
        val db = dir.resolve("b01.db")
        SqliteTraceStore(db.toString()).use { s ->
            s.createRun("b01-run")
            s.insertEvents("b01-run", listOf(
                StoredEvent("call", 1, 10, 1, invocationId = 1, className = "trace.demoapp.web.OrderController", methodName = "place", descriptor = "(Ltrace/demoapp/web/OrderController\$PlaceOrderRequest;)Ljava/util/Map;"),
                StoredEvent("call", 2, 20, 1, invocationId = 2, parentInvocationId = 1, className = "trace.demoapp.service.OrderService", methodName = "placeOrder", descriptor = "(Ljava/lang/String;I)Ltrace/demoapp/data/OrderEntity;"),
                StoredEvent("throw", 3, 30, 1, invocationId = 2, className = "trace.demoapp.service.OrderService", methodName = "placeOrder", descriptor = "(Ljava/lang/String;I)Ltrace/demoapp/data/OrderEntity;",
                    payloadJson = """{"exception_type":"java.lang.NullPointerException","exception_message_summary":"Cannot read field \"priceCents\" because \"product\" is null","exception_identity":42}"""),
            ))
            s.finalizeRun("b01-run", 0, true)
        }
        val sourceRoot = Path.of("../demo-app/src/main/java").toAbsolutePath().normalize()
        // Subprocess classpath from the code sources of classes this test already
        // loaded (java.class.path in a Gradle test worker is only the bootstrap).
        val classPath = System.getProperty("mcp.test.classpath")
            ?: error("mcp.test.classpath system property missing (set by mcp-server/build.gradle.kts)")
        assertTrue(classPath.contains("kotlin-sdk"), "test classpath lacks the MCP SDK: $classPath")
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val serverLog = dir.resolve("server.log").toFile()
        val process = ProcessBuilder(java, "-cp", classPath, "trace.mcpserver.McpServerKt",
            db.toString(), sourceRoot.toString()).redirectError(serverLog).start()
        try {
            val transport = StdioClientTransport(
                process.inputStream.asSource().buffered(),
                process.outputStream.asSink().buffered(), null)
            val client = Client(Implementation(name = "trace-test-client", version = "0.1.0"))
            try {
                client.connect(transport)
            } catch (e: Exception) {
                fail<Unit>("MCP connect failed: $e; server log:\n${serverLog.readText()}")
            }

            val tools = client.listTools().tools.map { it.name }
            assertEquals(TraceTools.TOOL_NAMES.sorted(), tools.sorted())

            fun call(name: String, args: Map<String, Any?>): JsonObject = runBlocking {
                val result = client.callTool(name, args)
                val text = (result.content.first() as TextContent).text!!
                Json.parseToJsonElement(text).jsonObject
            }

            val failure = call("get_failure_point", mapOf("run_id" to "b01-run"))
            val failureItems = failure["items"]!!.jsonArray
            assertEquals("java.lang.NullPointerException",
                failureItems[0].jsonObject["exception_type"]!!.jsonPrimitive.content)
            assertTrue(failureItems[0].jsonObject["where"]!!.jsonPrimitive.content
                .contains("OrderService#placeOrder"), failure.toString())

            val targetId = failureItems[0].jsonObject["event_id"]!!.jsonPrimitive.long
            val slice = call("trace_back", mapOf("run_id" to "b01-run", "target_id" to targetId))
            val wheres = slice["items"]!!.jsonArray.map { it.jsonObject["where"]!!.jsonPrimitive.content }
            assertTrue(wheres.any { it.contains("OrderController#place") }, wheres.toString())
            // Ground truth (fixture.md): pricing/tax never run for B01.
            assertFalse(wheres.any { it.contains("PricingService") || it.contains("TaxService") }, wheres.toString())

            val source = call("get_source", mapOf("symbol_id" to "trace.demoapp.service.OrderService#placeOrder"))
            assertTrue(source["items"]!!.jsonArray[0].jsonObject["snippet"]!!.jsonPrimitive.content
                .contains("SEEDED-B01"), source.toString())

            val badRun = client.callTool("get_failure_point", mapOf("run_id" to "missing"))
            val badText = (badRun.content.first() as TextContent).text!!
            assertTrue(badText.contains("RUN_NOT_FOUND"), badText)
            client.close()
        } finally {
            process.destroyForcibly()
        }
    }
}
