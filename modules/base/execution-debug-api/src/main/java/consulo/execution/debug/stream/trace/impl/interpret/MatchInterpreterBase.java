// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.trace.impl.TraceElementImpl;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedArrayLengthException;
import consulo.execution.debug.stream.trace.impl.interpret.ex.UnexpectedValueTypeException;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class MatchInterpreterBase implements CallTraceInterpreter {
  private final CallTraceInterpreter myPeekResolver = new SimplePeekCallTraceInterpreter();

  @Override
  public @Nonnull TraceInfo resolve(@Nonnull StreamCall call, @Nonnull Value value) {
    if (value instanceof ArrayReference array) {
      if (array.length() != 2) {
        throw new UnexpectedArrayLengthException("trace array for *match call should contain two items. Actual = " + array.length());
      }

      final Value peeksResult = array.getValue(0);
      final Value streamResult = array.getValue(1);
      final TraceElement streamResultElement = TraceElementImpl.ofResultValue(((ArrayReference)streamResult).getValue(0));

      final TraceInfo peeksInfo = myPeekResolver.resolve(call, peeksResult);

      final Collection<TraceElement> traceBeforeFilter = peeksInfo.getValuesOrderBefore().values();
      final Map<Integer, TraceElement> traceAfter = peeksInfo.getValuesOrderAfter();
      final Collection<TraceElement> traceAfterFilter = traceAfter.values();

      final boolean result = getResult(traceBeforeFilter, traceAfterFilter);
      final Action action = getAction(result);

      final Map<Integer, TraceElement> beforeTrace =
        Action.CONNECT_FILTERED.equals(action) ? onlyFiltered(traceAfterFilter) : difference(traceBeforeFilter, traceAfter.keySet());

      return new ValuesOrderInfo(call, beforeTrace, makeIndexByTime(Stream.of(streamResultElement)));
    }

    throw new UnexpectedValueTypeException("value should be array reference, but given " + value.typeName());
  }

  protected abstract boolean getResult(@Nonnull Collection<TraceElement> traceBeforeFilter,
                                       @Nonnull Collection<TraceElement> traceAfterFilter);

  protected abstract @Nonnull Action getAction(boolean result);

  protected enum Action {
    CONNECT_FILTERED, CONNECT_DIFFERENCE
  }

  private static @Nonnull Map<Integer, TraceElement> onlyFiltered(@Nonnull Collection<TraceElement> afterFilter) {
    return makeIndexByTime(afterFilter.stream());
  }

  private static @Nonnull Map<Integer, TraceElement> difference(@Nonnull Collection<TraceElement> before, @Nonnull Set<Integer> timesAfter) {
    return makeIndexByTime(before.stream().filter(x -> !timesAfter.contains(x.getTime())));
  }

  private static @Nonnull Map<Integer, TraceElement> makeIndexByTime(@Nonnull Stream<TraceElement> elementStream) {
    return elementStream.collect(Collectors.toMap(TraceElement::getTime, Function.identity()));
  }
}
