package trace.eval

import kotlinx.serialization.json.*

/**
 * Anthropic Messages API (verified 2026-10-28, platform.claude.com):
 * POST https://api.anthropic.com/v1/messages, x-api-key + anthropic-version,
 * tools as [{name, description, input_schema}], replies carry text and
 * tool_use content blocks; tool results go back as tool_result blocks.
 */
class AnthropicClient(
    private val apiKey: String,
    override val model: String = "claude-sonnet-5-5",
    private val http: HttpTransport = JdkHttpTransport(),
    private val baseUrl: String = "https://api.anthropic.com",
) : LlmClient {
    override val provider = "anthropic"

    override fun complete(system: String, messages: List<Msg>, tools: List<ToolDef>): LlmReply {
        val body = buildJsonObject {
            put("model", model); put("max_tokens", 4096); put("system", system)
            putJsonArray("messages") { messages.forEach { add(encodeMessage(it)) } }
            putJsonArray("tools") {
                tools.forEach { tool ->
                    add(buildJsonObject {
                        put("name", tool.name); put("description", tool.description)
                        put("input_schema", tool.inputSchema)
                    })
                }
            }
        }
        val raw = http.post("$baseUrl/v1/messages",
            mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01"), body.toString())
        val json = Json.parseToJsonElement(raw).jsonObject
        val usage = Usage(json["usage"]?.jsonObject?.long("input_tokens") ?: 0,
            json["usage"]?.jsonObject?.long("output_tokens") ?: 0)
        val blocks = json["content"]?.jsonArray ?: return LlmReply.Text("", usage)
        val calls = blocks.mapNotNull { block ->
            val b = block.jsonObject
            if (b.str("type") == "tool_use") ToolCallRef(b.str("id") ?: "", b.str("name") ?: "",
                b["input"]?.jsonObject ?: buildJsonObject {}) else null
        }
        val text = blocks.mapNotNull { it.jsonObject.takeIf { b -> b.str("type") == "text" }?.str("text") }
            .joinToString("\n")
        return if (calls.isEmpty()) LlmReply.Text(text, usage) else LlmReply.Calls(calls, text, usage)
    }

    private fun encodeMessage(msg: Msg): JsonObject = when (msg) {
        is Msg.User -> buildJsonObject { put("role", "user"); put("content", msg.text) }
        is Msg.AssistantText -> buildJsonObject { put("role", "assistant"); put("content", msg.text) }
        is Msg.AssistantCalls -> buildJsonObject {
            put("role", "assistant")
            putJsonArray("content") {
                if (msg.text.isNotBlank()) add(buildJsonObject { put("type", "text"); put("text", msg.text) })
                msg.calls.forEach { call ->
                    add(buildJsonObject {
                        put("type", "tool_use"); put("id", call.id); put("name", call.name); put("input", call.arguments)
                    })
                }
            }
        }
        is Msg.ToolResults -> buildJsonObject {
            put("role", "user")
            putJsonArray("content") {
                msg.results.forEach { (id, result) ->
                    add(buildJsonObject { put("type", "tool_result"); put("tool_use_id", id); put("content", result) })
                }
            }
        }
    }

    companion object {
        fun fromEnvironment(http: HttpTransport = JdkHttpTransport()): AnthropicClient {
            val key = System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }
                ?: throw LlmException("ANTHROPIC_API_KEY is not set (environment variables only; never commit keys)")
            return AnthropicClient(key, System.getenv("TRACE_LLM_MODEL") ?: "claude-sonnet-5-5", http)
        }
    }
}
