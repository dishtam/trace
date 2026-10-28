package trace.agent;

/** One schema-v0.1 event as written to the event file (event_id/global_seq assigned by collector on ingest). */
public final class TraceEvent {
    public String runId, eventType, className, methodName, descriptor, payloadJson;
    public long seq, timestampNs, threadId;
    public Long invocationId, parentInvocationId;
    public Integer bci, sourceLine;
    public String sourceFile;

    public String toJsonLine() {
        StringBuilder b = new StringBuilder(256);
        b.append("{\"run_id\":").append(Json.quote(runId));
        b.append(",\"schema_version\":\"0.1\"");
        b.append(",\"event_type\":").append(Json.quote(eventType));
        b.append(",\"seq\":").append(seq);
        b.append(",\"timestamp_ns\":").append(timestampNs);
        b.append(",\"thread_id\":").append(threadId);
        if (invocationId != null) b.append(",\"invocation_id\":").append(invocationId);
        if (parentInvocationId != null) b.append(",\"parent_invocation_id\":").append(parentInvocationId);
        if (className != null) b.append(",\"class_name\":").append(Json.quote(className));
        if (methodName != null) b.append(",\"method_name\":").append(Json.quote(methodName));
        if (descriptor != null) b.append(",\"descriptor\":").append(Json.quote(descriptor));
        if (bci != null) b.append(",\"bci\":").append(bci);
        if (sourceFile != null) b.append(",\"source_file\":").append(Json.quote(sourceFile));
        if (sourceLine != null) b.append(",\"source_line\":").append(sourceLine);
        b.append(",\"payload\":").append(payloadJson == null ? "{}" : payloadJson);
        return b.append('}').toString();
    }
}
