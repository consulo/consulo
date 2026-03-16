// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.trace;

import consulo.execution.debug.stream.trace.NextAwareState;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.wrapper.StreamCall;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
final class FirstStateImpl extends StateBase implements NextAwareState {
  private final StreamCall myNextCall;
  private final Map<TraceElement, List<TraceElement>> myToNext;

  FirstStateImpl(List<TraceElement> elements,
                 StreamCall nextCall,
                 Map<TraceElement, List<TraceElement>> toNextMapping) {
    super(elements);
    myNextCall = nextCall;
    myToNext = toNextMapping;
  }

  @Override
  public StreamCall getNextCall() {
    return myNextCall;
  }

  @Override
  public List<TraceElement> getNextValues(TraceElement value) {
    return myToNext.getOrDefault(value, Collections.emptyList());
  }
}
