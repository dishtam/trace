package trace.eval

import trace.store.SqliteTraceStore
import trace.store.StoredEvent
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.Random

/**
 * S0-8: SQLite vs DuckDB on a synthetic 1M-event run (ADR-0008).
 *
 * Method (documented in ADR-0008): one deterministic dataset (java.util.Random
 * seed 42) shaped like real v0.1 captures — call/return/throw/assign events over
 * 4 threads, invocation chains up to ~60 deep, payloads carrying the same JSON
 * value summaries the agent writes. Identical DDL (trace-schema.md) and the
 * identical trace_back recursive CTE (graph layer SQL) run on both engines.
 * Ingest = each engine's idiomatic bulk write with indexes already created:
 * SQLite through SqliteTraceStore itself (JDBC batches), DuckDB through its
 * JDBC Appender. Query timing:
 * 5 warmup rounds, then 100 timed executions cycling 20 deep throw targets,
 * each fetching its full result set; p95 over those 100.
 *
 * Usage: StorageBenchKt <workDir> [eventCount]
 */
object StorageBench {
    private const val RUN_ID = "bench-1m"

    data class Dataset(val events: List<StoredEvent>, val targets: List<Long>)

    fun generate(targetEvents: Int): Dataset {
        val random = Random(42)
        val events = mutableListOf<StoredEvent>()
        val stacks = Array(4) { mutableListOf<Long>() } // per-thread invocation stacks
        val depthOf = mutableMapOf<Long, Int>()
        var seq = 0L
        var invocation = 0L
        val throwTargets = mutableListOf<Long>()
        val classes = listOf("trace.demoapp.service.OrderService", "trace.demoapp.service.CheckoutService",
            "trace.demoapp.service.PricingService", "trace.demoapp.service.TaxService",
            "trace.demoapp.service.InventoryService", "trace.demoapp.web.OrderController")
        val methods = listOf("placeOrder", "checkout", "priceCents", "taxCents", "decrement", "place")

        fun emit(type: String, thread: Int, inv: Long?, parent: Long?, cls: String?, m: String?, payload: String): Long {
            events += StoredEvent(type, ++seq, 1_700_000_000_000_000_000L + seq * 1000, thread.toLong(),
                invocationId = inv, parentInvocationId = parent, className = cls, methodName = m,
                descriptor = "(Ljava/lang/String;I)V", payloadJson = payload)
            return events.size.toLong() // event_id as inserted (1-based rowid order)
        }
        for (t in 0..3) emit("thread-start", t + 1, null, null, null, null, """{"thread_name":"bench-$t"}""")

        // Recursive invocation tree: one chain child (deep ancestry for
        // trace_back) plus occasional side branches, like real call stacks.
        fun invoke(thread: Int, parent: Long?, depth: Int) {
            if (events.size >= targetEvents) return
            val id = ++invocation
            depthOf[id] = depth
            val cls = classes[(id % classes.size).toInt()]
            val method = methods[(id % methods.size).toInt()]
            emit("call", thread, id, parent, cls, method,
                """{"capture_level":"summary","arg_summaries":[{"type_name":"java.lang.String","summary":"SKU-${id % 97}","redacted":false,"unsafe_to_render":false,"truncated":false}],"this_summary":{"type_name":"$cls","identity_hash":${id * 31},"summary":"","redacted":false,"unsafe_to_render":true,"truncated":false}}""")
            if (id % 3 == 0L) emit("assign", thread, id, parent, cls, method,
                """{"var_index":1,"var_name":"total","value":{"type_name":"long","summary":"${id * 7}","redacted":false,"unsafe_to_render":false,"truncated":false}}""")
            if (depth < 70 && random.nextInt(100) < 90) invoke(thread, id, depth + 1)
            if (depth < 70 && random.nextInt(100) < 25) invoke(thread, id, depth + 1)
            if (random.nextInt(100) < 10) {
                val throwId = emit("throw", thread, id, parent, cls, method,
                    """{"exception_type":"java.lang.IllegalStateException","exception_message_summary":"synthetic failure $id","exception_identity":${id * 131}}""")
                if (depth >= 40) throwTargets += throwId
            } else {
                emit("return", thread, id, parent, null, null,
                    """{"duration_ns":${1000 + id % 5000},"return_summary":{"type_name":"long","summary":"${id * 7}","redacted":false,"unsafe_to_render":false,"truncated":false}}""")
            }
        }
        var thread = 0
        while (events.size < targetEvents) invoke(++thread % 4 + 1, null, 0)
        // Return/throw events carry no location in real captures either; keep class on exits for readability of results.
        val targets = throwTargets.filterIndexed { i, _ -> i % maxOf(1, throwTargets.size / 20) == 0 }.take(20)
        require(targets.size >= 10) { "synthetic data produced too few deep throw targets: ${targets.size}" }
        return Dataset(events, targets)
    }

