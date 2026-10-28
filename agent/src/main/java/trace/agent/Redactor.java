package trace.agent;

import java.util.regex.Pattern;

/** In-agent redaction (security-privacy.md), applied before any event leaves the process. */
public final class Redactor {
    static final String REDACTED = "[REDACTED]";
    private static final Pattern SENSITIVE_NAME =
            Pattern.compile("(?i)password|passwd|token|secret|api[_-]?key|auth|credential|session");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+\\S+");
    private static final Pattern LONG_DIGITS = Pattern.compile("\\d{13,19}");
    private static final Pattern KEY_PREFIX = Pattern.compile("(sk-[A-Za-z0-9_-]{8,}|AKIA[0-9A-Z]{12,})");

    private Redactor() {}

    public static boolean isSensitiveName(String name) {
        return name != null && SENSITIVE_NAME.matcher(name).find();
    }

    /** Returns the redacted form, or null when the value shape is not sensitive. */
    public static String redactValueShape(String value) {
        if (value == null) return null;
        if (EMAIL.matcher(value).find() || BEARER.matcher(value).find()
                || LONG_DIGITS.matcher(value).find() || KEY_PREFIX.matcher(value).find()) {
            return REDACTED;
        }
        return null;
    }
}
