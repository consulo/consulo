// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedArrayLengthException;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedValueException;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedValueTypeException;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class DistinctCallTraceInterpreter implements CallTraceInterpreter {
  private final CallTraceInterpreter myPeekResolver = new SimplePeekCallTraceInterpreter();

  private enum Direction {
    DIRECT, REVERSE
  }

  @Override
  public @Nonnull TraceInfo resolve(@Nonnull StreamCall call, @Nonnull Value value) {
    if (value instanceof ArrayReference) {
      final Value peekTrace = ((ArrayReference)value).getValue(0);
      final Value trace = ((ArrayReference)value).getValue(1);

      final TraceInfo order = myPeekResolver.resolve(call, peekTrace);

      final Map<TraceElement, List<TraceElement>> direct = resolve(trace, order, Direction.DIRECT);
      final Map<TraceElement, List<TraceElement>> reverse = resolve(trace, order, Direction.REVERSE);

      return new ValuesOrderInfo(order.getCall(), order.getValuesOrderBefore(), order.getValuesOrderAfter(), direct, reverse);
    }

    throw new UnexpectedValueException("distinct trace must be an array value");
  }

  private static @Nonnull Map<TraceElement, List<TraceElement>> resolve(@Nonnull Value value,
                                                                        @Nonnull TraceInfo order,
                                                                        @Nonnull Direction direction) {
    if (value instanceof ArrayReference convertedMap) {
      final Value keys = convertedMap.getValue(0);
      final Value values = convertedMap.getValue(1);
      if (keys instanceof ArrayReference keysArray && values instanceof ArrayReference valuesArray) {
        return Direction.DIRECT.equals(direction)
               ? resolveDirectTrace(keysArray, valuesArray, order)
               : resolveReverseTrace(keysArray, valuesArray, order);
      }

      throw new UnexpectedValueException("keys and values arrays must be arrays");
    }

    throw new UnexpectedValueException("value must be an array reference");
  }

  private static @Nonnull Map<TraceElement, List<TraceElement>> resolveDirectTrace(@Nonnull ArrayReference keys,
                                                                                   @Nonnull ArrayReference values,
                                                                                   @Nonnull TraceInfo order) {
    final int size = keys.length();
    if (size != values.length()) {
      throw new UnexpectedArrayLengthException("length of keys array should be same with values array");
    }

    final Map<TraceElement, List<TraceElement>> result = new HashMap<>();

    final Map<Integer, TraceElement> before = order.getValuesOrderBefore();
    final Map<Integer, TraceElement> after = order.getValuesOrderAfter();
    for (int i = 0; i < size; i++) {
      final int fromTime = extractIntValue(keys.getValue(i));
      final int afterTime = extractIntValue(values.getValue(i));
      TraceElement beforeElement = before.get(fromTime);
      TraceElement afterElement = after.get(afterTime);
      if (beforeElement != null && afterElement != null) {
        result.put(beforeElement, Collections.singletonList(afterElement));
      }
    }

    return result;
  }

  private static @Nonnull Map<TraceElement, List<TraceElement>> resolveReverseTrace(@Nonnull ArrayReference keys,
                                                                                    @Nonnull ArrayReference values,
                                                                                    @Nonnull TraceInfo order) {
    final int size = keys.length();
    if (size != values.length()) {
      throw new UnexpectedArrayLengthException("length of keys array should be same with values array");
    }

    final Map<TraceElement, List<TraceElement>> result = new HashMap<>();

    final Map<Integer, TraceElement> before = order.getValuesOrderBefore();
    final Map<Integer, TraceElement> after = order.getValuesOrderAfter();
    for (int i = 0; i < size; i++) {
      final int fromTime = extractIntValue(keys.getValue(i));
      final int afterTime = extractIntValue(values.getValue(i));
      final TraceElement beforeElement = before.get(fromTime);
      final TraceElement afterElement = after.get(afterTime);
      if (beforeElement != null && afterElement != null) {
        result.computeIfAbsent(afterElement, x -> new ArrayList<>()).add(beforeElement);
      }
    }

    return result;
  }

  private static int extractIntValue(@Nonnull Value value) {
    if (value instanceof IntegerValue) {
      return ((IntegerValue)value).value();
    }

    throw new UnexpectedValueTypeException("value should be IntegerValue, but actual is " + value.typeName());
  }
}
