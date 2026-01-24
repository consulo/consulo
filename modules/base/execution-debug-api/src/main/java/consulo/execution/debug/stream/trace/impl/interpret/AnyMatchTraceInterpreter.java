// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace.impl.interpret;

import consulo.execution.debug.stream.trace.TraceElement;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * anyMatch(condition) -> filter(condition).anyMatch(x -> true);   (result is true <~> any element passed thought filter)
 *
 * @author Vitaliy.Bibaev
 */
public class AnyMatchTraceInterpreter extends MatchInterpreterBase {
  @Override
  protected boolean getResult(@Nonnull Collection<TraceElement> traceBeforeFilter, @Nonnull Collection<TraceElement> traceAfterFilter) {
    return !traceAfterFilter.isEmpty();
  }

  @Override
  protected @Nonnull Action getAction(boolean result) {
    if (result) {
      return Action.CONNECT_FILTERED;
    }
    else {
      return Action.CONNECT_DIFFERENCE;
    }
  }
}
