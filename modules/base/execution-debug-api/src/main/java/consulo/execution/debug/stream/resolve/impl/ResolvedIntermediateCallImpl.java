// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve.impl;

import consulo.execution.debug.stream.resolve.ResolvedStreamCall;
import consulo.execution.debug.stream.trace.NextAwareState;
import consulo.execution.debug.stream.trace.PrevAwareState;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class ResolvedIntermediateCallImpl implements ResolvedStreamCall.Intermediate {
  private final IntermediateStreamCall myCall;
  private final NextAwareState myStateBefore;
  private final PrevAwareState myStateAfter;

  public ResolvedIntermediateCallImpl(@Nonnull IntermediateStreamCall call,
                                      @Nonnull NextAwareState stateBefore,
                                      @Nonnull PrevAwareState stateAfter) {
    myCall = call;
    myStateBefore = stateBefore;
    myStateAfter = stateAfter;
  }

  @Override
  public @Nonnull IntermediateStreamCall getCall() {
    return myCall;
  }

  @Override
  public @Nonnull NextAwareState getStateBefore() {
    return myStateBefore;
  }

  @Override
  public @Nonnull PrevAwareState getStateAfter() {
    return myStateAfter;
  }
}
