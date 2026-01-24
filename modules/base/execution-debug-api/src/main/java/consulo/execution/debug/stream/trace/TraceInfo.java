// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceInfo {
  @Nonnull
  StreamCall getCall();

  @Nonnull
  Map<Integer, TraceElement> getValuesOrderBefore();

  @Nonnull
  Map<Integer, TraceElement> getValuesOrderAfter();

  @Nullable
  Map<TraceElement, List<TraceElement>> getDirectTrace();

  @Nullable
  Map<TraceElement, List<TraceElement>> getReverseTrace();
}
