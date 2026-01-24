// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.lib.InterpreterFactory;
import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.trace.impl.TraceElementImpl;
import consulo.execution.debug.stream.trace.impl.interpret.ValuesOrderInfo;
import consulo.execution.debug.stream.wrapper.StreamCall;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Vitaliy.Bibaev
 */
public class TraceResultInterpreterImpl implements TraceResultInterpreter {
    private static final Logger LOG = Logger.getInstance(TraceResultInterpreterImpl.class);
    private final InterpreterFactory myInterpreterFactory;

    public TraceResultInterpreterImpl(@Nonnull InterpreterFactory interpreterFactory) {
        myInterpreterFactory = interpreterFactory;
    }

    @Override
    public @Nonnull TracingResult interpret(@Nonnull StreamChain chain, @Nonnull ArrayReference resultArray, boolean isException) {
        ArrayReference info = (ArrayReference) resultArray.getValue(0);
        ArrayReference result = (ArrayReference) resultArray.getValue(1);
        Value streamResult = result.getValue(0);
        Value time = resultArray.getValue(2);
        logTime(time);
        List<TraceInfo> trace = getTrace(chain, info);
        return new TracingResultImpl(chain, TraceElementImpl.ofResultValue(streamResult), trace, isException);
    }

    private @Nonnull List<TraceInfo> getTrace(@Nonnull StreamChain chain, @Nonnull ArrayReference info) {
        int callCount = chain.length();
        List<TraceInfo> result = new ArrayList<>(callCount);

        for (int i = 0; i < callCount; i++) {
            StreamCall call = chain.getCall(i);
            Value trace = info.getValue(i);
            CallTraceInterpreter interpreter = myInterpreterFactory.getInterpreter(call.getName(), call.getType());

            TraceInfo traceInfo = trace == null ? ValuesOrderInfo.empty(call) : interpreter.resolve(call, trace);
            result.add(traceInfo);
        }

        return result;
    }

    private static void logTime(@Nonnull Value elapsedTimeArray) {
        Value elapsedTime = ((ArrayReference) elapsedTimeArray).getValue(0);
        long elapsedNanoseconds = ((LongValue) elapsedTime).value();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanoseconds);
        LOG.info("evaluation completed in " + elapsedMillis + "ms");
    }
}
