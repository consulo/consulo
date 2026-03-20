// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream;

/**
 * @author Vitaliy.Bibaev
 */
public final class TraceEvaluationException extends TraceException {
  public TraceEvaluationException(String message, String traceExpression) {
    super(message, traceExpression);
  }
}
