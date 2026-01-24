// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.stream.wrapper.StreamChain;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceExpressionBuilder {
  @NonNls
  @Nonnull
  String createTraceExpression(@Nonnull StreamChain chain);

  @Nonnull
  default XExpression createXExpression(@Nonnull StreamChain chain, @Nonnull String expressionText) {
    return XExpression.fromText(expressionText, EvaluationMode.CODE_FRAGMENT);
  }
}
