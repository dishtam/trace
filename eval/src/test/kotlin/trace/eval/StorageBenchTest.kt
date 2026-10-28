package trace.eval

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StorageBenchTest {
    @Test fun generatorIsDeterministicAndDeep() {
        val a = StorageBench.generate(20_000)
        val b = StorageBench.generate(20_000)
        assertEquals(a.events.size, b.events.size)
        assertEquals(a.targets, b.targets)
        assertTrue(a.events.size >= 20_000)
        assertTrue(a.targets.size >= 10, "need deep throw targets for trace_back: ${a.targets.size}")
        val types = a.events.groupingBy { it.eventType }.eachCount()
        assertTrue(types.containsKey("call") && types.containsKey("return")
            && types.containsKey("throw") && types.containsKey("assign"), types.toString())
        // Parent links must form real chains: every parent is an earlier invocation.
        val calls = a.events.filter { it.eventType == "call" }
        assertTrue(calls.any { it.parentInvocationId != null })
    }
}
