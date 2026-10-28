package trace.agent;

import net.bytebuddy.asm.Advice;

/** Separate advice class: Byte Buddy requires one exit advice per throwable mode. */
public final class TraceThrowAdvice {
    private TraceThrowAdvice() {}

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Origin("#t") String type,
                            @Advice.Origin("#m") String method,
                            @Advice.Origin("#d") String descriptor,
                            @Advice.Thrown Throwable thrown) {
        if (thrown != null) TraceRuntime.onThrow(type, method, descriptor, thrown);
    }
}

