// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.trace.impl.TraceElementImpl;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedValueException;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedValueTypeException;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class CollectIdentityTraceInterpreter implements CallTraceInterpreter {
    private final SimplePeekCallTraceInterpreter myPeekResolver = new SimplePeekCallTraceInterpreter();

    @Override
    public @Nonnull TraceInfo resolve(@Nonnull StreamCall call, @Nonnull Value value) {
        if (!(value instanceof ArrayReference array)) {
            throw new UnexpectedValueTypeException("Array reference expected. But " + value.typeName() + " received");
        }

        TraceInfo resolved = myPeekResolver.resolve(call, array.getValue(0));
        Map<Integer, TraceElement> before = resolved.getValuesOrderBefore();
        if (before.isEmpty()) {
            return resolved;
        }

        int timeAfter = extractTime(array) + 1;

        Iterator<Integer> iterator = before.keySet().stream().sorted().toList().iterator();
        Map<Integer, TraceElement> after = new HashMap<>(before.size());
        while (iterator.hasNext()) {
            Integer timeBefore = iterator.next();

            TraceElement elementBefore = before.get(timeBefore);
            TraceElement elementAfter = new TraceElementImpl(timeAfter, elementBefore.getValue());

            after.put(timeAfter, elementAfter);
            ++timeAfter;
        }

        return new ValuesOrderInfo(call, before, after);
    }

    public static int extractTime(@Nonnull ArrayReference value) {
        Value timeArray = value.getValue(1);
        if (timeArray instanceof ArrayReference) {
            Value time = ((ArrayReference) timeArray).getValue(0);
            if (time instanceof IntegerValue) {
                return ((IntegerValue) time).value();
            }
        }

        throw new UnexpectedValueException("Could not find a maximum time value");
    }
}
