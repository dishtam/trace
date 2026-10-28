package trace.agent;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a captured value WITHOUT ever invoking user code:
 * no toString/equals/hashCode/getters/iterators on application objects
 * (instrumentation-safety rule 1). Only JDK-immutable scalars are rendered;
 * everything else is type + identity hash, flagged unsafe_to_render when
 * rendering could trigger behaviour (proxies, lazy collections, opaque objects).
 */
public final class ValueSummarizer {
    public static final int MAX_SUMMARY_CHARS = 256;   // trace-schema.md Proposed cap
    public static final int MAX_ELEMENTS = 8;          // trace-schema.md Proposed cap

    private ValueSummarizer() {}

    public static ValueSummary summarize(Object value) { return summarize(null, value); }

    public static ValueSummary summarize(String nameHint, Object value) {
        if (Redactor.isSensitiveName(nameHint)) {
            return new ValueSummary(typeName(value), identity(value), value != null, "", true, false, false);
        }
        if (value == null) return new ValueSummary("null", 0, false, "null", false, false, false);
        Class<?> type = value.getClass();
        String typeName = type.getName();
        long identity = identity(value);

        if (value instanceof String s) return text(typeName, identity, true, s);
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return new ValueSummary(typeName, identity, true, String.valueOf(value), false, false, false);
        }
        if (value instanceof Enum<?> e) {
            // Enum.name() is final in java.lang.Enum: no user code runs.
            return new ValueSummary(typeName, identity, true, e.name(), false, false, false);
        }
        if (value instanceof Class<?> c) return new ValueSummary(typeName, identity, true, c.getName(), false, false, false);
        if (value instanceof UUID) return text(typeName, identity, true, value.toString());
        if (type.getName().startsWith("java.time.")) return text(typeName, identity, true, value.toString());

        if (type.isArray()) {
            int length = Array.getLength(value);
            StringBuilder b = new StringBuilder("[").append(type.getComponentType().getName())
                    .append("; length=").append(length);
            int shown = Math.min(length, MAX_ELEMENTS);
            for (int i = 0; i < shown; i++) {
                Object element = Array.get(value, i); // field/array read: no user code
                b.append(i == 0 ? "; " : ", ");
                b.append(element == null || isScalar(element) ? scalarText(element) : element.getClass().getName());
            }
            return finish(typeName, identity, b.append(']').toString());
        }
        // Collections/maps and proxies: iterating or rendering can run user code
        // or trigger lazy loading. Type + identity only.
        if (value instanceof Collection<?> || value instanceof Map<?, ?> || isProxyLike(type)) {
            return new ValueSummary(typeName, identity, true, "", false, true, false);
        }
        // Opaque application object: never call toString(). Type + identity only.
        return new ValueSummary(typeName, identity, true, "", false, true, false);
    }

    private static boolean isProxyLike(Class<?> type) {
        String name = type.getName();
        return Proxy.isProxyClass(type) || name.contains("$Proxy") || name.contains("$$")
                || name.contains("HibernateProxy") || name.contains("CGLIB") || name.contains("ByteBuddy");
    }

    private static boolean isScalar(Object o) {
        return o instanceof String || o instanceof Number || o instanceof Boolean
                || o instanceof Character || o instanceof Enum<?>;
    }

    private static String scalarText(Object o) {
        if (o == null) return "null";
        if (o instanceof String s) return truncate(s);
        if (o instanceof Enum<?> e) return e.name();
        return String.valueOf(o);
    }

    private static ValueSummary text(String typeName, long identity, boolean hasIdentity, String raw) {
        String redacted = Redactor.redactValueShape(raw);
        if (redacted != null) return new ValueSummary(typeName, identity, hasIdentity, redacted, true, false, false);
        return finish(typeName, identity, raw);
    }

    private static ValueSummary finish(String typeName, long identity, String raw) {
        boolean truncated = raw.length() > MAX_SUMMARY_CHARS;
        return new ValueSummary(typeName, identity, true,
                truncated ? raw.substring(0, MAX_SUMMARY_CHARS) : raw, false, false, truncated);
    }

    static String truncate(String s) {
        return s.length() > MAX_SUMMARY_CHARS ? s.substring(0, MAX_SUMMARY_CHARS) : s;
    }

    private static String typeName(Object value) { return value == null ? "null" : value.getClass().getName(); }
    private static long identity(Object value) {
        // System.identityHashCode does not invoke user hashCode().
        return value == null ? 0 : System.identityHashCode(value) & 0xffffffffL;
    }
}
