/*
 * Copyright 2013-2017 consulo.io
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
package consulo.concurrency;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.Consumer;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.*;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 30-Apr-17
 * <p>
 * from kotlin intellij-community\platform\projectModel-api\src\org\jetbrains\concurrency\promise.kt
 */
public class Promises {
  private static final NotNullLazyValue<Promise> REJECTED = NotNullLazyValue.createValue(() -> Promise.REJECTED);
  private static final NotNullLazyValue<Promise> DONE = NotNullLazyValue.createValue(() -> Promise.DONE);
  private static final NotNullLazyValue<RuntimeException> OBSOLETE_ERROR = NotNullLazyValue.createValue(() -> Promise.createError("Obsolete"));
  private static final NotNullLazyValue<Promise> CANCELLED_PROMISE = NotNullLazyValue.createValue(() -> new RejectedPromise(OBSOLETE_ERROR.getValue()));

  public static boolean isFulfilled(@Nullable  Promise<?> promise) {
    return promise != null && promise.getState() == Promise.State.FULFILLED;
  }

  public static boolean isRejected(@Nullable  Promise<?> promise) {
    return promise != null && promise.getState() == Promise.State.REJECTED;
  }

  public static boolean isPending(@Nullable  Promise<?> promise) {
    return promise != null && promise.getState() == Promise.State.PENDING;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Promise<T> rejectedPromise() {
    return REJECTED.getValue();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Promise<T> resolvedPromise() {
    return DONE.getValue();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Promise<T> cancelledPromise() {
    return CANCELLED_PROMISE.getValue();
  }

  @NotNull
  public static <T> Promise<T> rejectedPromise(@Nullable Throwable error) {
    if (error == null) {
      //noinspection unchecked
      return rejectedPromise();
    }
    else {
      return new RejectedPromise<>(error);
    }
  }

  @NotNull
  public static <T> Promise<T> rejectedPromise(@NotNull String error) {
    return rejectedPromise(Promise.createError(error));
  }

  @NotNull
  @Deprecated
  public static <T> Promise<T> reject(@Nullable Throwable error) {
    return rejectedPromise(error);
  }

  @NotNull
  @Deprecated
  public static <T> Promise<T> reject(@NotNull String error) {
    return rejectedPromise(error);
  }

  @NotNull
  public static Promise<Void> all(@NotNull Collection<Promise<?>> promises) {
    return all(promises, null);
  }

  @NotNull
  public static <T> Promise<T> all(@NotNull Collection<Promise<?>> promises, @Nullable T totalResult) {
    if (promises.isEmpty()) {
      //noinspection unchecked
      return (Promise<T>)DONE.getValue();
    }

    final AsyncPromise<T> totalPromise = new AsyncPromise<>();
    Consumer done = new CountDownConsumer<>(promises.size(), totalPromise, totalResult);
    Consumer<Throwable> rejected = error -> {
      if (totalPromise.getState() == AsyncPromise.State.PENDING) {
        totalPromise.setError(error);
      }
    };

    for (Promise<?> promise : promises) {
      //noinspection unchecked
      promise.done(done);
      promise.rejected(rejected);
    }
    return totalPromise;
  }

  @NotNull
  public static Promise<Void> wrapAsVoid(@NotNull ActionCallback asyncResult) {
    final AsyncPromise<Void> promise = new AsyncPromise<>();
    asyncResult.doWhenDone(() -> promise.setResult(null))
            .doWhenRejected(error -> promise.setError(Promise.createError(error == null ? "Internal error" : error)));
    return promise;
  }

  @NotNull
  public static <T> Promise<T> wrap(@NotNull AsyncResult<T> asyncResult) {
    final AsyncPromise<T> promise = new AsyncPromise<>();
    asyncResult.doWhenDone(promise::setResult).doWhenRejected(error -> promise.setError(Promise.createError(error)));
    return promise;
  }

  @NotNull
  public static <T> Promise<T> resolve(T result) {
    if (result == null) {
      //noinspection unchecked
      return (Promise<T>)Promise.DONE;
    }
    else {
      return new DonePromise<>(result);
    }
  }

  public static boolean errorIfNotMessage(Logger logger, Throwable e) {
    if (e instanceof Promise.MessageError) {
      ThreeState log = ((Promise.MessageError)e).getLog();
      if (log == ThreeState.YES || (log == ThreeState.UNSURE && (ApplicationManager.getApplication().isUnitTestMode()))) {
        logger.error(e);
        return true;
      }
    }
    else if (!(e instanceof ProcessCanceledException)) {
      logger.error(e);
      return true;
    }

    return false;
  }

}
