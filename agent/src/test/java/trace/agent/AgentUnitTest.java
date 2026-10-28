package trace.agent;

import org.junit.jupiter.api.Test;
import trace.fixture.FixtureMain;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AgentUnitTest {

    @Test void ringBufferDropsAndCountsOnOverflow() {
        RingBuffer<String> buffer = new RingBuffer<>(16);
        int accepted = 0;
        for (int i = 0; i < 100; i++) if (buffer.offer("e" + i)) accepted++;
        assertEquals(16, accepted);
        assertEquals(84, buffer.droppedCount());
        assertEquals("e0", buffer.poll());
    }

    @Test void ringBufferIsSafeUnderConcurrentProducers() throws Exception {
        RingBuffer<Integer> buffer = new RingBuffer<>(1024);
        AtomicInteger offered = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int t = 0; t < 4; t++) pool.submit(() -> {
            for (int i = 0; i < 250; i++) if (buffer.offer(i)) offered.incrementAndGet();
        });
        pool.shutdown(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        // poll() may return null while a producer has claimed but not yet published a slot; retry.
        int polled = 0; long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (polled < 1000 && System.nanoTime() < deadline) { if (buffer.poll() != null) polled++; }
        assertEquals(1000, offered.get());
        assertEquals(1000, polled);
        assertEquals(0, buffer.droppedCount());
    }

    @Test void toStringThrowingObjectIsNeverInvoked() {
        ValueSummary summary = ValueSummarizer.summarize(new FixtureMain.EvilToString());
        assertTrue(summary.unsafeToRender);
        assertEquals("", summary.summary);
        assertEquals(FixtureMain.EvilToString.class.getName(), summary.typeName);
    }

    @Test void lazyCollectionIsUnsafeToRender() {
        ValueSummary summary = ValueSummarizer.summarize(new FixtureMain.LazyList());
        assertTrue(summary.unsafeToRender);
        assertEquals("", summary.summary);
    }

    @Test void plainCollectionIsNotIterated() {
        ValueSummary summary = ValueSummarizer.summarize(List.of("a", "b"));
        assertTrue(summary.unsafeToRender); // rendering would iterate user-visible data
    }

    @Test void scalarsRenderAndTruncate() {
        assertEquals("42", ValueSummarizer.summarize(42).summary);
        ValueSummary longText = ValueSummarizer.summarize("x".repeat(500));
        assertTrue(longText.truncated);
        assertEquals(ValueSummarizer.MAX_SUMMARY_CHARS, longText.summary.length());
    }

    @Test void redactionByNameAndShape() {
        ValueSummary byName = ValueSummarizer.summarize("password", "hunter2");
        assertTrue(byName.redacted); assertEquals("", byName.summary);
        ValueSummary byShape = ValueSummarizer.summarize("contact is a@b.com ok");
        assertTrue(byShape.redacted); assertFalse(byShape.summary.contains("a@b.com"));
        assertTrue(Redactor.isSensitiveName("api_key"));
        assertNotNull(Redactor.redactValueShape("Bearer abc123"));
    }
}
