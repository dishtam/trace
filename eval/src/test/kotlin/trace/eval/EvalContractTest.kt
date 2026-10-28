package trace.eval

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class EvalContractTest {

    @Test fun anthropicClientSendsToolsAndParsesToolUse() {
        var sentBody = ""
        val http = HttpTransport { _, _, body ->
            sentBody = body
            """{"content":[{"type":"text","text":"checking"},{"type":"tool_use","id":"t1","name":"get_failure_point","input":{"run_id":"r1"}}],
               "usage":{"input_tokens":100,"output_tokens":20}}"""
        }
        val client = AnthropicClient("test-key", http = http)
        val reply = client.complete("sys", listOf(Msg.User("go")), listOf(
            ToolDef("get_failure_point", "what failed", buildJsonObject { put("type", "object") })))
        assertTrue(sentBody.contains("\"input_schema\""), sentBody)
        assertTrue(sentBody.contains("\"name\":\"get_failure_point\""), sentBody)
        val calls = reply as LlmReply.Calls
        assertEquals("get_failure_point", calls.calls.single().name)
        assertEquals(100, reply.usage.inputTokens)
    }

    @Test fun metaClientNestsFunctionAndParsesToolCalls() {
        var sentBody = ""
        val http = HttpTransport { url, headers, body ->
            sentBody = body
            assertTrue(url.endsWith("/v1/chat/completions"), url)
            assertTrue(headers["Authorization"]!!.startsWith("Bearer "), headers.toString())
            """{"choices":[{"message":{"content":"","tool_calls":[{"id":"c1","type":"function",
               "function":{"name":"search_text","arguments":"{\"pattern\":\"SEEDED\"}"}}]}}],
               "usage":{"prompt_tokens":50,"completion_tokens":10}}"""
        }
        val client = MetaClient("test-key", http = http)
        val reply = client.complete("sys", listOf(Msg.User("go")), listOf(
            ToolDef("search_text", "grep", buildJsonObject { put("type", "object") })))
        assertTrue(sentBody.contains("\"type\":\"function\""), sentBody)
        val calls = reply as LlmReply.Calls
        assertEquals("search_text", calls.calls.single().name)
        assertEquals("SEEDED", calls.calls.single().arguments["pattern"]!!.jsonPrimitive.content)
    }

    @Test fun metaClientRefusesNonWonderModels() {
        assertThrows(IllegalArgumentException::class.java) {
            MetaClient("k", model = "muse-spark-contributor")
        }
        // The one allowed model constructs fine.
        MetaClient("k", model = "muse-wonder-1.0", http = HttpTransport { _, _, _ -> "{}" })
    }

    @Test fun budgetEnforcesCostAndCalls() {
        val budget = BudgetTracker("muse-wonder-1.0", maxCostUsd = 0.50, maxToolCalls = 2)
        budget.record(Usage(1000, 1000), 1)
        assertFalse(budget.exceeded())
        budget.record(Usage(0, 0), 2)
        assertTrue(budget.exceeded()) // tool-call cap
        val cost = BudgetTracker("claude-sonnet-5-5", maxCostUsd = 0.001)
        cost.record(Usage(1_000_000, 0), 0)
        assertTrue(cost.exceeded()) // $2 of input
        assertThrows(LlmException::class.java) { Prices.costUsd("made-up-model", Usage(1, 1)) }
    }

    @Test fun baselineToolsAreSandboxedAndCapped(@TempDir dir: Path) {
        val src = dir.resolve("src/demo"); Files.createDirectories(src)
        Files.writeString(src.resolve("A.java"), "class A { void m() { /* SEEDED thing */ } }")
        Files.writeString(dir.resolve("secret.txt"), "ground truth")
        val tools = BaselineTools(dir.resolve("src"))
        assertTrue(tools.call("search_text", buildJsonObject { put("pattern", "SEEDED") }).contains("A.java:1"))
        assertTrue(tools.call("read_file", buildJsonObject { put("path", "../secret.txt") }).startsWith("ERROR"))
        assertTrue(tools.call("read_file", buildJsonObject { put("path", "demo/A.java") }).contains("SEEDED"))
        assertTrue(tools.call("drop_tables", buildJsonObject {}).startsWith("ERROR"))
    }

    @Test fun fixtureDocNeverLeaksGroundTruth(@TempDir dir: Path) {
        val fixture = dir.resolve("fixture.md")
        Files.writeString(fixture, """
            - category: Null propagation
            - trigger: POST /orders with unknown SKU.
              Expected symptom: HTTP 500.
            - ground-truth root cause location: SecretClass#secretMethod
            - ground-truth explanation: the secret reason.
        """.trimIndent())
        val doc = FixtureDoc.load(fixture)
        assertEquals("Null propagation", doc.category)
        assertTrue(doc.triggerText.contains("POST /orders"))
        assertFalse(doc.triggerText.contains("SecretClass"))
        assertFalse(doc.triggerText.contains("secret reason"))
    }
}

/** Scripted client: one tool call, then a final answer. Exercises the shared loop. */
private class ScriptedClient(private val toolName: String) : LlmClient {
    override val provider = "fake"; override val model = "muse-wonder-1.0"
    private var step = 0
    override fun complete(system: String, messages: List<Msg>, tools: List<ToolDef>): LlmReply {
        assertTrue(tools.any { it.name == toolName }, "runner must offer the tool set it was given")
        return if (step++ == 0) LlmReply.Calls(
            listOf(ToolCallRef("id-1", toolName, buildJsonObject { put("pattern", "x") })), "", Usage(10, 5))
        else LlmReply.Text("Root cause: fake (unconfirmed)\nROOT_CAUSE: com.acme.Fake#method", Usage(10, 5))
    }
}

class RunnerLoopTest {
    @Test fun loopCallsToolThenReports(@TempDir dir: Path) {
        val src = dir.resolve("src"); Files.createDirectories(src)
        Files.writeString(src.resolve("A.java"), "class A {}")
        val result = BaselineTools(src).use { tools ->
            InvestigationRunner(ScriptedClient("search_text"), tools).investigate("symptom")
        }
        assertEquals("com.acme.Fake#method", result.claimedRootCause)
        assertEquals(1, result.budget.toolCalls)
        assertFalse(result.budgetExceeded)
    }
}
