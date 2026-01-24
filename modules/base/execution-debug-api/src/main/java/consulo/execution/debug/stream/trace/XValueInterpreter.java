// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XValue;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public interface XValueInterpreter {
  sealed interface Result permits Result.Array, Result.Error, Result.Unknown {
    final class Array implements Result {
      private final ArrayReference arrayReference;
      private final boolean hasInnerExceptions;
      private final GenericEvaluationContext evaluationContext;

      public Array(@Nonnull ArrayReference arrayReference, boolean hasInnerExceptions, @Nonnull GenericEvaluationContext evaluationContext) {
        this.arrayReference = arrayReference;
        this.hasInnerExceptions = hasInnerExceptions;
        this.evaluationContext = evaluationContext;
      }

      @Nonnull
      public ArrayReference getArrayReference() {
        return arrayReference;
      }

      public boolean getHasInnerExceptions() {
        return hasInnerExceptions;
      }

      @Nonnull
      public GenericEvaluationContext getEvaluationContext() {
        return evaluationContext;
      }
    }

    final class Error implements Result {
      @Nls
      private final String message;

      public Error(@Nls @Nonnull String message) {
        this.message = message;
      }

      @Nls
      @Nonnull
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

  @Nonnull
  CompletableFuture<Result> extract(@Nonnull XDebugSession session, @Nonnull XValue result);
}
