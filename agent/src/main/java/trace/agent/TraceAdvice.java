package trace.agent;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/** Inlined into instrumented methods. References only TraceRuntime (bootstrap-visible) + JDK. */
public final class TraceAdvice {
    private TraceAdvice() {}

    @Advice.OnMethodEnter
    public static void enter(@Advice.Origin("#t") String type,
                             @Advice.Origin("#m") String method,
                             @Advice.Origin("#d") String descriptor,
                             @Advice.This(optional = true) Object self,
                             @Advice.AllArguments Object[] args) {
        TraceRuntime.onEnter(type, method, descriptor, self, args);
    }

    @Advice.OnMethodExit
    public static void exit(@Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returned,
                            @Advice.Origin("#d") String descriptor) {
        TraceRuntime.onReturn(returned, descriptor.endsWith(")V"));
    }
}

