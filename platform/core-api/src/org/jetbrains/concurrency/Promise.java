/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.concurrency;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import consulo.annotations.DeprecationInfo;
import org.jetbrains.annotations.NotNull;

public abstract class Promise<T> {
  @Deprecated
  @DeprecationInfo("Use Promises.resolvedPromise()")
  public static final Promise<Void> DONE = new DonePromise<Void>(null);
  @Deprecated
  @DeprecationInfo("Use Promises.rejectedPromise()")
  public static final Promise<Void> REJECTED = new RejectedPromise<Void>(createError("rejected"));

  @NotNull
  public static RuntimeException createError(@NotNull String error) {
    return new MessageError(error);
  }

  public enum State {
    PENDING,
    FULFILLED,
    REJECTED
  }

  @NotNull
  public abstract Promise<T> done(@NotNull Consumer<T> done);

  @NotNull
  public abstract Promise<T> processed(@NotNull AsyncPromise<T> fulfilled);

  @NotNull
  public abstract Promise<T> rejected(@NotNull Consumer<Throwable> rejected);

  public abstract Promise<T> processed(@NotNull Consumer<T> processed);

  @NotNull
  public abstract <SUB_RESULT> Promise<SUB_RESULT> then(@NotNull Function<T, SUB_RESULT> done);

  @NotNull
  public abstract State getState();

  @SuppressWarnings("ExceptionClassNameDoesntEndWithException")
  static class MessageError extends RuntimeException {
    public MessageError(@NotNull String error) {
      super(error);
    }

    @NotNull
    @Override
    public final synchronized Throwable fillInStackTrace() {
      return this;
    }
  }

  /**
   * Log error if not message error
   */
  public static void logError(@NotNull Logger logger, @NotNull Throwable e) {
    if (!(e instanceof MessageError) || ApplicationManager.getApplication().isUnitTestMode()) {
      logger.error(e);
    }
  }

  public abstract void notify(@NotNull AsyncPromise<T> child);
}