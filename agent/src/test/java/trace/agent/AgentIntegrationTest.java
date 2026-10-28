package trace.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Runs FixtureMain in a subprocess with -javaagent and checks the event file (schema v0.1). */
class AgentIntegrationTest {

    private List<String> runAgent(Path out, String extraArgs, String... fixtureArgs) throws Exception {
        String jar = System.getProperty("trace.agentJar");
        assertNotNull(jar, "trace.agentJar system property (set by agent/build.gradle.kts)");
        String testClasses = new File(trace.fixture.FixtureMain.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsolutePath();
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = new java.util.ArrayList<>(List.of(java,
                "-javaagent:" + jar + "=allow=trace.fixture;out=" + out + (extraArgs.isEmpty() ? "" : ";" + extraArgs),
                "-cp", testClasses, "trace.fixture.FixtureMain"));
        command.addAll(List.of(fixtureArgs));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "fixture timed out: " + output);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("DONE"), "app output perturbed by agent: " + output);
        return Files.readAllLines(out);
    }

    @Test void recordsCallReturnThrowInOrder(@TempDir Path dir) throws Exception {
        List<String> lines = runAgent(dir.resolve("events.jsonl"), "");
        String all = String.join("\n", lines);
        assertTrue(all.contains("\"event_type\":\"thread-start\""), all);
        assertTrue(all.contains("\"method_name\":\"greet\""), all);
        assertTrue(all.contains("\"event_type\":\"throw\""), all);
        assertTrue(all.contains("IllegalStateException"), all);
        assertTrue(all.contains("\"event_type\":\"snapshot-marker\""), all);
        assertTrue(all.contains("dump-end"), all);
        int greetCall = all.indexOf("\"method_name\":\"greet\"");
        int greetReturn = all.indexOf("\"event_type\":\"return\"", greetCall);
        int failCall = all.indexOf("\"method_name\":\"fail\"");
        int throwEvent = all.indexOf("\"event_type\":\"throw\"");
        assertTrue(greetCall < greetReturn && greetReturn < failCall && failCall < throwEvent,
                "per-thread call/return/throw order wrong:\n" + all);
        // Safety: evil toString never rendered; lazy list unsafe; email-shaped value redacted in-agent
        assertTrue(all.contains("\"unsafe_to_render\":true"), all);
        assertTrue(all.contains("[REDACTED]"), all);
        assertFalse(all.contains("a@b.com"), "raw email must never reach the event file:\n" + all);
    }

    @Test void allowListRespected(@TempDir Path dir) throws Exception {
        List<String> lines = runAgent(dir.resolve("events.jsonl"), "");
        String all = String.join("\n", lines);
        assertFalse(all.contains("\"class_name\":\"java."), all);
        assertFalse(all.contains("\"class_name\":\"trace.agent"), all);
    }

    @Test void overflowDropsAndCountsNeverBlocks(@TempDir Path dir) throws Exception {
        List<String> lines = runAgent(dir.resolve("events.jsonl"), "capacity=64", "100000");
        String all = String.join("\n", lines);
        assertTrue(all.contains("drop-notice"), "overflow must be counted, not silent:\n" + all);
    }
}
