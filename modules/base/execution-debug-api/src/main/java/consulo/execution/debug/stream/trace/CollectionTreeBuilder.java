// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XValueContainer;

import org.jspecify.annotations.Nullable;

public interface CollectionTreeBuilder {
  boolean isSupported(XValueContainer container);

  
  XNamedValue createXNamedValue(@Nullable Value value, GenericEvaluationContext evaluationContext);

  /**
   * Is called under `com.intellij.debugger.streams.trace.EvaluationContextWrapper.launchDebuggerCommand`
   */
  Object getKey(XValueContainer container, Object nullMarker);

  
  Object getKey(TraceElement traceElement, Object nullMarker);

  
  XDebuggerEditorsProvider getEditorsProvider();
}
