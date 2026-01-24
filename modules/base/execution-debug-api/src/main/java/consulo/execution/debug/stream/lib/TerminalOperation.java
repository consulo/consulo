// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib;

import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface TerminalOperation extends Operation {
  @Nonnull
  TerminatorCallHandler getTraceHandler(@Nonnull TerminatorStreamCall call, @Nonnull String resultExpression, @Nonnull Dsl dsl);
}
