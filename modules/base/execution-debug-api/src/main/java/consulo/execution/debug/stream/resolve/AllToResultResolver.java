// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.TraceInfo;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Vitaliy.Bibaev
 */
public class AllToResultResolver implements ValuesOrderResolver {
  @Override
  public @Nonnull Result resolve(@Nonnull TraceInfo info) {
    final Map<Integer, TraceElement> before = info.getValuesOrderBefore();
    final Map<Integer, TraceElement> after = info.getValuesOrderAfter();

    assert after.size() == 1;

    final TraceElement resultElement = after.values().iterator().next();
    final List<TraceElement> to = Collections.singletonList(resultElement);
    
    final Map<TraceElement, List<TraceElement>> forward = new LinkedHashMap<>();
    for (final TraceElement beforeElement : before.values()) {
      forward.put(beforeElement, to);
    }

    final Map<TraceElement, List<TraceElement>> backward = Collections.singletonMap(resultElement, new ArrayList<>(before.values()));

    return Result.of(forward, backward);
  }
}
