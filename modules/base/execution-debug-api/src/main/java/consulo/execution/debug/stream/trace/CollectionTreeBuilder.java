// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XValueContainer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface CollectionTreeBuilder {
  boolean isSupported(@Nonnull XValueContainer container);

  @Nonnull
  XNamedValue createXNamedValue(@Nullable Value value, @Nonnull GenericEvaluationContext evaluationContext);

  /**
   * Is called under `com.intellij.debugger.streams.trace.EvaluationContextWrapper.launchDebuggerCommand`
   */
  @Nonnull
  Object getKey(@Nonnull XValueContainer container, @Nonnull Object nullMarker);

  @Nonnull
  Object getKey(@Nonnull TraceElement traceElement, @Nonnull Object nullMarker);

  @Nonnull
  XDebuggerEditorsProvider getEditorsProvider();
}
