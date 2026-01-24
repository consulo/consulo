// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.trace.IntermediateState;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.Value;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
abstract class StateBase implements IntermediateState {
  private final List<TraceElement> myElements;

  StateBase(@Nonnull List<TraceElement> elements) {
    myElements = List.copyOf(elements);
  }

  @Override
  public @Nonnull List<TraceElement> getTrace() {
    return myElements;
  }

  @Override
  public @Nullable Value getStreamResult() {
    return null;
  }
}
