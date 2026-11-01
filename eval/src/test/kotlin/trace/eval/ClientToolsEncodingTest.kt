package trace.eval

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClientToolsEncodingTest {
    @Test fun metaClientOmitsToolsFieldWhenNoToolsOffered() {
        var sentBody = ""
        val http = HttpTransport { _, _, body ->
            sentBody = body
            """{"choices":[{"message":{"content":"final answer"}}],"usage":{"prompt_tokens":10,"completion_tokens":5}}"""
        }
        val client = MetaClient("test-key", http = http)
        val reply = client.complete("sys", listOf(Msg.User("conclude")), emptyList())
        assertFalse(sentBody.contains("\"tools\""), "tools field must be omitted, was: $sentBody")
        assertEquals("final answer", (reply as LlmReply.Text).text)

        val withTools = MetaClient("test-key", http = http)
        withTools.complete("sys", listOf(Msg.User("go")), listOf(
            ToolDef("probe", "probe", buildJsonObject { put("type", "object") })))
        assertTrue(sentBody.contains("\"tools\""), "tools field must be present when offered: $sentBody")
    }

    @Test fun anthropicClientOmitsToolsFieldWhenNoToolsOffered() {
        var sentBody = ""
        val http = HttpTransport { _, _, body ->
            sentBody = body
            """{"content":[{"type":"text","text":"final answer"}],"usage":{"input_tokens":10,"output_tokens":5}}"""
        }
        val client = AnthropicClient("test-key", http = http)
        val reply = client.complete("sys", listOf(Msg.User("conclude")), emptyList())
        assertFalse(sentBody.contains("\"tools\""), "tools field must be omitted, was: $sentBody")
        assertEquals("final answer", (reply as LlmReply.Text).text)
    }
}
