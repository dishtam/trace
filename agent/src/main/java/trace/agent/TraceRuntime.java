package trace.agent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entry points called from instrumented bytecode (via Advice). Every method is
 * guarded and non-throwing: a failure here must never reach the application
 * (instrumentation-safety rules 4 and 7).
 */
public final class TraceRuntime {
    private static volatile AgentConfig config;
    private static volatile RingBuffer<TraceEvent> buffer;
    private static volatile EventSink sink;
    private static volatile String runId;
    private static final AtomicLong nextInvocation = new AtomicLong(1);
    private static final ThreadLocal<Boolean> GUARD = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Deque<long[]>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<long[]> SEQ = ThreadLocal.withInitial(() -> new long[1]);
    private static final ThreadLocal<Boolean> THREAD_STARTED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TraceRuntime() {}

    public static synchronized void init(AgentConfig cfg, RingBuffer<TraceEvent> buf, EventSink eventSink, String id) {
        config = cfg; buffer = buf; sink = eventSink; runId = id;
    }

    public static boolean active() { return buffer != null && !GUARD.get(); }

    public static void onEnter(String className, String methodName, String descriptor, Object self, Object[] args) {
        if (!active()) return;
        GUARD.set(Boolean.TRUE);
        try {
            ensureThreadStart();
            long invocation = nextInvocation.getAndIncrement();
            Deque<long[]> stack = STACK.get();
            long parent = stack.isEmpty() ? 0 : stack.peek()[0];
            stack.push(new long[]{invocation, System.nanoTime()});
            TraceEvent event = base("call", invocation, parent == 0 ? null : parent);
            event.className = className; event.methodName = methodName; event.descriptor = descriptor;
            StringBuilder payload = new StringBuilder("{\"capture_level\":\"summary\"");
            if (self != null) payload.append(",\"this_summary\":").append(ValueSummarizer.summarize(self).toJson());
            payload.append(",\"arg_summaries\":[");
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) payload.append(',');
                    payload.append(ValueSummarizer.summarize(args[i]).toJson());
                }
            }
            event.payloadJson = payload.append("]}").toString();
            buffer.offer(event);
        } catch (Throwable ignored) {
        } finally { GUARD.set(Boolean.FALSE); }
    }

    public static void onReturn(Object returned, boolean isVoid) {
        if (!active()) return;
        GUARD.set(Boolean.TRUE);
        try {
            Deque<long[]> stack = STACK.get();
            if (stack.isEmpty()) return;
            long[] frame = stack.pop();
            TraceEvent event = base("return", frame[0], null);
            event.payloadJson = "{\"duration_ns\":" + (System.nanoTime() - frame[1])
                    + (isVoid ? "}" : ",\"return_summary\":" + ValueSummarizer.summarize(returned).toJson() + "}");
            buffer.offer(event);
        } catch (Throwable ignored) {
        } finally { GUARD.set(Boolean.FALSE); }
    }

    public static void onThrow(String className, String methodName, String descriptor, Throwable thrown) {
        if (!active()) return;
        GUARD.set(Boolean.TRUE);
        try {
            Deque<long[]> stack = STACK.get();
            Long invocation = stack.isEmpty() ? null : stack.pop()[0];
            TraceEvent event = base("throw", invocation, null);
            event.className = className; event.methodName = methodName; event.descriptor = descriptor;
            String message = "";
            try { message = thrown.getMessage() == null ? "" : thrown.getMessage(); } catch (Throwable ignored) {}
            String redacted = Redactor.redactValueShape(message);
            boolean wasRedacted = redacted != null;
            if (wasRedacted) message = redacted;
            if (message.length() > ValueSummarizer.MAX_SUMMARY_CHARS) {
                message = message.substring(0, ValueSummarizer.MAX_SUMMARY_CHARS);
            }
            event.payloadJson = "{\"exception_type\":" + Json.quote(thrown.getClass().getName())
                    + ",\"exception_message_summary\":" + Json.quote(message)
                    + ",\"exception_identity\":" + (System.identityHashCode(thrown) & 0xffffffffL)
                    + ",\"message_redacted\":" + wasRedacted + "}";
            buffer.offer(event);
        } catch (Throwable ignored) {
        } finally { GUARD.set(Boolean.FALSE); }
    }

    public static void dumpNow(String reason) {
        if (buffer == null) return;
        try {
            buffer.offer(marker("dump-start", reason, 0));
            if (sink != null) sink.flushNow();
            long dropped = sink == null ? 0 : sink.droppedCount();
            if (dropped > 0) buffer.offer(marker("drop-notice", reason, dropped));
            buffer.offer(marker("dump-end", reason, dropped));
            if (sink != null) sink.flushNow();
        } catch (Throwable ignored) {}
    }

    public static long droppedCount() { return sink == null ? 0 : sink.droppedCount(); }
    public static String runId() { return runId; }
    static String newRunId() { return UUID.randomUUID().toString(); }

    private static TraceEvent marker(String kind, String reason, long dropped) {
        TraceEvent event = base("snapshot-marker", null, null);
        event.payloadJson = "{\"marker_kind\":" + Json.quote(kind)
                + ",\"dropped_event_count\":" + dropped
                + ",\"reason\":" + Json.quote(reason == null ? "" : reason) + "}";
        return event;
    }

    private static void ensureThreadStart() {
        if (THREAD_STARTED.get()) return;
        THREAD_STARTED.set(Boolean.TRUE);
        Thread current = Thread.currentThread();
        TraceEvent event = base("thread-start", null, null);
        event.payloadJson = "{\"thread_name\":" + Json.quote(current.getName()) + "}";
        buffer.offer(event);
    }

    private static TraceEvent base(String type, Long invocation, Long parent) {
        TraceEvent event = new TraceEvent();
        event.runId = runId; event.eventType = type;
        event.seq = ++SEQ.get()[0];
        event.timestampNs = System.currentTimeMillis() * 1_000_000L; // wall clock for display (schema §Ordering)
        event.threadId = Thread.currentThread().getId();
        event.invocationId = invocation; event.parentInvocationId = parent;
        return event;
    }
}
