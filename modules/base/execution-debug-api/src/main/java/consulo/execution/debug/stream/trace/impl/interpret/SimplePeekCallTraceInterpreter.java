// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.trace.impl.TraceElementImpl;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedValueException;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public class SimplePeekCallTraceInterpreter implements CallTraceInterpreter {
  @Override
  public @Nonnull TraceInfo resolve(@Nonnull StreamCall call, @Nonnull Value value) {
    if (value instanceof ArrayReference trace) {
      Value before = trace.getValue(0);
      Value after = trace.getValue(1);
      if (before instanceof ArrayReference && after instanceof ArrayReference) {
        Map<Integer, TraceElement> beforeTrace = resolveTrace((ArrayReference)before);
        Map<Integer, TraceElement> afterTrace = resolveTrace((ArrayReference)after);
        return new ValuesOrderInfo(call, beforeTrace, afterTrace);
      }
    }

    throw new UnexpectedValueException("peek operation trace is wrong format");
  }

  protected static @Nonnull Map<Integer, TraceElement> resolveTrace(@Nonnull ArrayReference mapArray) {
    Value keys = mapArray.getValue(0);
    Value values = mapArray.getValue(1);
    if (keys instanceof ArrayReference && values instanceof ArrayReference) {
      return resolveTrace((ArrayReference)keys, (ArrayReference)values);
    }

    throw new UnexpectedValueException("keys and values must be stored in arrays in peek resolver");
  }

  private static @Nonnull Map<Integer, TraceElement> resolveTrace(@Nonnull ArrayReference keysArray, @Nonnull ArrayReference valuesArray) {
    LinkedHashMap<Integer, TraceElement> result = new LinkedHashMap<>();
    if (keysArray.length() == valuesArray.length()) {
      for (int i = 0, size = keysArray.length(); i < size; i++) {
        TraceElement element = resolveTraceElement(keysArray.getValue(i), valuesArray.getValue(i));
        result.put(element.getTime(), element);
      }

      return result;
    }

    throw new UnexpectedValueException("keys and values arrays should be with the same sizes");
  }

  private static @Nonnull TraceElement resolveTraceElement(@Nonnull Value key, @Nullable Value value) {
    if (key instanceof IntegerValue) {
      return new TraceElementImpl(((IntegerValue)key).value(), value);
    }

    throw new UnexpectedValueException("key must be an integer value");
  }
}
