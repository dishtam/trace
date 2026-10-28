package trace.mcpserver

import kotlinx.serialization.json.*
import trace.graph.SourceIndex
import trace.graph.TraceQueries
import java.nio.file.Path
import java.util.Base64

/** Outcome of one tool call: the contract body plus whether it is a structured error. */
data class ToolOutcome(val body: JsonObject, val isError: Boolean)

/**
 * The four S0-5 tools (llm-strategy.md contracts, ADR-0009: capped summaries with
 * citable IDs, never the raw graph). Transport-independent: the MCP server in
 * McpServer.kt is a thin wrapper, and contract tests call this layer directly.
 *
 * Error codes: RUN_NOT_FOUND, FAILURE_NOT_FOUND, TARGET_NOT_FOUND,
 * INVOCATION_NOT_FOUND, SOURCE_NOT_FOUND, INVALID_ARGUMENT, UNAVAILABLE.
 */
class TraceTools(dbPath: String, sourceRoots: List<Path>) {
    private val queries = TraceQueries(dbPath)
    private val sources = SourceIndex(sourceRoots)

    fun call(tool: String, args: JsonObject): ToolOutcome = try {
        when (tool) {
            "get_failure_point" -> getFailurePoint(args)
            "trace_back" -> traceBack(args)
            "get_call_subtree" -> getCallSubtree(args)
            "get_source" -> getSource(args)
            else -> error("INVALID_ARGUMENT", "unknown tool: $tool", "call one of: get_failure_point, trace_back, get_call_subtree, get_source")
        }
    } catch (e: ToolException) {
        error(e.code, e.message ?: "", e.hint)
    } catch (e: Exception) {
        error("UNAVAILABLE", "tool failed: ${e.message}", "check the trace database path and retry")
    }

    private fun getFailurePoint(args: JsonObject): ToolOutcome {
        val runId = args.requiredString("run_id")
        if (!queries.runExists(runId)) throw ToolException("RUN_NOT_FOUND", "no run with id $runId", "use a run_id produced by a recorded run")
        val failure = queries.failurePoint(runId)
            ?: throw ToolException("FAILURE_NOT_FOUND", "run $runId has no uncaught exception", "the run may have completed normally; inspect it with get_call_subtree")
        val items = buildJsonArray {
            add(buildJsonObject {
                put("event_id", failure.eventId); put("kind", "throw")
                failure.invocationId?.let { put("invocation_id", it) }
                put("where", "${failure.className}#${failure.methodName}")
                put("exception_type", failure.exceptionType?.let { JsonPrimitive(it) } ?: JsonNull)
                put("exception_message", failure.exceptionMessage?.let { JsonPrimitive(it) } ?: JsonNull)
            })
            failure.frames.forEach { frame ->
                add(buildJsonObject {
                    put("event_id", frame.eventId); put("kind", frame.kind)
                    frame.invocationId?.let { put("invocation_id", it) }
                    put("where", frame.where); put("depth", frame.depth)
                })
            }
        }
        return ok("${failure.exceptionType ?: "exception"} at ${failure.className}#${failure.methodName}; ${failure.frames.size} frames", items, truncated = false, cursor = null)
    }

    private fun traceBack(args: JsonObject): ToolOutcome {
        val runId = args.requiredString("run_id")
        val targetId = args.requiredId("target_id")
        val maxDepth = args.optionalInt("max_depth", 20)
        if (maxDepth !in 1..MAX_DEPTH) throw ToolException("INVALID_ARGUMENT", "max_depth must be 1..$MAX_DEPTH", "narrow the target instead of deepening the slice")
        val offset = decodeCursor(args.optionalString("cursor"))
        if (!queries.runExists(runId)) throw ToolException("RUN_NOT_FOUND", "no run with id $runId", "use a run_id produced by a recorded run")
        val page = queries.traceBack(runId, targetId, maxDepth, offset)
            ?: throw ToolException("TARGET_NOT_FOUND", "no event $targetId in run $runId", "use an event_id returned by get_failure_point or trace_back")
        val items = buildJsonArray {
            page.items.forEach { item ->
                add(buildJsonObject {
                    put("event_id", item.eventId); put("kind", item.kind)
                    item.invocationId?.let { put("invocation_id", it) }
                    put("where", item.where); put("depth", item.depth)
                })
            }
        }
        return ok("${page.items.size} slice events from event $targetId (depth <= $maxDepth)", items,
            page.truncated, page.nextOffset?.let(::encodeCursor))
    }

