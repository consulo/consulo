// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.resolve.impl;

import consulo.execution.debug.stream.resolve.ResolvedStreamCall;
import consulo.execution.debug.stream.trace.NextAwareState;
import consulo.execution.debug.stream.trace.PrevAwareState;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Vitaliy.Bibaev
 */
public class ResolvedTerminatorCallImpl implements ResolvedStreamCall.Terminator {
  private final TerminatorStreamCall myCall;
  private final NextAwareState myStateBefore;
  private final PrevAwareState myStateAfter;

  public ResolvedTerminatorCallImpl(@Nonnull TerminatorStreamCall call,
                                    @Nonnull NextAwareState stateBefore,
                                    @Nonnull PrevAwareState stateAfter) {
    myCall = call;
    myStateBefore = stateBefore;
    myStateAfter = stateAfter;
  }

  @Override
  public @Nonnull TerminatorStreamCall getCall() {
    return myCall;
  }

  @Override
  public @Nullable PrevAwareState getStateAfter() {
    return myStateAfter;
  }

  @Override
  public @Nonnull NextAwareState getStateBefore() {
    return myStateBefore;
  }
}
