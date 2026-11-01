package trace.eval

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RunnerWrapUpTest {
    @Test fun budgetExhaustionStillConcludesWithWellFormedConversation() {
        var executed = 0
        var wrapUpTools: List<ToolDef>? = null
        val tools = object : InvestigationTools {
            override fun defs() = listOf(ToolDef("probe", "probe", buildJsonObject { put("type", "object") }))
            override fun call(name: String, arguments: JsonObject): String {
                executed++; return "evidence-$executed"
            }
        }
        val client = object : LlmClient {
            override val provider = "fake"; override val model = "muse-wonder-1.0"
            private var step = 0
            override fun complete(system: String, messages: List<Msg>, tools: List<ToolDef>): LlmReply {
                return if (step++ == 0) LlmReply.Calls(listOf(
                    ToolCallRef("c1", "probe", buildJsonObject {}),
                    ToolCallRef("c2", "probe", buildJsonObject {})), "", Usage(10, 5))
                else {
                    wrapUpTools = tools
                    // Wrap-up is a fresh conversation: one digest message, no history.
                    assertEquals(1, messages.size, "wrap-up must start fresh: $messages")
                    val digest = (messages.single() as Msg.User).text
                    assertTrue(digest.contains("evidence-1"), "digest must carry the tool log: $digest")
                    LlmReply.Text("Root cause: wrapped (unconfirmed)\nROOT_CAUSE: com.acme.Fake#wrapped", Usage(10, 5))
                }
            }
        }
        val result = InvestigationRunner(client, tools, maxTurns = 5, budgetCapUsd = 10.0, maxToolCalls = 1)
            .investigate("symptom")
        assertEquals(1, executed, "second call in the batch must not execute after budget exhaustion")
        assertEquals(emptyList<ToolDef>(), wrapUpTools, "wrap-up turn must be tool-free")
        assertTrue(result.budgetExceeded)
        assertEquals("com.acme.Fake#wrapped", result.claimedRootCause)
    }

    @Test fun wrapUpRetriesWhenFirstConcludingReplyIsNotText() {
        val tools = object : InvestigationTools {
            override fun defs() = listOf(ToolDef("probe", "probe", buildJsonObject { put("type", "object") }))
            override fun call(name: String, arguments: JsonObject): String = "evidence"
        }
        val client = object : LlmClient {
            override val provider = "fake"; override val model = "muse-wonder-1.0"
            private var step = 0
            override fun complete(system: String, messages: List<Msg>, tools: List<ToolDef>): LlmReply {
                return when (step++) {
                    0 -> LlmReply.Calls(listOf(
                        ToolCallRef("c1", "probe", buildJsonObject {}),
                        ToolCallRef("c2", "probe", buildJsonObject {})), "", Usage(10, 5))
                    1 -> LlmReply.Calls(listOf(
                        ToolCallRef("c3", "probe", buildJsonObject {})), "", Usage(10, 5))
                    else -> LlmReply.Text(
                        "Root cause: retried (unconfirmed)\nROOT_CAUSE: com.acme.Fake#retried", Usage(10, 5))
                }
            }
        }
        val result = InvestigationRunner(client, tools, maxTurns = 5, budgetCapUsd = 10.0, maxToolCalls = 1)
            .investigate("symptom")
        assertEquals("com.acme.Fake#retried", result.claimedRootCause)
        assertTrue(result.toolCallsLog.any { it.contains("wrap-up retry") }, result.toolCallsLog.toString())
    }
}
