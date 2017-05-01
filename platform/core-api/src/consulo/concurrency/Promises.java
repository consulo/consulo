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

import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.Consumer;
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
  private static final NotNullLazyValue<Promise> ourRejectedPromise = NotNullLazyValue.createValue(() -> Promise.REJECTED);
  private static final NotNullLazyValue<Promise> ourResolvedPromise = NotNullLazyValue.createValue(() -> Promise.DONE);

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Promise<T> rejectedPromise() {
    return ourRejectedPromise.getValue();
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public static <T> Promise<T> resolvedPromise() {
    return ourResolvedPromise.getValue();
  }

  @NotNull
  public static <T> Promise<T> reject(@Nullable Throwable error) {
    if (error == null) {
      //noinspection unchecked
      return rejectedPromise();
    }
    else {
      return new RejectedPromise<T>(error);
    }
  }

  @NotNull
  public static <T> Promise<T> reject(@NotNull String error) {
    return Promises.reject(Promise.createError(error));
  }

  @NotNull
  public static Promise<Void> all(@NotNull Collection<Promise<?>> promises) {
    return all(promises, null);
  }

  @NotNull
  public static <T> Promise<T> all(@NotNull Collection<Promise<?>> promises, @Nullable T totalResult) {
    if (promises.isEmpty()) {
      //noinspection unchecked
      return (Promise<T>)Promise.DONE;
    }

    final AsyncPromise<T> totalPromise = new AsyncPromise<T>();
    Consumer done = new CountDownConsumer<T>(promises.size(), totalPromise, totalResult);
    Consumer<Throwable> rejected = new Consumer<Throwable>() {
      @Override
      public void consume(Throwable error) {
        if (totalPromise.getState() == AsyncPromise.State.PENDING) {
          totalPromise.setError(error);
        }
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
    final AsyncPromise<Void> promise = new AsyncPromise<Void>();
    asyncResult.doWhenDone(new Runnable() {
      @Override
      public void run() {
        promise.setResult(null);
      }
    }).doWhenRejected(new Consumer<String>() {
      @Override
      public void consume(String error) {
        promise.setError(Promise.createError(error == null ? "Internal error" : error));
      }
    });
    return promise;
  }

  @NotNull
  public static <T> Promise<T> wrap(@NotNull AsyncResult<T> asyncResult) {
    final AsyncPromise<T> promise = new AsyncPromise<T>();
    asyncResult.doWhenDone(new Consumer<T>() {
      @Override
      public void consume(T result) {
        promise.setResult(result);
      }
    }).doWhenRejected(new Consumer<String>() {
      @Override
      public void consume(String error) {
        promise.setError(Promise.createError(error));
      }
    });
    return promise;
  }

  @NotNull
  public static <T> Promise<T> resolve(T result) {
    if (result == null) {
      //noinspection unchecked
      return (Promise<T>)Promise.DONE;
    }
    else {
      return new DonePromise<T>(result);
    }
  }
}
