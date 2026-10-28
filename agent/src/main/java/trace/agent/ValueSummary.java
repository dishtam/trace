package trace.agent;

/** Schema v0.1 Value (trace-schema.md). Summary is empty when unsafeToRender. */
public final class ValueSummary {
    public final String typeName;
    public final long identityHash;
    public final boolean hasIdentity;
    public final String summary;
    public final boolean redacted;
    public final boolean unsafeToRender;
    public final boolean truncated;

    public ValueSummary(String typeName, long identityHash, boolean hasIdentity, String summary,
                        boolean redacted, boolean unsafeToRender, boolean truncated) {
        this.typeName = typeName; this.identityHash = identityHash; this.hasIdentity = hasIdentity;
        this.summary = summary; this.redacted = redacted;
        this.unsafeToRender = unsafeToRender; this.truncated = truncated;
    }

    public String toJson() {
        StringBuilder b = new StringBuilder(96);
        b.append("{\"type_name\":").append(Json.quote(typeName));
        if (hasIdentity) b.append(",\"identity_hash\":").append(identityHash);
        b.append(",\"summary\":").append(Json.quote(summary));
        b.append(",\"redacted\":").append(redacted);
        b.append(",\"unsafe_to_render\":").append(unsafeToRender);
        b.append(",\"truncated\":").append(truncated).append('}');
        return b.toString();
    }
}
