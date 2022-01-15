/*
 * Copyright 2013-2019 consulo.io
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

import com.intellij.openapi.util.ActionCallback;
import consulo.logging.Logger;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ControlFlowException;
import org.jetbrains.concurrency.internal.DonePromise;
import org.jetbrains.concurrency.internal.InternalPromiseUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class Promises {
  private static final Supplier<Promise> REJECTED = new InternalPromiseUtil.LazyValue<>(() -> new DonePromise(InternalPromiseUtil.PromiseValue.createRejected(createError("rejected"))));

  private static class CountDownConsumer<T> implements Consumer<Object> {
    private final AsyncPromise<T> myPromise;
    private final T myTotalResult;
    private AtomicInteger countDown;

    private CountDownConsumer(int countDown, AsyncPromise<T> promise, T totalResult) {
      myPromise = promise;
      myTotalResult = totalResult;
      this.countDown = new AtomicInteger(countDown);
    }

    @Override
    public void accept(Object o) {
      if (countDown.decrementAndGet() == 0) {
        myPromise.setResult(myTotalResult);
      }
    }
  }

  public static boolean isRejected(Promise<?> promise) {
    return promise.getState() == Promise.State.REJECTED;
  }

  public static boolean isPending(Promise<?> promise) {
    return promise.getState() == Promise.State.PENDING;
  }

  @Nonnull
  public static <T> Promise<T> rejectedPromise() {
    return REJECTED.get();
  }

  @Nonnull
  public static <T> Promise<T> rejectedPromise(String error) {
    return new DonePromise(InternalPromiseUtil.PromiseValue.createRejected(createError(error, true)));
  }

  @Nonnull
  public static <T> Promise<T> resolvedPromise() {
    return (Promise<T>)InternalPromiseUtil.FULFILLED_PROMISE.get();
  }

  @Nonnull
  public static <T> Promise<T> resolvedPromise(@Nullable T result) {
    return resolvedCancellablePromise(result);
  }

  @Nonnull
  public static <T> CancellablePromise<T> resolvedCancellablePromise(@Nullable T result) {
    if (result == null) {
      return (CancellablePromise<T>)InternalPromiseUtil.FULFILLED_PROMISE.get();
    }
    else {
      return new DonePromise(InternalPromiseUtil.PromiseValue.createFulfilled(result));
    }
  }

  public static boolean errorIfNotMessage(Logger logger, Throwable e) {
    if (e instanceof InternalPromiseUtil.MessageError) {
      boolean log = ((InternalPromiseUtil.MessageError)e).log;

      // TODO handle unit test?
      //if (log == ThreeState.YES || (log == ThreeState.UNSURE && ApplicationManager.getApplication().isUnitTestMode())) {
      //  logger.error(e);
      //  return true;
      //}
    }
    else if (!(e instanceof ControlFlowException) && !(e instanceof CancellationException)) {
      logger.error(e);
      return true;
    }

    return false;
  }

  @Nonnull
  public static <T> Promise<List<T>> collectResults(Collection<Promise<T>> thisValue, boolean ignoreErrors) {
    if (thisValue.isEmpty()) {
      return resolvedPromise(Collections.emptyList());
    }

    AsyncPromise<List<T>> result = new AsyncPromise<>();
    AtomicInteger latch = new AtomicInteger(thisValue.size());
    List<T> list = Collections.synchronizedList(new ArrayList<T>(Collections.nCopies(thisValue.size(), null)));

    Runnable arrive = () -> {
      if (latch.decrementAndGet() == 0) {
        if (ignoreErrors) {
          list.removeIf(Objects::isNull);
        }

        result.setResult(list);
      }
    };

    int i = 0;
    for (Promise<T> promise : thisValue) {
      final int index = i;

      try {
        promise.onSuccess(it -> {
          list.set(index, it);

          arrive.run();
        });

        promise.onError(it -> {
          if (ignoreErrors) {
            arrive.run();
          }
          else {
            result.setError(it);
          }
        });
      }
      finally {
        i++;
      }
    }

    return result;
  }

  @Nonnull
  public static Promise<?> all(Collection<? extends Promise<?>> promises) {
    if (promises.size() == 1) {
      return promises.iterator().next();
    }
    else {
      return all(promises, null);
    }
  }

  @Nonnull
  public static <T> Promise<T> all(Collection<? extends Promise<?>> promises, T totalResult) {
    return all(promises, totalResult, false);
  }

  @Nonnull
  public static <T> Promise<T> all(Collection<? extends Promise<?>> promises, T totalResult, boolean ignoreErrors) {
    if (promises.isEmpty()) {
      return resolvedPromise();
    }

    AsyncPromise<T> totalPromise = new AsyncPromise<>();
    CountDownConsumer<T> done = new CountDownConsumer<>(promises.size(), totalPromise, totalResult);

    Consumer<Throwable> rejected = ignoreErrors ? it -> done.accept(null) : totalPromise::setError;

    for (Promise<?> promise : promises) {
      promise.onSuccess(done);
      promise.onError(rejected);
    }
    return totalPromise;
  }

  @Nonnull
  public static ActionCallback toActionCallback(Promise<?> promise) {
    ActionCallback result = new ActionCallback();
    promise.onSuccess(o -> result.setDone());
    promise.onError(throwable -> result.setRejected());
    return result;
  }

  @Nonnull
  public static AsyncResult<Void> toAsyncResult(Promise<?> promise) {
    AsyncResult<Void> result = AsyncResult.undefined();
    promise.onSuccess(o -> result.setDone());
    promise.onError(throwable -> result.setRejected());
    return result;
  }

  @Nonnull
  public static Promise<Object> toPromise(ActionCallback callback) {
    AsyncPromise<Object> promise = new AsyncPromise<>();
    callback.doWhenDone(() -> promise.setResult(null));
    callback.doWhenRejected(error -> promise.setError(createError(error == null ? "Internal error" : error)));
    return promise;
  }

  public static RuntimeException createError(String error) {
    return createError(error, false);
  }

  public static RuntimeException createError(String error, boolean log) {
    return new InternalPromiseUtil.MessageError(error, log);
  }
}
