package trace.collector

import trace.store.SqliteTraceStore
import trace.store.StoredEvent
import java.io.File

data class IngestSummary(val runId: String, val events: Int, val dropped: Long, val complete: Boolean)

/**
 * ADR-0014 spike path: agent event file (JSONL, schema v0.1) -> SQLite via store.
 * The collector assigns event_id (DB) and global_seq (ingest order) here;
 * global_seq is explicitly NOT a cross-thread causality claim (trace-schema.md).
 */
object TraceIngest {
    private val STRING_FIELD = { name: String -> Regex("\"$name\":\"((?:[^\"\\\\]|\\\\.)*)\"") }
    private val LONG_FIELD = { name: String -> Regex("\"$name\":(-?\\d+)") }

    fun ingest(eventFile: File, dbFile: File, app: String? = null): IngestSummary {
        val lines = eventFile.readLines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "event file is empty: $eventFile" }
        val events = lines.map(::parseLine)
        val runId = stringField(lines.first(), "run_id") ?: error("first event has no run_id")
        var dropped = 0L
        var complete = false
        for (line in lines) {
            longField(line, "dropped_event_count")?.let { dropped = maxOf(dropped, it) }
            if (line.contains("\"marker_kind\":\"dump-end\"")) complete = true
        }
        dbFile.parentFile?.mkdirs()
        SqliteTraceStore(dbFile.absolutePath).use { store ->
            store.createRun(runId, schemaVersion = "0.1",
                startedAtNs = events.first().timestampNs, app = app, captureProfile = "summary")
            store.insertEvents(runId, events)
            store.finalizeRun(runId, droppedEvents = dropped, complete = complete)
        }
        return IngestSummary(runId, events.size, dropped, complete)
    }

    internal fun parseLine(line: String): StoredEvent {
        val payloadStart = line.indexOf("\"payload\":")
        val payload = if (payloadStart < 0) "{}" else line.substring(payloadStart + "\"payload\":".length).removeSuffix("}")
        return StoredEvent(
            eventType = stringField(line, "event_type") ?: error("missing event_type"),
            seq = longField(line, "seq") ?: 0,
            timestampNs = longField(line, "timestamp_ns") ?: 0,
            threadId = longField(line, "thread_id") ?: 0,
            invocationId = longField(line, "invocation_id"),
            parentInvocationId = longField(line, "parent_invocation_id"),
            className = stringField(line, "class_name"),
            methodName = stringField(line, "method_name"),
            descriptor = stringField(line, "descriptor"),
            bci = longField(line, "bci")?.toInt(),
            sourceFile = stringField(line, "source_file"),
            sourceLine = longField(line, "source_line")?.toInt(),
            payloadJson = payload.trim(),
        )
    }

    private fun stringField(line: String, name: String): String? =
        STRING_FIELD(name).find(line)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")?.replace("\\\\", "\\")

    private fun longField(line: String, name: String): Long? =
        LONG_FIELD(name).find(line)?.groupValues?.get(1)?.toLong()

    @JvmStatic fun main(args: Array<String>) {
        require(args.size >= 2) { "usage: collector <events.jsonl> <trace.db> [app]" }
        val summary = ingest(File(args[0]), File(args[1]), args.getOrNull(2))
        println("ingested run=${summary.runId} events=${summary.events} dropped=${summary.dropped} complete=${summary.complete}")
    }
}
