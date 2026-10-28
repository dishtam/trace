package trace.graph

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import trace.store.SqliteTraceStore
import trace.store.StoredEvent
import java.nio.file.Files
import java.nio.file.Path

class TraceQueriesTest {

    /** Hand-built run: main(1) -> placeOrder(2) -> checkout(3) -> price(4, throws); placeOrder catches. */
    private fun buildRun(db: Path): String {
        val runId = "run-test"
        SqliteTraceStore(db.toString()).use { s ->
            s.createRun(runId)
            s.insertEvents(runId, listOf(
                StoredEvent("call", 1, 10, 1, invocationId = 1, className = "acme.Main", methodName = "main", descriptor = "()V"),
                StoredEvent("call", 2, 20, 1, invocationId = 2, parentInvocationId = 1, className = "acme.OrderService", methodName = "placeOrder", descriptor = "()V"),
                StoredEvent("call", 3, 30, 1, invocationId = 3, parentInvocationId = 2, className = "acme.Checkout", methodName = "checkout", descriptor = "()V"),
                StoredEvent("call", 4, 40, 1, invocationId = 4, parentInvocationId = 3, className = "acme.Pricing", methodName = "price", descriptor = "()V"),
                StoredEvent("throw", 5, 50, 1, invocationId = 4, className = "acme.Pricing", methodName = "price", descriptor = "()V",
                    payloadJson = """{"exception_type":"java.lang.NullPointerException","exception_message_summary":"x is null","exception_identity":9}"""),
                StoredEvent("catch", 6, 60, 1, invocationId = 2, className = "acme.OrderService", methodName = "placeOrder", descriptor = "()V",
                    payloadJson = """{"throw_event_id":5}"""),
                StoredEvent("return", 7, 70, 1, invocationId = 2),
                StoredEvent("return", 8, 80, 1, invocationId = 1),
            ))
            s.finalizeRun(runId, 0, true)
        }
        return runId
    }

    @Test fun failurePointFindsUncaughtAndFrames(@TempDir dir: Path) {
        val db = dir.resolve("t.db"); val runId = buildRun(db)
        // Make the throw uncaught for this variant: remove the catch by using a second run.
        val queries = TraceQueries(db.toString())
        val failure = queries.failurePoint(runId)
        assertNotNull(failure)
        // throw in invocation 4 has no return -> uncaught even though a catch event exists downstream.
        assertEquals("java.lang.NullPointerException", failure!!.exceptionType)
        assertEquals(listOf("price", "checkout", "placeOrder", "main"), failure.frames.map { it.methodName })
    }

    @Test fun failurePointResolvesOriginOfLatestPropagatedException(@TempDir dir: Path) {
        val db = dir.resolve("t.db")
        SqliteTraceStore(db.toString()).use { s ->
            s.createRun("prop")
            s.insertEvents("prop", listOf(
                StoredEvent("call", 1, 10, 1, invocationId = 1, className = "acme.Web", methodName = "handle", descriptor = "()V"),
                StoredEvent("call", 2, 20, 1, invocationId = 2, parentInvocationId = 1, className = "acme.Service", methodName = "work", descriptor = "()V"),
                StoredEvent("throw", 3, 30, 1, invocationId = 2, className = "acme.Service", methodName = "work", descriptor = "()V",
                    payloadJson = """{"exception_type":"java.lang.NullPointerException","exception_message_summary":"x","exception_identity":7}"""),
                StoredEvent("throw", 4, 40, 1, invocationId = 1, className = "acme.Web", methodName = "handle", descriptor = "()V",
                    payloadJson = """{"exception_type":"java.lang.NullPointerException","exception_message_summary":"x","exception_identity":7}"""),
            ))
            s.finalizeRun("prop", 0, true)
        }
        val failure = TraceQueries(db.toString()).failurePoint("prop")!!
        assertEquals("acme.Service", failure.className)
        assertEquals(listOf("work", "handle"), failure.frames.map { it.methodName })
    }

