/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import consulo.annotation.DeprecationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Deprecated
@DeprecationInfo("Use consulo.util.concurrent.AsyncResult")
public class AsyncResult<T> extends ActionCallback {
  private static final Logger LOG = LoggerFactory.getLogger(AsyncResult.class);

  private static final AsyncResult REJECTED = new Rejected();

  @Nonnull
  public static <R> AsyncResult<R> undefined() {
    return new AsyncResult<>();
  }

  @Nonnull
  public static <R> AsyncResult<R> rejected() {
    //noinspection unchecked
    return REJECTED;
  }

  @Nonnull
  public static <R> AsyncResult<R> rejected(@Nonnull String errorMessage) {
    AsyncResult<R> result = new AsyncResult<>();
    result.reject(errorMessage);
    return result;
  }

  @Nonnull
  public static <R> AsyncResult<R> resolved() {
    return resolved(null);
  }

  @Nonnull
  public static <R> AsyncResult<R> resolved(@Nullable R result) {
    return new AsyncResult<R>().setDone(result);
  }

  @Nonnull
  @Deprecated
  public static <R> AsyncResult<R> done(@Nullable R result) {
    return new AsyncResult<R>().setDone(result);
  }

  @Nonnull
  public static <T> AsyncResult<T> merge(@Nonnull Collection<AsyncResult<T>> list) {
    if (list.isEmpty()) {
      return resolved();
    }

    AsyncResult<T> result = undefined();

    AtomicInteger count = new AtomicInteger(list.size());
    AtomicBoolean rejectResult = new AtomicBoolean();

    Runnable finishAction = () -> {
      int i = count.decrementAndGet();
      if (i == 0) {
        if (rejectResult.get()) {
          result.setRejected();
        }
        else {
          result.setDone();
        }
      }
    };

    for (AsyncResult<?> asyncResult : list) {
      asyncResult.doWhenDone(finishAction::run);

      asyncResult.doWhenRejected(o -> {
        rejectResult.set(true);
        finishAction.run();
      });
    }
    return result;
  }

  protected T myResult;

  @Deprecated
  public AsyncResult() {
  }

  AsyncResult(int countToDone, @Nullable T result) {
    super(countToDone);

    myResult = result;
  }

  @Nonnull
  public AsyncResult<T> setDone(T result) {
    myResult = result;
    setDone();
    return this;
  }

  @Nonnull
  public AsyncResult<T> setRejected(T result) {
    myResult = result;
    setRejected();
    return this;
  }

  @Nonnull
  public <DependentResult> AsyncResult<DependentResult> subResult(@Nonnull Function<T, DependentResult> doneHandler) {
    return subResult(new AsyncResult<DependentResult>(), doneHandler);
  }

  @Nonnull
  public <SubResult, SubAsyncResult extends AsyncResult<SubResult>> SubAsyncResult subResult(@Nonnull SubAsyncResult subResult, @Nonnull Function<T, SubResult> doneHandler) {
    doWhenDone(new SubResultDoneCallback<>(subResult, doneHandler)).notifyWhenRejected(subResult);
    return subResult;
  }

  @Nonnull
  public ActionCallback subCallback(@Nonnull Consumer<T> doneHandler) {
    ActionCallback subCallback = new ActionCallback();
    doWhenDone(new SubCallbackDoneCallback<>(subCallback, doneHandler)).notifyWhenRejected(subCallback);
    return subCallback;
  }

  @Nonnull
  @Override
  public AsyncResult<T> doWhenDone(@Nonnull Runnable runnable) {
    super.doWhenDone(runnable);
    return this;
  }

  @Nonnull
  @Override
  public AsyncResult<T> rejectWithThrowable(Throwable error) {
    super.rejectWithThrowable(error);
    return this;
  }

  @Nonnull
  public AsyncResult<T> doWhenDone(@Nonnull final Consumer<T> consumer) {
    doWhenDone(() -> consumer.accept(myResult));
    return this;
  }

  @Nonnull
  public AsyncResult<T> doWhenRejected(@Nonnull final BiConsumer<T, String> consumer) {
    doWhenRejected(() -> consumer.accept(myResult, myError));
    return this;
  }

  @Override
  @Nonnull
  public AsyncResult<T> doWhenProcessed(@Nonnull final Runnable runnable) {
    doWhenDone(runnable);
    doWhenRejected(runnable);
    return this;
  }

  @Override
  @Nonnull
  @SuppressWarnings("unchecked")
  public final AsyncResult<T> notify(@Nonnull final ActionCallback child) {
    if (child instanceof AsyncResult) {
      return notify((AsyncResult<T>)child);
    }
    super.notify(child);
    return this;
  }

  @Nonnull
  public final AsyncResult<T> notify(@Nonnull final AsyncResult<T> child) {
    doWhenDone((Consumer<T>)child::setDone);
    doWhenRejected(child::reject);
    doWhenRejectedWithThrowable(child::rejectWithThrowable);
    return this;
  }

  @Nonnull
  public AsyncResult<Void> toVoid() {
    AsyncResult<Void> result = new AsyncResult<>();
    doWhenDone((Runnable)result::setDone);
    doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  public T getResult() {
    return myResult;
  }

  public T getResultSync() {
    return getResultSync(-1);
  }

  @Nullable
  public T getResultSync(long msTimeout) {
    waitFor(msTimeout);
    return myResult;
  }

  @Nonnull
  public final AsyncResult<T> doWhenProcessed(@Nonnull final Consumer<T> consumer) {
    doWhenDone(consumer);
    doWhenRejected((result, error) -> consumer.accept(result));
    return this;
  }

  private static class Rejected<T> extends AsyncResult<T> {
    public Rejected() {
      setRejected();
    }

    public Rejected(T value) {
      setRejected(value);
    }
  }

  // we don't use inner class, avoid memory leak, we don't want to hold this result while dependent is computing
  private static class SubResultDoneCallback<Result, SubResult, AsyncSubResult extends AsyncResult<SubResult>> implements Consumer<Result> {
    private final AsyncSubResult subResult;
    private final Function<Result, SubResult> doneHandler;

    public SubResultDoneCallback(AsyncSubResult subResult, Function<Result, SubResult> doneHandler) {
      this.subResult = subResult;
      this.doneHandler = doneHandler;
    }

    @Override
    public void accept(Result result) {
      SubResult v;
      try {
        v = doneHandler.apply(result);
      }
      catch (Throwable e) {
        subResult.reject(e.getMessage());
        LOG.error(e.getMessage(), e);
        return;
      }
      subResult.setDone(v);
    }
  }

  private static class SubCallbackDoneCallback<Result> implements Consumer<Result> {
    private final ActionCallback subResult;
    private final Consumer<Result> doneHandler;

    public SubCallbackDoneCallback(ActionCallback subResult, Consumer<Result> doneHandler) {
      this.subResult = subResult;
      this.doneHandler = doneHandler;
    }

    @Override
    public void accept(Result result) {
      try {
        doneHandler.accept(result);
      }
      catch (Throwable e) {
        subResult.reject(e.getMessage());
        LOG.error(e.getMessage(), e);
        return;
      }
      subResult.setDone();
    }
  }
}