    private val EDGES_TRACE_BACK_SQL = """
        WITH RECURSIVE slice(event_id, depth) AS (
          SELECT ?, 0
          UNION
          SELECT e.from_event, s.depth + 1
          FROM slice s JOIN edges e ON e.to_event = s.event_id
          WHERE s.depth < 1000
        )
        SELECT event_id, depth FROM slice ORDER BY depth ASC
    """.trimIndent()

    /** Predecessor edges exactly as the derived-edges CTE computes them (schema sketch: materialised edges). */
    fun buildEdges(events: List<StoredEvent>): List<Triple<Long, Long, String>> {
        val idOf = events.mapIndexed { index, e -> e to (index + 1L) }.toMap()
        val callByInvocation = events.filter { it.eventType == "call" && it.invocationId != null }
            .associateBy { it.invocationId }
        val edges = mutableListOf<Triple<Long, Long, String>>()
        for (e in events) {
            val to = idOf[e]!!
            when (e.eventType) {
                "call" -> e.parentInvocationId?.let { p ->
                    callByInvocation[p]?.let { edges += Triple(idOf[it]!!, to, "call") }
                }
                "throw", "return", "catch", "assign", "field-write" -> e.invocationId?.let { inv ->
                    callByInvocation[inv]?.let { if (idOf[it]!! != to) edges += Triple(idOf[it]!!, to, "call") }
                }
            }
            if (e.eventType == "catch") TraceQueriesPayload.longField(e.payloadJson, "throw_event_id")
                ?.let { edges += Triple(it, to, "exception-propagation") }
            if (e.eventType == "throw") TraceQueriesPayload.longField(e.payloadJson, "cause_event_id")
                ?.let { edges += Triple(it, to, "exception-propagation") }
        }
        return edges
    }

    object TraceQueriesPayload {
        fun longField(payload: String, field: String): Long? =
            Regex(""""$field":\s*(-?\d+)""").find(payload)?.groupValues?.get(1)?.toLong()
    }

    private val TRACE_BACK_SQL = """
        WITH RECURSIVE slice(event_id, depth) AS (
          SELECT ?, 0
          UNION
          SELECT p.event_id, s.depth + 1
          FROM slice s
          JOIN events cur ON cur.run_id = ? AND cur.event_id = s.event_id
          JOIN events p ON p.run_id = cur.run_id AND (
               (cur.event_type = 'call' AND p.event_type = 'call' AND p.invocation_id = cur.parent_invocation_id)
            OR (cur.event_type IN ('throw','return','catch','assign','field-write')
                AND p.event_type = 'call' AND p.invocation_id = cur.invocation_id AND p.event_id <> cur.event_id)
            OR (cur.event_type = 'catch' AND p.event_type = 'throw'
                AND p.event_id = CAST(json_extract(cur.payload, '$.throw_event_id') AS INTEGER))
            OR (cur.event_type = 'throw' AND p.event_type = 'throw'
                AND p.event_id = CAST(json_extract(cur.payload, '$.cause_event_id') AS INTEGER))
          )
          WHERE s.depth < 1000
        )
        SELECT e.event_id, s.depth FROM slice s JOIN events e ON e.event_id = s.event_id
        ORDER BY s.depth ASC, e.global_seq ASC
    """.trimIndent()

