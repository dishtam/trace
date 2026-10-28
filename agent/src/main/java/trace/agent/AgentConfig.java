package trace.agent;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Agent args: comma/semicolon separated key=value, e.g.
 * allow=trace.demoapp,org.example;out=trace-events.jsonl;capacity=65536;trigger=/tmp/trace.dump;config=/path/agent.properties
 * Keys: allow (required, comma-separated package prefixes), out, capacity (ring size, pow2),
 * trigger (file whose appearance requests a dump), redact (default true).
 */
public final class AgentConfig {
    public final List<String> allowPrefixes = new ArrayList<>();
    public Path out = Path.of("trace-events.jsonl");
    public int capacity = 65536;
    public Path trigger;
    public boolean redact = true;

    public static AgentConfig parse(String args) {
        AgentConfig config = new AgentConfig();
        Properties props = new Properties();
        if (args != null) {
            for (String part : args.split(";")) {
                int eq = part.indexOf('=');
                if (eq > 0) props.setProperty(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        if (props.containsKey("config")) {
            try (var in = new FileInputStream(props.getProperty("config"))) {
                Properties file = new Properties(); file.load(in);
                file.forEach((k, v) -> props.putIfAbsent(k, v));
            } catch (Throwable ignored) { /* fail-safe: keep inline args */ }
        }
        for (String p : props.getProperty("allow", "").split(",")) {
            if (!p.isBlank()) config.allowPrefixes.add(p.trim());
        }
        if (props.containsKey("out")) config.out = Path.of(props.getProperty("out"));
        if (props.containsKey("trigger")) config.trigger = Path.of(props.getProperty("trigger"));
        if (props.containsKey("capacity")) {
            try {
                int requested = Integer.parseInt(props.getProperty("capacity"));
                int pow2 = 16;
                while (pow2 < requested && pow2 < (1 << 24)) pow2 <<= 1;
                config.capacity = pow2;
            } catch (Throwable ignored) { /* keep default */ }
        }
        if (props.containsKey("redact")) config.redact = Boolean.parseBoolean(props.getProperty("redact"));
        return config;
    }
}
