// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XValue;

import java.util.concurrent.CompletableFuture;

public interface XValueInterpreter {
  sealed interface Result permits Result.Array, Result.Error, Result.Unknown {
    final class Array implements Result {
      private final ArrayReference arrayReference;
      private final boolean hasInnerExceptions;
      private final GenericEvaluationContext evaluationContext;

      public Array(ArrayReference arrayReference, boolean hasInnerExceptions, GenericEvaluationContext evaluationContext) {
        this.arrayReference = arrayReference;
        this.hasInnerExceptions = hasInnerExceptions;
        this.evaluationContext = evaluationContext;
      }

      
      public ArrayReference getArrayReference() {
        return arrayReference;
      }

      public boolean getHasInnerExceptions() {
        return hasInnerExceptions;
      }

      
      public GenericEvaluationContext getEvaluationContext() {
        return evaluationContext;
      }
    }

    final class Error implements Result {
      
      private final String message;

      public Error(String message) {
        this.message = message;
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

  
  CompletableFuture<Result> extract(XDebugSession session, XValue result);
}