    private fun getCallSubtree(args: JsonObject): ToolOutcome {
        val runId = args.requiredString("run_id")
        val invocationId = args.requiredId("invocation_id")
        val depth = args.optionalInt("depth", 3)
        if (depth !in 0..MAX_SUBTREE_DEPTH) throw ToolException("INVALID_ARGUMENT", "depth must be 0..$MAX_SUBTREE_DEPTH", "expand one level at a time")
        val offset = decodeCursor(args.optionalString("cursor"))
        if (!queries.runExists(runId)) throw ToolException("RUN_NOT_FOUND", "no run with id $runId", "use a run_id produced by a recorded run")
        val page = queries.callSubtree(runId, invocationId, depth, offset)
            ?: throw ToolException("INVOCATION_NOT_FOUND", "no invocation $invocationId in run $runId", "use an invocation_id returned by get_failure_point or trace_back")
        val items = buildJsonArray {
            page.items.forEach { node ->
                add(buildJsonObject {
                    put("invocation_id", node.invocationId)
                    node.parentInvocationId?.let { put("parent_invocation_id", it) }
                    put("call_event_id", node.callEventId)
                    put("where", "${node.className}#${node.methodName}")
                    put("depth", node.depth); put("outcome", node.outcome)
                })
            }
        }
        return ok("${page.items.size} invocations under $invocationId (depth <= $depth)", items,
            page.truncated, page.nextOffset?.let(::encodeCursor))
    }

    private fun getSource(args: JsonObject): ToolOutcome {
        val symbol = args.optionalString("symbol_id")
        val className = args.optionalString("class_name") ?: symbol?.substringBefore('#')
            ?: throw ToolException("INVALID_ARGUMENT", "get_source needs class_name or symbol_id", "pass a class+method from a previous tool response")
        val methodName = args.optionalString("method_name") ?: symbol?.substringAfter('#', "")?.ifBlank { null }
        val snippet = sources.find(className, methodName)
            ?: throw ToolException("SOURCE_NOT_FOUND", "no source for $className under the configured source roots", "check TRACE_SOURCE_ROOTS; bytecode location only for missing sources")
        val items = buildJsonArray {
            add(buildJsonObject {
                put("symbol_id", "$className#${methodName ?: ""}".trimEnd('#'))
                put("file", snippet.file); put("line", snippet.line)
                put("snippet", snippet.snippet)
            })
        }
        return ok("source for $className#${methodName ?: ""} at ${snippet.file}:${snippet.line}", items, snippet.truncated, null)
    }

    private fun ok(summary: String, items: JsonArray, truncated: Boolean, cursor: String?) = ToolOutcome(
        buildJsonObject {
            put("summary", summary); put("items", items); put("truncated", truncated)
            cursor?.let { put("cursor", it) }
        }, isError = false)

    private fun error(code: String, message: String, hint: String) = ToolOutcome(
        buildJsonObject { put("code", code); put("message", message); put("hint", hint) }, isError = true)

    private class ToolException(val code: String, message: String, val hint: String) : Exception(message)

    private fun JsonObject.requiredString(name: String): String =
        optionalString(name) ?: throw ToolException("INVALID_ARGUMENT", "missing required argument: $name", "arguments are IDs from prior tool responses")

    private fun JsonObject.optionalString(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.requiredId(name: String): Long {
        val prim = this[name] as? JsonPrimitive
            ?: throw ToolException("INVALID_ARGUMENT", "missing required argument: $name", "use an ID returned by a previous tool response")
        return prim.content.toLongOrNull()
            ?: throw ToolException("INVALID_ARGUMENT", "$name must be an integer ID", "use an ID returned by a previous tool response")
    }

    private fun JsonObject.optionalInt(name: String, default: Int): Int {
        val prim = this[name] as? JsonPrimitive ?: return default
        return prim.content.toIntOrNull()
            ?: throw ToolException("INVALID_ARGUMENT", "$name must be an integer", "omit it to use the default ($default)")
    }

    private fun encodeCursor(offset: Int): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("offset:$offset".toByteArray())

    private fun decodeCursor(cursor: String?): Int {
        if (cursor == null) return 0
        val decoded = try { String(Base64.getUrlDecoder().decode(cursor)) } catch (e: Exception) { null }
        return decoded?.removePrefix("offset:")?.toIntOrNull()?.takeIf { it >= 0 }
            ?: throw ToolException("INVALID_ARGUMENT", "invalid cursor", "pass the cursor from the previous response unchanged")
    }

    companion object {
        const val MAX_DEPTH = 50
        const val MAX_SUBTREE_DEPTH = 10
        val TOOL_NAMES = listOf("get_failure_point", "trace_back", "get_call_subtree", "get_source")
    }
}

