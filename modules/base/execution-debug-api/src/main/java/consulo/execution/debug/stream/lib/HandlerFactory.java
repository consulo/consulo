// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib;

import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

/**
 * @author Vitaliy.Bibaev
 */
public interface HandlerFactory {
  @Nonnull
  IntermediateCallHandler getForIntermediate(int number, @Nonnull IntermediateStreamCall call);

  @Nonnull
  TerminatorCallHandler getForTermination(@Nonnull TerminatorStreamCall call, @Nonnull @NonNls String resultExpression);
}
