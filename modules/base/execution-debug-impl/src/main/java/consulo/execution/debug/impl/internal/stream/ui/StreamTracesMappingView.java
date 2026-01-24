// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.execution.debug.stream.trace.CollectionTreeBuilder;
import consulo.execution.debug.stream.trace.DebuggerCommandLauncher;
import consulo.execution.debug.stream.trace.GenericEvaluationContext;
import consulo.execution.debug.stream.ui.TraceController;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamTracesMappingView extends FlatView {
  public StreamTracesMappingView(@Nonnull DebuggerCommandLauncher launcher,
                                 @Nonnull GenericEvaluationContext context,
                                 @Nonnull TraceController prevController,
                                 @Nonnull TraceController nextController,
                                 @Nonnull CollectionTreeBuilder builder,
                                 @Nonnull String debugName) {
    super(Arrays.asList(prevController, nextController), launcher, context, builder, debugName);
  }
}
