package trace.store

import java.sql.Connection
import java.sql.DriverManager

/** One schema-v0.1 event (trace-schema.md). eventId/globalSeq assigned on insert. */
data class StoredEvent(
    val eventType: String,
    val seq: Long,
    val timestampNs: Long,
    val threadId: Long,
    val invocationId: Long? = null,
    val parentInvocationId: Long? = null,
    val className: String? = null,
    val methodName: String? = null,
    val descriptor: String? = null,
    val bci: Int? = null,
    val sourceFile: String? = null,
    val sourceLine: Int? = null,
    val payloadJson: String = "{}",
)

data class RunInfo(
    val runId: String, val schemaVersion: String, val startedAtNs: Long?,
    val app: String?, val captureProfile: String?, val asyncCoverage: String?,
    val droppedEvents: Long, val complete: Boolean,
)

/**
 * SQLite store implementing the trace-schema.md v0.1 DDL exactly.
 * Completeness: v0.1 DDL has no explicit complete column, so finalisation records
 * dropped_events on the run and, for incomplete runs, async_coverage='partial' plus
 * a snapshot-marker reason. TODO(decide): explicit completeness column in v0.2
 * (via trace-event-schema skill) — do not diverge silently here.
 */
class SqliteTraceStore(dbPath: String) : AutoCloseable {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

    init {
        conn.createStatement().use { st ->
            DDL.forEach { st.execute(it) }
        }
    }

    fun createRun(runId: String, schemaVersion: String = "0.1", startedAtNs: Long? = null,
                  app: String? = null, captureProfile: String? = null, asyncCoverage: String? = null) {
        conn.prepareStatement(
            "INSERT INTO runs(run_id, schema_version, started_at_ns, app, capture_profile, async_coverage, dropped_events) VALUES (?,?,?,?,?,?,0)"
        ).use { ps ->
            ps.setString(1, runId); ps.setString(2, schemaVersion)
            if (startedAtNs == null) ps.setNull(3, java.sql.Types.INTEGER) else ps.setLong(3, startedAtNs)
            ps.setString(4, app); ps.setString(5, captureProfile); ps.setString(6, asyncCoverage)
            ps.executeUpdate()
        }
    }

    /** Inserts events in list order; global_seq follows that order (ingest order, not causality). */
    fun insertEvents(runId: String, events: List<StoredEvent>) {
        conn.autoCommit = false
        try {
            var globalSeq = currentGlobalSeq(runId)
            conn.prepareStatement(
                """INSERT INTO events(run_id, event_type, seq, global_seq, timestamp_ns, thread_id,
                   invocation_id, parent_invocation_id, class_name, method_name, descriptor, bci,
                   source_file, source_line, payload) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"""
            ).use { ps ->
                for (e in events) {
                    ps.setString(1, runId); ps.setString(2, e.eventType); ps.setLong(3, e.seq)
                    ps.setLong(4, ++globalSeq); ps.setLong(5, e.timestampNs); ps.setLong(6, e.threadId)
                    if (e.invocationId == null) ps.setNull(7, java.sql.Types.INTEGER) else ps.setLong(7, e.invocationId)
                    if (e.parentInvocationId == null) ps.setNull(8, java.sql.Types.INTEGER) else ps.setLong(8, e.parentInvocationId)
                    ps.setString(9, e.className); ps.setString(10, e.methodName); ps.setString(11, e.descriptor)
                    if (e.bci == null) ps.setNull(12, java.sql.Types.INTEGER) else ps.setInt(12, e.bci)
                    ps.setString(13, e.sourceFile)
                    if (e.sourceLine == null) ps.setNull(14, java.sql.Types.INTEGER) else ps.setInt(14, e.sourceLine)
                    ps.setString(15, e.payloadJson)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            conn.commit()
        } catch (t: Throwable) { conn.rollback(); throw t } finally { conn.autoCommit = true }
    }

    private fun currentGlobalSeq(runId: String): Long {
        conn.prepareStatement("SELECT COALESCE(MAX(global_seq),0) FROM events WHERE run_id=?").use { ps ->
            ps.setString(1, runId); ps.executeQuery().use { return if (it.next()) it.getLong(1) else 0 }
        }
    }

    fun finalizeRun(runId: String, droppedEvents: Long, complete: Boolean) {
        conn.prepareStatement("UPDATE runs SET dropped_events=? WHERE run_id=?").use { ps ->
            ps.setLong(1, droppedEvents); ps.setString(2, runId); ps.executeUpdate()
        }
        if (!complete) {
            conn.prepareStatement(
                "UPDATE runs SET async_coverage='partial' WHERE run_id=? AND async_coverage IS NULL"
            ).use { ps -> ps.setString(1, runId); ps.executeUpdate() }
            insertEvents(runId, listOf(StoredEvent(
                eventType = "snapshot-marker", seq = nextSeq(runId), timestampNs = System.currentTimeMillis() * 1_000_000,
                threadId = 0, payloadJson = """{"marker_kind":"drop-notice","dropped_event_count":$droppedEvents,"reason":"incomplete-run"}""")))
        }
    }

    private fun nextSeq(runId: String): Long {
        conn.prepareStatement("SELECT COALESCE(MAX(seq),0)+1 FROM events WHERE run_id=? AND thread_id=0").use { ps ->
            ps.setString(1, runId); ps.executeQuery().use { return if (it.next()) it.getLong(1) else 1 }
        }
    }

    fun eventsOf(runId: String, eventType: String? = null): List<Map<String, Any?>> {
        val sql = if (eventType == null) "SELECT * FROM events WHERE run_id=? ORDER BY global_seq"
                  else "SELECT * FROM events WHERE run_id=? AND event_type=? ORDER BY global_seq"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, runId); if (eventType != null) ps.setString(2, eventType)
            ps.executeQuery().use { rs ->
                val rows = mutableListOf<Map<String, Any?>>()
                val md = rs.metaData
                while (rs.next()) rows += (1..md.columnCount).associate { md.getColumnName(it) to rs.getObject(it) }
                return rows
            }
        }
    }

