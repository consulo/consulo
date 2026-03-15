// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class EmptyResolver implements ValuesOrderResolver {
  @Override
  public Result resolve(TraceInfo info) {
    final Map<Integer, TraceElement> orderBefore = info.getValuesOrderBefore();
    final Map<Integer, TraceElement> orderAfter = info.getValuesOrderAfter();

    return Result.of(toEmptyMap(orderBefore), toEmptyMap(orderAfter));
  }

  private static Map<TraceElement, List<TraceElement>> toEmptyMap(Map<Integer, TraceElement> order) {
    return order.keySet().stream().collect(Collectors.toMap(order::get, x -> Collections.emptyList()));
  }
}
