package trace.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * Agent entry point. Uses ONLY JDK classes until the agent jar is appended to
 * the bootstrap classpath; all real work lives in AgentInstaller so that
 * Byte Buddy and TraceRuntime load exactly once, from the bootstrap loader,
 * visible to instrumented classes in every classloader (ADR-0013).
 */
public final class TraceAgent {
    private TraceAgent() {}

    public static void premain(String args, Instrumentation inst) { boot(args, inst); }
    public static void agentmain(String args, Instrumentation inst) { boot(args, inst); }

    private static void boot(String args, Instrumentation inst) {
        try {
            File jar = new File(TraceAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jar.isFile()) inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));
        } catch (Throwable ignored) { /* same-classloader use still works */ }
        AgentInstaller.install(args, inst);
    }
}
