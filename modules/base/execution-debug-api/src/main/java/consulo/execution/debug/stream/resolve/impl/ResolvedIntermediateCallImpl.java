// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve.impl;

import consulo.execution.debug.stream.resolve.ResolvedStreamCall;
import consulo.execution.debug.stream.trace.NextAwareState;
import consulo.execution.debug.stream.trace.PrevAwareState;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;

/**
 * @author Vitaliy.Bibaev
 */
public class ResolvedIntermediateCallImpl implements ResolvedStreamCall.Intermediate {
  private final IntermediateStreamCall myCall;
  private final NextAwareState myStateBefore;
  private final PrevAwareState myStateAfter;

  public ResolvedIntermediateCallImpl(IntermediateStreamCall call,
                                      NextAwareState stateBefore,
                                      PrevAwareState stateAfter) {
    myCall = call;
    myStateBefore = stateBefore;
    myStateAfter = stateAfter;
  }

  @Override
  public IntermediateStreamCall getCall() {
    return myCall;
  }

  @Override
  public NextAwareState getStateBefore() {
    return myStateBefore;
  }

  @Override
  public PrevAwareState getStateAfter() {
    return myStateAfter;
  }
}
