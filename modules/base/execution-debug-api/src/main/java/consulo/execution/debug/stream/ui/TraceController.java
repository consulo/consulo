// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.ui;

import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.trace.Value;
import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceController extends ValuesHighlightingListener {
  @Nullable
  Value getStreamResult();

  @Nonnull
  List<TraceElement> getTrace();

  @Nullable
  StreamCall getNextCall();

  @Nullable
  StreamCall getPrevCall();

  @Nonnull
  List<TraceElement> getNextValues(@Nonnull TraceElement element);

  @Nonnull
  List<TraceElement> getPrevValues(@Nonnull TraceElement element);

  default boolean isSelectionExists() {
    return isSelectionExists(PropagationDirection.BACKWARD) || isSelectionExists(PropagationDirection.FORWARD);
  }

  boolean isSelectionExists(@Nonnull PropagationDirection direction);

  void register(@Nonnull TraceContainer listener);
}
