package trace.eval

import kotlinx.serialization.json.*

/**
 * Meta Model API, Chat Completions surface (verified 2026-10-28, dev.meta.ai):
 * POST https://api.meta.ai/v1/chat/completions, Authorization: Bearer
 * $MODEL_API_KEY, tools nested as [{type:"function", function:{name,
 * description, parameters}}]; the assistant answers with tool_calls.
 * Only the pay-as-you-go Standard-mode model muse-wonder-1.0 is ever used —
 * never a Contributor-mode model/plan (llm-strategy.md, ADR-0006).
 */
class MetaClient(
    private val apiKey: String,
    override val model: String = "muse-wonder-1.0",
    private val http: HttpTransport = JdkHttpTransport(),
    private val baseUrl: String = "https://api.meta.ai",
) : LlmClient {
    override val provider = "meta"

    init {
        require(model == "muse-wonder-1.0") {
            "refusing model $model: only pay-as-you-go Standard-mode muse-wonder-1.0 is allowed (never Contributor mode)"
        }
    }

    override fun complete(system: String, messages: List<Msg>, tools: List<ToolDef>): LlmReply {
        val body = buildJsonObject {
            put("model", model); put("max_tokens", 4096)
            putJsonArray("messages") {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                messages.forEach { addAll(encodeMessage(it)) }
            }
            putJsonArray("tools") {
                tools.forEach { tool ->
                    add(buildJsonObject {
                        put("type", "function")
                        putJsonObject("function") {
                            put("name", tool.name); put("description", tool.description)
                            put("parameters", tool.inputSchema)
                        }
                    })
                }
            }
        }
        val raw = http.post("$baseUrl/v1/chat/completions",
            mapOf("Authorization" to "Bearer $apiKey"), body.toString())
        val json = Json.parseToJsonElement(raw).jsonObject
        val usageObj = json["usage"]?.jsonObject
        val usage = Usage(usageObj?.long("prompt_tokens") ?: 0, usageObj?.long("completion_tokens") ?: 0)
        val message = json["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject ?: return LlmReply.Text("", usage)
        val text = message.str("content") ?: ""
        val calls = (message["tool_calls"] as? JsonArray)?.mapNotNull { entry ->
            val call = entry.jsonObject
            val fn = call["function"]?.jsonObject ?: return@mapNotNull null
            ToolCallRef(call.str("id") ?: "", fn.str("name") ?: "", parseArgs(fn.str("arguments") ?: "{}"))
        } ?: emptyList()
        return if (calls.isEmpty()) LlmReply.Text(text, usage) else LlmReply.Calls(calls, text, usage)
    }

    private fun encodeMessage(msg: Msg): List<JsonObject> = when (msg) {
        is Msg.User -> listOf(buildJsonObject { put("role", "user"); put("content", msg.text) })
        is Msg.AssistantText -> listOf(buildJsonObject { put("role", "assistant"); put("content", msg.text) })
        is Msg.AssistantCalls -> listOf(buildJsonObject {
            put("role", "assistant"); put("content", msg.text)
            putJsonArray("tool_calls") {
                msg.calls.forEach { call ->
                    add(buildJsonObject {
                        put("id", call.id); put("type", "function")
                        putJsonObject("function") { put("name", call.name); put("arguments", call.arguments.toString()) }
                    })
                }
            }
        })
        is Msg.ToolResults -> msg.results.map { (id, result) ->
            buildJsonObject { put("role", "tool"); put("tool_call_id", id); put("content", result) }
        }
    }

    companion object {
        fun fromEnvironment(http: HttpTransport = JdkHttpTransport()): MetaClient {
            val key = System.getenv("MODEL_API_KEY")?.takeIf { it.isNotBlank() }
                ?: throw LlmException("MODEL_API_KEY is not set (environment variables only; never commit keys)")
            val model = System.getenv("TRACE_LLM_MODEL") ?: "muse-wonder-1.0"
            return MetaClient(key, model, http) // constructor refuses non-Wonder models
        }
    }
}