    fun runInfo(runId: String): RunInfo? {
        conn.prepareStatement("SELECT * FROM runs WHERE run_id=?").use { ps ->
            ps.setString(1, runId)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                val incompleteMarker = eventsOf(runId, "snapshot-marker")
                    .any { (it["payload"] as? String)?.contains("incomplete-run") == true }
                return RunInfo(rs.getString("run_id"), rs.getString("schema_version"),
                    rs.getObject("started_at_ns") as? Long, rs.getString("app"),
                    rs.getString("capture_profile"), rs.getString("async_coverage"),
                    rs.getLong("dropped_events"), complete = !incompleteMarker)
            }
        }
    }

    /** Read-only generic query for the graph layer (graph -> store, architecture.md). Params are bound, never interpolated. */
    fun jdbcQuery(sql: String, vararg params: Any?): List<Map<String, Any?>> {
        conn.prepareStatement(sql).use { ps ->
            params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
            ps.executeQuery().use { rs ->
                val rows = mutableListOf<Map<String, Any?>>()
                val md = rs.metaData
                while (rs.next()) rows += (1..md.columnCount).associate { md.getColumnName(it) to rs.getObject(it) }
                return rows
            }
        }
    }

    fun runExists(runId: String): Boolean =
        jdbcQuery("SELECT 1 AS x FROM runs WHERE run_id=?", runId).isNotEmpty()

    override fun close() = conn.close()

    companion object {
        // DDL copied exactly from docs/trace-schema.md (Storage mapping).
        val DDL = listOf(
            """CREATE TABLE IF NOT EXISTS runs (
                 run_id TEXT PRIMARY KEY, schema_version TEXT NOT NULL,
                 started_at_ns INTEGER, app TEXT, capture_profile TEXT,
                 async_coverage TEXT, dropped_events INTEGER NOT NULL DEFAULT 0
               )""",
            """CREATE TABLE IF NOT EXISTS events (
                 event_id INTEGER PRIMARY KEY, run_id TEXT NOT NULL REFERENCES runs(run_id),
                 event_type TEXT NOT NULL, seq INTEGER NOT NULL, global_seq INTEGER NOT NULL,
                 timestamp_ns INTEGER NOT NULL, thread_id INTEGER NOT NULL,
                 invocation_id INTEGER, parent_invocation_id INTEGER,
                 class_name TEXT, method_name TEXT, descriptor TEXT,
                 bci INTEGER, source_file TEXT, source_line INTEGER,
                 payload TEXT
               )""",
            "CREATE INDEX IF NOT EXISTS idx_events_run_seq ON events(run_id, thread_id, seq)",
            "CREATE INDEX IF NOT EXISTS idx_events_invocation ON events(run_id, invocation_id)",
            "CREATE INDEX IF NOT EXISTS idx_events_type ON events(run_id, event_type)",
        )
    }
}