    @JvmStatic fun main(args: Array<String>) {
        val workDir = Path.of(args.getOrElse(0) { "eval/results/storage-bench" })
        val targetEvents = args.getOrNull(1)?.toInt() ?: 1_000_000
        Files.createDirectories(workDir)
        println("generating $targetEvents synthetic events (seed 42)...")
        var t0 = System.nanoTime()
        val data = generate(targetEvents)
        val genMs = (System.nanoTime() - t0) / 1_000_000
        println("generated ${data.events.size} events, ${data.targets.size} deep targets in ${genMs} ms")

        // --- SQLite through the real store writer ---
        val sqliteDb = workDir.resolve("bench-sqlite.db")
        Files.deleteIfExists(sqliteDb)
        t0 = System.nanoTime()
        SqliteTraceStore(sqliteDb.toString()).use { store ->
            store.createRun(RUN_ID, app = "storage-bench", captureProfile = "summary")
            data.events.chunked(10_000).forEach { store.insertEvents(RUN_ID, it) }
            store.finalizeRun(RUN_ID, 0, true)
        }
        val sqliteIngestMs = (System.nanoTime() - t0) / 1_000_000
        val sqliteSizeMb = Files.size(sqliteDb) / 1_000_000.0

        // --- DuckDB through JDBC batch inserts, same DDL ---
        val duckDb = workDir.resolve("bench-duck.db")
        Files.deleteIfExists(duckDb); Files.deleteIfExists(Path.of("$duckDb.wal"))
        Class.forName("org.duckdb.DuckDBDriver")
        t0 = System.nanoTime()
        DriverManager.getConnection("jdbc:duckdb:$duckDb").use { conn ->
            // Dialect delta (trace-schema.md storage notes): DuckDB INTEGER is
            // 32-bit, SQLite INTEGER has 64-bit affinity, so int64 columns map
            // to BIGINT on DuckDB. Same tables, indexes, and semantics.
            val duckDdl = SqliteTraceStore.DDL.map { it.replace("INTEGER", "BIGINT") }
            conn.createStatement().use { st -> duckDdl.forEach { st.execute(it) } }
            conn.autoCommit = false
            conn.prepareStatement("INSERT INTO runs(run_id, schema_version, started_at_ns, app, capture_profile, async_coverage, dropped_events) VALUES (?,?,?,?,?,?,0)").use { ps ->
                ps.setString(1, RUN_ID); ps.setString(2, "0.1"); ps.setLong(3, 1_700_000_000_000_000_000L)
                ps.setString(4, "storage-bench"); ps.setString(5, "summary"); ps.setString(6, null); ps.executeUpdate()
            }
            // DuckDB ingest uses its engine-idiomatic bulk path, the JDBC
            // Appender (plain JDBC batches are pathologically slow on DuckDB;
            // measured and rejected during this benchmark — see ADR-0008).
            // event_id is written explicitly: DuckDB has no SQLite-style
            // rowid autoincrement on INTEGER PRIMARY KEY. Identical dataset.
            conn.autoCommit = true // the Appender manages its own transactions
            val duckConn = conn as org.duckdb.DuckDBConnection
            duckConn.createAppender("main", "events").use { appender ->
                var globalSeq = 0L
                data.events.forEachIndexed { index, e ->
                    appender.beginRow()
                    appender.append(index + 1L)
                    appender.append(RUN_ID); appender.append(e.eventType); appender.append(e.seq)
                    appender.append(++globalSeq); appender.append(e.timestampNs); appender.append(e.threadId)
                    if (e.invocationId == null) appender.appendNull() else appender.append(e.invocationId!!)
                    if (e.parentInvocationId == null) appender.appendNull() else appender.append(e.parentInvocationId!!)
                    if (e.className == null) appender.appendNull() else appender.append(e.className)
                    if (e.methodName == null) appender.appendNull() else appender.append(e.methodName)
                    if (e.descriptor == null) appender.appendNull() else appender.append(e.descriptor)
                    appender.appendNull()
                    if (e.sourceFile == null) appender.appendNull() else appender.append(e.sourceFile)
                    appender.appendNull()
                    appender.append(e.payloadJson)
                    appender.endRow()
                    if (index % 50_000 == 49_999) appender.flush()
                }
                appender.flush()
            }
            conn.prepareStatement("UPDATE runs SET dropped_events=0 WHERE run_id=?").use { ps ->
                ps.setString(1, RUN_ID); ps.executeUpdate()
            }
        }
        val duckIngestMs = (System.nanoTime() - t0) / 1_000_000
        val duckSizeMb = Files.size(duckDb) / 1_000_000.0

        // Materialised edges (schema sketch) built identically for both engines.
        val edges = buildEdges(data.events)
        fun addEdgesSqlite() {
            DriverManager.getConnection("jdbc:sqlite:$sqliteDb").use { conn ->
                conn.createStatement().use { st ->
                    st.execute("CREATE TABLE IF NOT EXISTS edges(from_event BIGINT, to_event BIGINT, kind TEXT)")
                    st.execute("CREATE INDEX IF NOT EXISTS idx_edges_to ON edges(to_event)")
                }
                conn.autoCommit = false
                conn.prepareStatement("INSERT INTO edges(from_event, to_event, kind) VALUES (?,?,?)").use { ps ->
                    edges.forEachIndexed { i, (from, to, kind) ->
                        ps.setLong(1, from); ps.setLong(2, to); ps.setString(3, kind); ps.addBatch()
                        if (i % 5000 == 4999) ps.executeBatch()
                    }
                    ps.executeBatch()
                }
                conn.commit()
            }
        }
        fun addEdgesDuck() {
            DriverManager.getConnection("jdbc:duckdb:$duckDb").use { conn ->
                conn.createStatement().use { st ->
                    st.execute("CREATE TABLE IF NOT EXISTS edges(from_event BIGINT, to_event BIGINT, kind VARCHAR)")
                    st.execute("CREATE INDEX IF NOT EXISTS idx_edges_to ON edges(to_event)")
                }
                val duckConn = conn as org.duckdb.DuckDBConnection
                duckConn.createAppender("main", "edges").use { appender ->
                    edges.forEach { (from, to, kind) ->
                        appender.beginRow(); appender.append(from); appender.append(to); appender.append(kind); appender.endRow()
                    }
                    appender.flush()
                }
            }
        }
        var tEdges = System.nanoTime(); addEdgesSqlite()
        val sqliteEdgesMs = (System.nanoTime() - tEdges) / 1_000_000
        tEdges = System.nanoTime(); addEdgesDuck()
        val duckEdgesMs = (System.nanoTime() - tEdges) / 1_000_000
        println("RESULT edges=${edges.size} edges_build_ms sqlite=$sqliteEdgesMs duckdb=$duckEdgesMs")

        fun bench(engine: String, shape: String, sql: String, reps: Int, bind: (java.sql.PreparedStatement, Long) -> Unit) {
            val db = if (engine == "sqlite") sqliteDb else duckDb
            val url = if (engine == "sqlite") "jdbc:sqlite:$db" else "jdbc:duckdb:$db"
            DriverManager.getConnection(url).use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    fun runOnce(target: Long): Pair<Long, Int> {
                        bind(ps, target)
                        val start = System.nanoTime()
                        var rows = 0
                        ps.executeQuery().use { rs -> while (rs.next()) rows++ }
                        return (System.nanoTime() - start) / 1_000_000 to rows
                    }
                    repeat(if (reps <= 3) 1 else 5) { runOnce(data.targets[it % data.targets.size]) } // warmup
                    val times = mutableListOf<Long>()
                    var rowsSeen = 0
                    repeat(reps) { runOnce(data.targets[it % data.targets.size]).also { times += it.first; rowsSeen = it.second } }
                    times.sort()
                    val p50 = times[times.size / 2]; val p95 = times[(times.size * 95 / 100).coerceAtMost(times.size - 1)]
                    println("RESULT engine=$engine shape=$shape reps=$reps p50_ms=$p50 p95_ms=$p95 max_ms=${times.last()} slice_rows_example=$rowsSeen")
                }
            }
        }
        // Parity: both shapes must return the same slice for the same target.
        fun countRows(url: String, sql: String, derived: Boolean, target: Long): Int =
            DriverManager.getConnection(url).use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setLong(1, target); if (derived) ps.setString(2, RUN_ID)
                    ps.executeQuery().use { rs -> var n = 0; while (rs.next()) n++; n }
                }
            }
        for (engine in listOf("sqlite", "duckdb")) {
            val url = if (engine == "sqlite") "jdbc:sqlite:$sqliteDb" else "jdbc:duckdb:$duckDb"
            val a = countRows(url, TRACE_BACK_SQL, true, data.targets.first())
            val b = countRows(url, EDGES_TRACE_BACK_SQL, false, data.targets.first())
            println("RESULT parity engine=$engine derived_rows=$a edges_rows=$b equal=${a == b}")
            check(a == b) { "slice parity failed for $engine: derived=$a edges=$b" }
        }
        // Derived-edges shape is the shipped TraceQueries SQL; it is scan-bound
        // (see ADR-0008). At 1M events one SQLite execution takes ~1-2 minutes,
        // so it gets 3 reps (DuckDB's parallel scan allows 10); the edges shape
        // gets the full 100. Rep counts are printed with every result.
        bench("sqlite", "derived", TRACE_BACK_SQL, 3) { ps, t -> ps.setLong(1, t); ps.setString(2, RUN_ID) }
        bench("duckdb", "derived", TRACE_BACK_SQL, 10) { ps, t -> ps.setLong(1, t); ps.setString(2, RUN_ID) }
        bench("sqlite", "edges", EDGES_TRACE_BACK_SQL, 100) { ps, t -> ps.setLong(1, t) }
        bench("duckdb", "edges", EDGES_TRACE_BACK_SQL, 100) { ps, t -> ps.setLong(1, t) }
        println("RESULT ingest_ms sqlite=$sqliteIngestMs duckdb=$duckIngestMs")
        println("RESULT db_size_mb sqlite=${"%.1f".format(sqliteSizeMb)} duckdb=${"%.1f".format(duckSizeMb)}")
        println("RESULT events=${data.events.size} targets=${data.targets.size}")
    }
}
