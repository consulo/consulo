// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface PrevAwareState extends IntermediateState {
  @Nullable
  StreamCall getPrevCall();

  @Nonnull
  List<TraceElement> getPrevValues(@Nonnull TraceElement value);
}
