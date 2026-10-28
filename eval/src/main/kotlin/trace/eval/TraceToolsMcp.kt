package trace.eval

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * TRACE tools through the real MCP server (stdio, Kotlin SDK client) — the
 * investigation only ever sees the four capped tools (ADR-0009), addressed by
 * run_id. The run_id is injected here, not chosen by the model beyond what the
 * starting prompt gives it.
 */
class TraceToolsMcp(
    private val dbPath: Path,
    private val sourceRoots: List<Path>,
    private val serverClasspath: String,
) : InvestigationTools {
    private var process: Process? = null
    private var client: Client? = null

    private fun connect(): Client {
        client?.let { return it }
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val log = Files.createTempFile("trace-mcp-server", ".log").toFile()
        val proc = ProcessBuilder(listOf(java, "-cp", serverClasspath, "trace.mcpserver.McpServerKt",
            dbPath.toString()) + sourceRoots.map { it.toString() })
            .redirectError(log).start()
        process = proc
        val transport = StdioClientTransport(
            proc.inputStream.asSource().buffered(), proc.outputStream.asSink().buffered(), null)
        val connected = Client(Implementation(name = "trace-eval-runner", version = "0.1.0"))
        try {
            runBlocking { connected.connect(transport) }
        } catch (e: Exception) {
            throw LlmException("MCP server connect failed: ${e.message}; server log: ${log.readText().take(500)}")
        }
        client = connected
        return connected
    }

    override fun defs(): List<ToolDef> {
        val connected = connect()
        val tools = runBlocking { connected.listTools().tools }
        return tools.map { ToolDef(it.name, it.description ?: "", it.inputSchema.properties?.let { props ->
            buildJsonObject { put("type", "object"); put("properties", props); it.inputSchema.required?.let { r -> putJsonArray("required") { r.forEach { req -> add(req) } } } }
        } ?: buildJsonObject { put("type", "object") }) }
    }

    override fun call(name: String, arguments: JsonObject): String {
        val connected = connect()
        val result = runBlocking {
            connected.callTool(name, arguments.mapValues { (_, v) -> v.toKotlinValue() })
        }
        val first = result.content.firstOrNull()
        val text = (first as? io.modelcontextprotocol.kotlin.sdk.types.TextContent)?.text
            ?: return "ERROR: empty tool response"
        return if (result.isError == true) "TOOL_ERROR: $text" else text
    }

    override fun close() {
        try { client?.let { runBlocking { it.close() } } } catch (e: Exception) { /* best effort */ }
        process?.destroyForcibly()
    }

    companion object {
        /** Classpath for the server subprocess: the eval runtime's own (JavaExec gives a real one). */
        fun currentRuntimeClasspath(): String =
            System.getProperty("java.class.path").split(File.pathSeparator)
                .joinToString(File.pathSeparator) { Path.of(it).toAbsolutePath().normalize().toString() }
    }
}

private fun JsonElement.toKotlinValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        content == "true" -> true
        content == "false" -> false
        else -> content.toLongOrNull() ?: content.toDoubleOrNull() ?: content
    }
    is JsonArray -> map { it.toKotlinValue() }
    is JsonObject -> mapValues { (_, v) -> v.toKotlinValue() }
}
