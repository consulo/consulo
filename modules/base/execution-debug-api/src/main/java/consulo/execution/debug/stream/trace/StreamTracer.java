// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.stream.wrapper.StreamChain;

/**
 * @author Vitaliy.Bibaev
 */
public interface StreamTracer {
  sealed interface Result permits Result.Evaluated, Result.EvaluationFailed, Result.CompilationFailed, Result.Unknown {
    final class Evaluated implements Result {
      private final TracingResult result;
      private final GenericEvaluationContext evaluationContext;

      public Evaluated(TracingResult result, GenericEvaluationContext evaluationContext) {
        this.result = result;
        this.evaluationContext = evaluationContext;
      }

      
      public TracingResult getResult() {
        return result;
      }

      
      public GenericEvaluationContext getEvaluationContext() {
        return evaluationContext;
      }
    }

    final class EvaluationFailed implements Result {
      private final String traceExpression;
      
      private final String message;

      public EvaluationFailed(String traceExpression, String message) {
        this.traceExpression = traceExpression;
        this.message = message;
      }

      
      public String getTraceExpression() {
        return traceExpression;
      }

      
      
      public String getMessage() {
        return message;
      }
    }

    final class CompilationFailed implements Result {
      private final String traceExpression;
      
      private final String message;

      public CompilationFailed(String traceExpression, String message) {
        this.traceExpression = traceExpression;
        this.message = message;
      }

      
      public String getTraceExpression() {
        return traceExpression;
      }

      
      
      public String getMessage() {
        return message;
      }
    }

    final class Unknown implements Result {
      public static final Unknown INSTANCE = new Unknown();

      private Unknown() {
      }
    }
  }

  
  Result trace(StreamChain chain);
}