    @Test fun traceBackWalksCallAncestryFromThrow(@TempDir dir: Path) {
        val db = dir.resolve("t.db"); val runId = buildRun(db)
        val queries = TraceQueries(db.toString())
        val page = queries.traceBack(runId, targetEventId = 5, maxDepth = 20, offset = 0)!!
        assertEquals(listOf("throw", "call", "call", "call", "call"), page.items.map { it.kind })
        assertEquals(listOf("price", "price", "checkout", "placeOrder", "main"),
            page.items.map { it.methodName })
        assertFalse(page.truncated)
    }

    @Test fun traceBackFromCatchReachesTheThrow(@TempDir dir: Path) {
        val db = dir.resolve("t.db"); val runId = buildRun(db)
        val page = TraceQueries(db.toString()).traceBack(runId, targetEventId = 6, maxDepth = 20, offset = 0)!!
        assertTrue(page.items.any { it.kind == "throw" }, page.items.toString())
    }

    @Test fun unknownRunAndTarget(@TempDir dir: Path) {
        val db = dir.resolve("t.db"); buildRun(db)
        val queries = TraceQueries(db.toString())
        assertNull(queries.failurePoint("nope"))
        assertNull(queries.traceBack("run-test", 9999, 20, 0))
        assertNull(queries.callSubtree("run-test", 9999, 3, 0))
    }

    @Test fun callSubtreeIsBounded(@TempDir dir: Path) {
        val db = dir.resolve("t.db"); val runId = buildRun(db)
        val page = TraceQueries(db.toString()).callSubtree(runId, 1, maxDepth = 10, offset = 0)!!
        assertEquals(listOf(1L, 2L, 3L, 4L), page.items.map { it.invocationId })
        assertEquals("throw", page.items.last().outcome)
    }

    @Test fun traceBackPagination(@TempDir dir: Path) {
        val db = dir.resolve("t.db")
        SqliteTraceStore(db.toString()).use { s ->
            s.createRun("big")
            val events = mutableListOf<StoredEvent>()
            events += StoredEvent("call", 1, 10, 1, invocationId = 1, className = "a.A", methodName = "m1", descriptor = "()V")
            for (i in 2..150) events += StoredEvent("call", i.toLong(), i * 10L, 1,
                invocationId = i.toLong(), parentInvocationId = (i - 1).toLong(),
                className = "a.A", methodName = "m$i", descriptor = "()V")
            s.insertEvents("big", events)
            s.finalizeRun("big", 0, true)
        }
        val queries = TraceQueries(db.toString())
        val target = SqliteTraceStore(db.toString()).use { s ->
            (s.jdbcQuery("SELECT event_id FROM events WHERE run_id='big' AND invocation_id=150").single()["event_id"] as Number).toLong()
        }
        val page1 = queries.traceBack("big", target, maxDepth = 500, offset = 0)!!
        assertEquals(100, page1.items.size); assertTrue(page1.truncated)
        val page2 = queries.traceBack("big", target, maxDepth = 500, offset = page1.nextOffset!!)!!
        assertEquals(50, page2.items.size); assertFalse(page2.truncated)
    }

    @Test fun sourceIndexFindsMethodAndCaps(@TempDir dir: Path) {
        val pkg = dir.resolve("acme"); Files.createDirectories(pkg)
        val body = (1..200).joinToString("\n") { "  // line $it" }
        Files.writeString(pkg.resolve("OrderService.java"),
            "package acme;\npublic class OrderService {\n  public void placeOrder() {\n$body\n  }\n}\n")
        val index = SourceIndex(listOf(dir))
        val snippet = index.find("acme.OrderService", "placeOrder")!!
        assertEquals(3, snippet.line)
        assertTrue(snippet.truncated)
        assertTrue(snippet.snippet.lines().size <= 120)
        assertNull(index.find("acme.Missing", null))
        assertNull(index.find("../outside.Evil", null))
    }
}

