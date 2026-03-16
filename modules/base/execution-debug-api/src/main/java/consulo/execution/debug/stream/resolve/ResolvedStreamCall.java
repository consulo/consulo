// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.resolve;

import consulo.execution.debug.stream.trace.IntermediateState;
import consulo.execution.debug.stream.trace.NextAwareState;
import consulo.execution.debug.stream.trace.PrevAwareState;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.StreamCall;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;
import org.jspecify.annotations.Nullable;

/**
 * @author Vitaliy.Bibaev
 */
public interface ResolvedStreamCall<CALL extends StreamCall, STATE_BEFORE extends IntermediateState, STATE_AFTER extends IntermediateState> {
  
  CALL getCall();

  @Nullable
  STATE_BEFORE getStateBefore();

  @Nullable
  STATE_AFTER getStateAfter();

  interface Intermediate extends ResolvedStreamCall<IntermediateStreamCall, NextAwareState, PrevAwareState> {
    
    @Override
    NextAwareState getStateBefore();

    
    @Override
    PrevAwareState getStateAfter();
  }

  interface Terminator extends ResolvedStreamCall<TerminatorStreamCall, NextAwareState, PrevAwareState> {
    
    @Override
    NextAwareState getStateBefore();
  }
}
