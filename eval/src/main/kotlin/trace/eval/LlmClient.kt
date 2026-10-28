package trace.eval

import kotlinx.serialization.json.*

/** Provider-neutral LLM surface (ADR-0010). Tool schemas are plain JSON Schema. */
data class ToolDef(val name: String, val description: String, val inputSchema: JsonObject)

data class ToolCallRef(val id: String, val name: String, val arguments: JsonObject)

data class Usage(val inputTokens: Long, val outputTokens: Long)

sealed interface Msg {
    data class User(val text: String) : Msg
    data class AssistantText(val text: String) : Msg
    data class AssistantCalls(val calls: List<ToolCallRef>, val text: String = "") : Msg
    data class ToolResults(val results: List<Pair<String, String>>) : Msg // call id -> result text
}

sealed interface LlmReply {
    val usage: Usage
    data class Text(val text: String, override val usage: Usage) : LlmReply
    data class Calls(val calls: List<ToolCallRef>, val text: String, override val usage: Usage) : LlmReply
}

interface LlmClient {
    val provider: String
    val model: String
    fun complete(system: String, messages: List<Msg>, tools: List<ToolDef>): LlmReply
}

/** Injectable HTTP so provider clients are contract-testable without network or keys. */
fun interface HttpTransport {
    fun post(url: String, headers: Map<String, String>, body: String): String
}

class JdkHttpTransport : HttpTransport {
    private val client = java.net.http.HttpClient.newHttpClient()
    override fun post(url: String, headers: Map<String, String>, body: String): String {
        val builder = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
        headers.forEach { (k, v) -> builder.header(k, v) }
        val response = client.send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw LlmException("HTTP ${response.statusCode()} from $url: ${response.body().take(400)}")
        }
        return response.body()
    }
}

class LlmException(message: String) : RuntimeException(message)

/** Verified prices (2026-10-28, official pages; see llm-strategy.md). USD per MTok. */
object Prices {
    private val table = mapOf(
        "claude-sonnet-5-5" to (2.0 to 10.0),
        "muse-wonder-1.0" to (4.0 to 20.0),
    )
    fun costUsd(model: String, usage: Usage): Double {
        val (input, output) = table[model] ?: throw LlmException("no verified price for model $model; refusing to guess")
        return usage.inputTokens / 1_000_000.0 * input + usage.outputTokens / 1_000_000.0 * output
    }
}

/** Hard per-investigation budget (owner decision 2026-10-28: $0.50 + 50 tool calls). */
class BudgetTracker(
    private val model: String,
    val maxCostUsd: Double = 0.50,
    val maxToolCalls: Int = 50,
) {
    var usage = Usage(0, 0); private set
    var toolCalls = 0; private set
    val costUsd: Double get() = Prices.costUsd(model, usage)
    fun record(replyUsage: Usage, callsMade: Int) {
        usage = Usage(usage.inputTokens + replyUsage.inputTokens, usage.outputTokens + replyUsage.outputTokens)
        toolCalls += callsMade
    }
    fun exceeded(): Boolean = costUsd > maxCostUsd || toolCalls > maxToolCalls
    fun summary(): String = "tokens_in=${usage.inputTokens} tokens_out=${usage.outputTokens} cost_usd=${"%.4f".format(costUsd)} tool_calls=$toolCalls"
}

internal fun JsonObject.str(name: String): String? = (this[name] as? JsonPrimitive)?.content
internal fun JsonObject.long(name: String): Long = (this[name] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0
internal fun parseArgs(raw: String): JsonObject =
    try { Json.parseToJsonElement(raw.ifBlank { "{}" }).jsonObject } catch (e: Exception) { buildJsonObject {} }
