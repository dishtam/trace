package trace.agent;

import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/** Loaded after the bootstrap append (see TraceAgent); safe to touch Byte Buddy here.
 *  Public: TraceAgent lives in a different classloader, so package-private access would fail. */
public final class AgentInstaller {
    private AgentInstaller() {}

    private static byte[] bytesOf(Class<?> type) {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (var in = AgentInstaller.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("missing " + resource);
            return in.readAllBytes();
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    public static synchronized void install(String args, Instrumentation inst) {
        try {
            AgentConfig config = AgentConfig.parse(args);
            if (config.allowPrefixes.isEmpty()) return; // no allow-list = capture nothing (fail-safe)
            RingBuffer<TraceEvent> buffer = new RingBuffer<>(config.capacity);
            EventSink sink = new EventSink(buffer, config);
            TraceRuntime.init(config, buffer, sink, TraceRuntime.newRunId());
            sink.start();

            Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                TraceRuntime.dumpNow("uncaught-exception");
                if (previous != null) previous.uncaughtException(thread, error);
            });
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                TraceRuntime.dumpNow("shutdown");
                sink.close();
            }, "trace-agent-shutdown"));

            ElementMatcher.Junction<TypeDescription> allow = none();
            for (String prefix : config.allowPrefixes) allow = allow.or(nameStartsWith(prefix));
            ElementMatcher.Junction<TypeDescription> excluded = nameStartsWith("trace.agent.")
                    .or(nameStartsWith("trace.shaded."))
                    .or(nameStartsWith("java.")).or(nameStartsWith("jdk."))
                    .or(nameStartsWith("sun.")).or(nameStartsWith("com.sun."))
                    .or(nameContains("$$Lambda$"))
                    // Framework-generated proxies in app packages (Spring CGLIB, Hibernate):
                    // framework internals, excluded by default (instrumentation-safety rule 5).
                    .or(nameContains("$$SpringCGLIB")).or(nameContains("$$Hibernate"))
                    .or(nameContains("$Hibernate")).or(nameContains("$$FastClass"));
            var methods = isMethod().and(not(isSynthetic())).and(not(isBridge()))
                    .and(not(isAbstract())).and(not(isNative())).and(not(nameStartsWith("lambda$")));
            AgentBuilder builder = new AgentBuilder.Default();
            if (System.getProperty("trace.agent.debug") != null) {
                builder = builder.with(new AgentBuilder.Listener.Adapter() {
                    @Override public void onError(String name, ClassLoader cl,
                            net.bytebuddy.utility.JavaModule m, boolean loaded, Throwable t) {
                        System.err.println("[trace-agent] ERROR " + name + ": " + t);
                    }
                    @Override public void onTransformation(TypeDescription type, ClassLoader cl,
                            net.bytebuddy.utility.JavaModule m, boolean loaded,
                            net.bytebuddy.dynamic.DynamicType dt) {
                        System.err.println("[trace-agent] TRANSFORMED " + type.getName());
                    }
                });
            }
            ClassFileLocator adviceLocator = new ClassFileLocator.Compound(
                    ClassFileLocator.Simple.of("trace.agent.TraceAdvice", bytesOf(TraceAdvice.class)),
                    ClassFileLocator.Simple.of("trace.agent.TraceThrowAdvice", bytesOf(TraceThrowAdvice.class)),
                    ClassFileLocator.ForClassLoader.ofBootLoader());
            builder.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .ignore(nameStartsWith("net.bytebuddy.").or(nameStartsWith("trace.agent."))
                            .or(nameStartsWith("trace.shaded."))
                            .or(nameStartsWith("java.")).or(nameStartsWith("jdk."))
                            .or(nameStartsWith("sun.")).or(nameStartsWith("com.sun.")))
                    .type(allow.and(not(excluded)))
                    .transform((b, type, loader, module, domain) -> b
                            .visit(Advice.to(TraceAdvice.class, adviceLocator).on(methods))
                            .visit(Advice.to(TraceThrowAdvice.class, adviceLocator).on(methods)))
                    .installOn(inst);
        } catch (Throwable ignored) {
            // Fail-safe (rule 7): a broken agent must never break the application.
            if (System.getProperty("trace.agent.debug") != null) ignored.printStackTrace();
        }
    }
}

