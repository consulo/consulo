// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.trace.PrevAwareState;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.Value;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
final class TerminationStateImpl extends StateBase implements PrevAwareState {
  private final TraceElement myResult;
  private final StreamCall myPrevCall;
  private final Map<TraceElement, List<TraceElement>> myToPrev;

  TerminationStateImpl(@Nonnull TraceElement result,
                       @Nonnull StreamCall prevCall,
                       @Nonnull List<TraceElement> elements,
                       @Nonnull Map<TraceElement, List<TraceElement>> toPrevMapping) {
    super(elements);
    myResult = result;
    myPrevCall = prevCall;
    myToPrev = toPrevMapping;
  }

  @Override
  public @Nullable Value getStreamResult() {
    return myResult.getValue();
  }

  @Override
  public @Nonnull StreamCall getPrevCall() {
    return myPrevCall;
  }

  @Override
  public @Nonnull List<TraceElement> getPrevValues(@Nonnull TraceElement value) {
    return myToPrev.getOrDefault(value, Collections.emptyList());
  }
}
