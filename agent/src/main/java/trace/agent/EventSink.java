package trace.agent;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

/** Drains the ring buffer to the event file on a daemon thread; the app thread never does I/O. */
public final class EventSink {
    private final RingBuffer<TraceEvent> buffer;
    private final AgentConfig config;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile BufferedWriter writer;
    private volatile long lastTriggerSeen = -1;
    private Thread thread;

    public EventSink(RingBuffer<TraceEvent> buffer, AgentConfig config) { this.buffer = buffer; this.config = config; }

    public synchronized void start() {
        try {
            if (config.out.getParent() != null) Files.createDirectories(config.out.getParent());
            writer = Files.newBufferedWriter(config.out, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable t) { writer = null; /* fail-safe: drop silently, app unaffected */ }
        thread = new Thread(this::drainLoop, "trace-agent-writer");
        thread.setDaemon(true);
        thread.start();
    }

    private void drainLoop() {
        while (running.get()) {
            try {
                TraceEvent event;
                boolean wrote = false;
                while ((event = buffer.poll()) != null) { writeLine(event.toJsonLine()); wrote = true; }
                if (wrote && writer != null) writer.flush();
                checkTrigger();
                Thread.sleep(20);
            } catch (Throwable ignored) { /* never propagate into anything */ }
        }
        try {
            TraceEvent event;
            while ((event = buffer.poll()) != null) writeLine(event.toJsonLine());
            if (writer != null) writer.flush();
        } catch (Throwable ignored) {}
    }

    private void checkTrigger() {
        if (config.trigger == null) return;
        try {
            if (Files.exists(config.trigger)) {
                long stamp = Files.getLastModifiedTime(config.trigger).toMillis();
                if (stamp != lastTriggerSeen) { lastTriggerSeen = stamp; TraceRuntime.dumpNow("trigger-file"); }
            }
        } catch (Throwable ignored) {}
    }

    private synchronized void writeLine(String line) {
        if (writer == null) return;
        try { writer.write(line); writer.newLine(); } catch (Throwable ignored) {}
    }

    /** Force-flush everything currently buffered. Called on dump / shutdown. */
    public synchronized void flushNow() {
        try {
            TraceEvent event;
            while ((event = buffer.poll()) != null) writeLine(event.toJsonLine());
            if (writer != null) writer.flush();
        } catch (Throwable ignored) {}
    }

    public synchronized void close() {
        running.set(false);
        if (thread != null) { try { thread.join(2000); } catch (Throwable ignored) {} }
        try { if (writer != null) writer.close(); } catch (Throwable ignored) {}
    }

    public long droppedCount() { return buffer.droppedCount(); }
}
