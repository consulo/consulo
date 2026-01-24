// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream;

import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class TraceCompilationException extends TraceException {

  public TraceCompilationException(@Nonnull String message, @Nonnull String traceExpression) {
    super(message, traceExpression);
  }
}
