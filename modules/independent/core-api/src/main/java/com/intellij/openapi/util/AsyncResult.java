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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.PairConsumer;
import consulo.annotations.DeprecationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncResult<T> extends ActionCallback {
  private static final Logger LOG = Logger.getInstance(AsyncResult.class);

  private static final AsyncResult REJECTED = new Rejected();

  @NotNull
  public static <R> AsyncResult<R> rejected() {
    //noinspection unchecked
    return REJECTED;
  }

  @NotNull
  public static <R> AsyncResult<R> rejected(@NotNull String errorMessage) {
    AsyncResult<R> result = new AsyncResult<>();
    result.reject(errorMessage);
    return result;
  }

  @NotNull
  public static <R> AsyncResult<R> done(@Nullable R result) {
    return new AsyncResult<R>().setDone(result);
  }

  public static AsyncResult<?> merge(@NotNull Collection<AsyncResult<?>> list) {
    if (list.isEmpty()) {
      return done(null);
    }

    AsyncResult<Object> result = new AsyncResult<>();

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

  public AsyncResult() {
  }

  AsyncResult(int countToDone, @Nullable T result) {
    super(countToDone);

    myResult = result;
  }

  @NotNull
  public AsyncResult<T> setDone(T result) {
    myResult = result;
    setDone();
    return this;
  }

  @NotNull
  public AsyncResult<T> setRejected(T result) {
    myResult = result;
    setRejected();
    return this;
  }

  @NotNull
  public <DependentResult> AsyncResult<DependentResult> subResult(@NotNull Function<T, DependentResult> doneHandler) {
    return subResult(new AsyncResult<DependentResult>(), doneHandler);
  }

  @NotNull
  public <SubResult, SubAsyncResult extends AsyncResult<SubResult>> SubAsyncResult subResult(@NotNull SubAsyncResult subResult, @NotNull Function<T, SubResult> doneHandler) {
    doWhenDone(new SubResultDoneCallback<>(subResult, doneHandler)).notifyWhenRejected(subResult);
    return subResult;
  }

  @NotNull
  public ActionCallback subCallback(@NotNull Consumer<T> doneHandler) {
    ActionCallback subCallback = new ActionCallback();
    doWhenDone(new SubCallbackDoneCallback<>(subCallback, doneHandler)).notifyWhenRejected(subCallback);
    return subCallback;
  }

  @NotNull
  public AsyncResult<T> doWhenDone(@NotNull final Consumer<T> consumer) {
    doWhenDone(() -> consumer.consume(myResult));
    return this;
  }

  @NotNull
  public AsyncResult<T> doWhenRejected(@NotNull final PairConsumer<T, String> consumer) {
    doWhenRejected(() -> consumer.consume(myResult, myError));
    return this;
  }

  @Override
  @NotNull
  public final AsyncResult<T> notify(@NotNull final ActionCallback child) {
    super.notify(child);
    return this;
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

  @NotNull
  public final ActionCallback doWhenProcessed(@NotNull final Consumer<T> consumer) {
    doWhenDone(consumer);
    doWhenRejected((result, error) -> consumer.consume(result));
    return this;
  }

  @Deprecated
  @DeprecationInfo("Use #done() method")
  public static class Done<T> extends AsyncResult<T> {
    public Done(T value) {
      setDone(value);
    }
  }

  @Deprecated
  @DeprecationInfo("Use #rejected() method")
  public static class Rejected<T> extends AsyncResult<T> {
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
    public void consume(Result result) {
      SubResult v;
      try {
        v = doneHandler.fun(result);
      }
      catch (Throwable e) {
        subResult.reject(e.getMessage());
        LOG.error(e);
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
    public void consume(Result result) {
      try {
        doneHandler.consume(result);
      }
      catch (Throwable e) {
        subResult.reject(e.getMessage());
        LOG.error(e);
        return;
      }
      subResult.setDone();
    }
  }
}
