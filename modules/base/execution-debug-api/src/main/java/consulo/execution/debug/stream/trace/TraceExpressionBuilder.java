// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.stream.wrapper.StreamChain;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceExpressionBuilder {
  
  
  String createTraceExpression(StreamChain chain);

  
  default XExpression createXExpression(StreamChain chain, String expressionText) {
    return XExpression.fromText(expressionText, EvaluationMode.CODE_FRAGMENT);
  }
}
