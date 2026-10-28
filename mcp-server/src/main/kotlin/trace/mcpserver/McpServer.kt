package trace.mcpserver

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import java.nio.file.Path

/**
 * TRACE MCP server (stdio). ADR-0009: the LLM sees only these capped tools.
 * Usage: McpServerKt <trace.db> [sourceRoot ...]
 * Source roots may also come from TRACE_SOURCE_ROOTS (path-separated env var).
 * Localhost auth/token is Phase 1 (roadmap M3), not S0-5.
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "usage: McpServerKt <trace.db> [sourceRoot ...]" }
    val dbPath = args[0]
    val roots = (args.drop(1).map { Path.of(it) } +
        (System.getenv("TRACE_SOURCE_ROOTS")?.split(System.getProperty("path.separator"))?.filter { it.isNotBlank() }
            ?.map { Path.of(it) } ?: emptyList())
        .ifEmpty { listOf(Path.of("demo-app/src/main/java")) })
    val tools = TraceTools(dbPath, roots)

    // stdout is the protocol channel. kotlin-logging prints a one-time init
    // banner to stdout when its factory initializes (triggered by Server
    // construction), so build the server with stdout shielded, then restore it
    // for the transport. All real logging goes to stderr via slf4j-simple.
    val protocolOut = System.out
    System.setOut(System.err)
    val server = try {
        Server(
            Implementation(name = "trace-mcp-server", version = "0.1.0"),
            ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false))),
        ).also { registerTools(it, tools) }
    } finally {
        System.setOut(protocolOut)
    }

    val closed = CompletableDeferred<Unit>()
    server.onClose { closed.complete(Unit) }
    runBlocking {
        val transport = StdioServerTransport(System.`in`.asSource().buffered(), protocolOut.asSink().buffered())
        server.createSession(transport)
        closed.await()
    }
}

private fun registerTools(server: Server, tools: TraceTools) {
    fun schema(properties: Map<String, JsonObject>, required: List<String>) =
        ToolSchema(properties = buildJsonObject { properties.forEach { (k, v) -> put(k, v) } }, required = required)
    fun prop(type: String, description: String) = buildJsonObject {
        put("type", type); put("description", description)
    }

    server.addTool(
        name = "get_failure_point",
        description = "What failed in a run: the uncaught exception, its location, and up to 20 call frames with citable IDs.",
        inputSchema = schema(mapOf("run_id" to prop("string", "run ID from a recorded run")), listOf("run_id")),
    ) { request -> respond(tools.call("get_failure_point", request.arguments())) }

    server.addTool(
        name = "trace_back",
        description = "Backward slice from an event: the events that produced it, in causal order, 100 per page. Use the cursor to page.",
        inputSchema = schema(mapOf(
            "run_id" to prop("string", "run ID"),
            "target_id" to prop("integer", "event_id from get_failure_point or a previous trace_back"),
            "max_depth" to prop("integer", "optional, 1..50, default 20"),
            "cursor" to prop("string", "optional pagination cursor from the previous response"),
        ), listOf("run_id", "target_id")),
    ) { request -> respond(tools.call("trace_back", request.arguments())) }

    server.addTool(
        name = "get_call_subtree",
        description = "Expand one invocation into its call subtree (invocation summaries), 200 nodes per page.",
        inputSchema = schema(mapOf(
            "run_id" to prop("string", "run ID"),
            "invocation_id" to prop("integer", "invocation_id from a previous tool response"),
            "depth" to prop("integer", "optional, 0..10, default 3"),
            "cursor" to prop("string", "optional pagination cursor from the previous response"),
        ), listOf("run_id", "invocation_id")),
    ) { request -> respond(tools.call("get_call_subtree", request.arguments())) }

    server.addTool(
        name = "get_source",
        description = "Source snippet (max 120 lines) for a class/method from a previous tool response.",
        inputSchema = schema(mapOf(
            "class_name" to prop("string", "fully-qualified class name"),
            "method_name" to prop("string", "optional method name"),
            "symbol_id" to prop("string", "optional class#method symbol from a previous response"),
        ), emptyList()),
    ) { request -> respond(tools.call("get_source", request.arguments())) }
}

private fun CallToolRequest.arguments(): JsonObject = params.arguments ?: buildJsonObject {}

private fun respond(outcome: ToolOutcome): CallToolResult =
    CallToolResult(content = listOf(TextContent(outcome.body.toString())), isError = outcome.isError)

