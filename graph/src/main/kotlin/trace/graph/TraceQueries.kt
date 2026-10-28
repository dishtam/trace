package trace.graph

import trace.store.SqliteTraceStore

/** Citable reference to one stored event. `where` is class#method for display; IDs are the citation. */
data class EventRef(
    val eventId: Long, val kind: String, val invocationId: Long?,
    val className: String?, val methodName: String?, val depth: Int, val payload: String,
) {
    val where: String get() = "${className ?: "?"}#${methodName ?: "?"}"
}

data class FailurePoint(
    val eventId: Long, val invocationId: Long?, val exceptionType: String?,
    val exceptionMessage: String?, val className: String?, val methodName: String?,
    val frames: List<EventRef>,
)

data class Page<T>(val items: List<T>, val truncated: Boolean, val nextOffset: Int?)

data class SubtreeNode(
    val invocationId: Long, val parentInvocationId: Long?, val className: String?,
    val methodName: String?, val depth: Int, val outcome: String, val callEventId: Long,
)

/**
 * Graph query layer (architecture.md: graph -> store). All queries are bounded;
 * caps live in the MCP contract (llm-strategy.md) and are enforced here so no
 * caller can receive an unbounded result.
 *
 * trace_back derivation at schema v0.1 (trace-schema.md Graph derivation):
 * predecessors of an event are (a) the call event of its own invocation,
 * (b) the parent call event via parent_invocation_id, (c) for catch events the
 * caught throw, (d) for throw events its cause throw. assign/field-write events
 * join through rule (a) automatically once capture emits them.
 */
class TraceQueries(private val dbPath: String) {

    fun runExists(runId: String): Boolean = open { it.runExists(runId) }

    fun failurePoint(runId: String): FailurePoint? = open { store ->
        if (!store.runExists(runId)) return@open null
        // Uncaught = throw whose invocation never returned normally (agent v0 emits
        // no catch events, so this is the derived definition; see llm-strategy.md).
        val throws = store.jdbcQuery(
            """SELECT t.* FROM events t WHERE t.run_id=? AND t.event_type='throw'
               AND NOT EXISTS (SELECT 1 FROM events r WHERE r.run_id=t.run_id
                               AND r.event_type='return' AND r.invocation_id=t.invocation_id)
               ORDER BY t.global_seq DESC""", runId)
        // The latest failure is the investigation target, but its point of origin
        // is the earliest throw of the same exception instance: rethrows keep
        // exception_identity while propagating outward (trace-schema.md).
        val latest = throws.firstOrNull() ?: return@open null
        val identity = payloadField(latest["payload"] as? String, "exception_identity")
        val failure = if (identity == null) latest else throws
            .filter { payloadField(it["payload"] as? String, "exception_identity") == identity }
            .minByOrNull { (it["global_seq"] as Number).toLong() } ?: latest
        val invocationId = (failure["invocation_id"] as? Number)?.toLong()
        val frames = mutableListOf<EventRef>()
        var current = invocationId
        val seen = mutableSetOf<Long>()
        while (current != null && frames.size < MAX_FRAMES && seen.add(current)) {
            val call = store.jdbcQuery(
                "SELECT * FROM events WHERE run_id=? AND event_type='call' AND invocation_id=? LIMIT 1",
                runId, current).firstOrNull() ?: break
            frames += call.toRef(depth = frames.size)
            current = (call["parent_invocation_id"] as? Number)?.toLong()
        }
        FailurePoint(
            eventId = (failure["event_id"] as Number).toLong(),
            invocationId = invocationId,
            exceptionType = payloadField(failure["payload"] as? String, "exception_type"),
            exceptionMessage = payloadField(failure["payload"] as? String, "exception_message_summary"),
            className = failure["class_name"] as? String,
            methodName = failure["method_name"] as? String,
            frames = frames,
        )
    }

    /** One page of the backward slice. Returns null when the target event does not exist in the run. */
    fun traceBack(runId: String, targetEventId: Long, maxDepth: Int, offset: Int): Page<EventRef>? = open { store ->
        if (!store.runExists(runId)) return@open null
        val target = store.jdbcQuery("SELECT event_id FROM events WHERE run_id=? AND event_id=?", runId, targetEventId)
        if (target.isEmpty()) return@open null
        val rows = store.jdbcQuery(
            """WITH RECURSIVE slice(event_id, depth) AS (
                 SELECT ?, 0
                 UNION
                 SELECT p.event_id, s.depth + 1
                 FROM slice s
                 JOIN events cur ON cur.run_id=? AND cur.event_id = s.event_id
                 JOIN events p ON p.run_id = cur.run_id AND (
                      (cur.event_type = 'call' AND p.event_type = 'call' AND p.invocation_id = cur.parent_invocation_id)
                   OR (cur.event_type IN ('throw','return','catch','assign','field-write')
                       AND p.event_type = 'call' AND p.invocation_id = cur.invocation_id AND p.event_id <> cur.event_id)
                   OR (cur.event_type = 'catch' AND p.event_type = 'throw'
                       AND p.event_id = CAST(json_extract(cur.payload, '$.throw_event_id') AS INTEGER))
                   OR (cur.event_type = 'throw' AND p.event_type = 'throw'
                       AND p.event_id = CAST(json_extract(cur.payload, '$.cause_event_id') AS INTEGER))
                 )
                 WHERE s.depth < ?
               )
               SELECT e.*, s.depth AS slice_depth FROM slice s JOIN events e ON e.event_id = s.event_id
               ORDER BY s.depth ASC, e.global_seq ASC
               LIMIT ? OFFSET ?""",
            targetEventId, runId, maxDepth, TRACE_BACK_PAGE + 1, offset)
        val items = rows.take(TRACE_BACK_PAGE).map { it.toRef(depth = (it["slice_depth"] as Number).toInt()) }
        Page(items, truncated = rows.size > TRACE_BACK_PAGE,
            nextOffset = if (rows.size > TRACE_BACK_PAGE) offset + TRACE_BACK_PAGE else null)
    }

    /** One page (BFS order) of the call subtree under an invocation. Null when the invocation is unknown. */
    fun callSubtree(runId: String, invocationId: Long, maxDepth: Int, offset: Int): Page<SubtreeNode>? = open { store ->
        if (!store.runExists(runId)) return@open null
        val calls = store.jdbcQuery(
            "SELECT * FROM events WHERE run_id=? AND event_type='call' ORDER BY global_seq", runId)
        val byId = calls.associateBy { (it["invocation_id"] as Number).toLong() }
        if (invocationId !in byId) return@open null
        val outcomes = store.jdbcQuery(
            "SELECT invocation_id, event_type FROM events WHERE run_id=? AND event_type IN ('return','throw')", runId)
            .associate { ((it["invocation_id"] as Number).toLong()) to (it["event_type"] as String) }
        val children = calls.groupBy { (it["parent_invocation_id"] as? Number)?.toLong() }
        val ordered = mutableListOf<SubtreeNode>()
        val queue = ArrayDeque<Pair<Long, Int>>()
        queue.add(invocationId to 0)
        val seen = mutableSetOf<Long>()
        while (queue.isNotEmpty() && ordered.size <= offset + SUBTREE_PAGE) {
            val (id, depth) = queue.removeFirst()
            if (!seen.add(id)) continue
            val call = byId[id] ?: continue
            ordered += SubtreeNode(id, (call["parent_invocation_id"] as? Number)?.toLong(),
                call["class_name"] as? String, call["method_name"] as? String, depth,
                outcomes[id] ?: "unknown", (call["event_id"] as Number).toLong())
            if (depth < maxDepth) children[id]?.forEach { queue.add(((it["invocation_id"] as Number).toLong()) to depth + 1) }
        }
        val pageItems = ordered.drop(offset).take(SUBTREE_PAGE)
        Page(pageItems, truncated = ordered.size > offset + SUBTREE_PAGE,
            nextOffset = if (ordered.size > offset + SUBTREE_PAGE) offset + SUBTREE_PAGE else null)
    }

    fun eventExists(runId: String, eventId: Long): Boolean = open { store ->
        store.runExists(runId) && store.jdbcQuery(
            "SELECT 1 AS x FROM events WHERE run_id=? AND event_id=?", runId, eventId).isNotEmpty()
    }

    private fun <T> open(block: (SqliteTraceStore) -> T): T = SqliteTraceStore(dbPath).use(block)

    private fun Map<String, Any?>.toRef(depth: Int) = EventRef(
        eventId = (this["event_id"] as Number).toLong(), kind = this["event_type"] as String,
        invocationId = (this["invocation_id"] as? Number)?.toLong(), className = this["class_name"] as? String,
        methodName = this["method_name"] as? String, depth = depth,
        payload = this["payload"] as? String ?: "{}")

    companion object {
        const val MAX_FRAMES = 20      // llm-strategy.md Proposed cap
        const val TRACE_BACK_PAGE = 100 // llm-strategy.md Proposed cap
        const val SUBTREE_PAGE = 200    // llm-strategy.md Proposed cap

        private val FIELD_REGEX = Regex(""""([A-Za-z_]+)":\s*("(?:[^"\\]|\\.)*"|-?\d+|true|false)""")
        fun payloadField(payload: String?, field: String): String? {
            if (payload == null) return null
            val match = FIELD_REGEX.findAll(payload).firstOrNull { it.groupValues[1] == field } ?: return null
            val raw = match.groupValues[2]
            return if (raw.startsWith("\"")) raw.trim('"').replace("\\\"", "\"").replace("\\\\", "\\") else raw
        }
    }
}

