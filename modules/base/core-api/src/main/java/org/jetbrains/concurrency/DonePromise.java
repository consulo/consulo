// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.concurrency;

import com.intellij.util.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.concurrency.InternalPromiseUtil.PromiseValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.jetbrains.concurrency.InternalPromiseUtil.CANCELLED_PROMISE;
import static org.jetbrains.concurrency.InternalPromiseUtil.isHandlerObsolete;

class DonePromise<T> extends InternalPromiseUtil.BasePromise<T> {
  private final PromiseValue<T> value;

  DonePromise(@Nonnull PromiseValue<T> value) {
    this.value = value;
  }

  @Nonnull
  @Override
  public Promise<T> onSuccess(@Nonnull Consumer<? super T> handler) {
    if (value.error != null) {
      return this;
    }

    if (!isHandlerObsolete(handler)) {
      handler.accept(value.result);
    }
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> processed(@Nonnull Promise<? super T> child) {
    if (child instanceof InternalPromiseUtil.PromiseImpl) {
      //noinspection unchecked
      ((InternalPromiseUtil.PromiseImpl<T>)child)._setValue(value);
    }
    else if (child instanceof InternalPromiseUtil.CompletablePromise) {
      //noinspection unchecked
      ((InternalPromiseUtil.CompletablePromise<T>)child).setResult(value.result);
    }
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> onProcessed(@Nonnull Consumer<? super T> handler) {
    if (value.error == null) {
      onSuccess(handler);
    }
    else if (!isHandlerObsolete(handler)) {
      handler.accept(null);
    }
    return this;
  }

  @Nonnull
  @Override
  public Promise<T> onError(@Nonnull Consumer<Throwable> handler) {
    if (value.error != null && !isHandlerObsolete(handler)) {
      handler.accept(value.error);
    }
    return this;
  }

  @Nonnull
  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> then(@Nonnull Function<? super T, ? extends SUB_RESULT> done) {
    if (value.error != null) {
      //noinspection unchecked
      return (Promise<SUB_RESULT>)this;
    }
    else if (isHandlerObsolete(done)) {
      //noinspection unchecked
      return (Promise<SUB_RESULT>)CANCELLED_PROMISE.getValue();
    }
    else {
      return new DonePromise<>(PromiseValue.createFulfilled(done.fun(value.result)));
    }
  }

  @Nonnull
  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> thenAsync(@Nonnull Function<? super T, Promise<SUB_RESULT>> done) {
    if (value.error == null) {
      return done.fun(value.result);
    }
    else {
      //noinspection unchecked
      return (Promise<SUB_RESULT>)this;
    }
  }

  @Nullable
  @Override
  public T blockingGet(int timeout, @Nonnull TimeUnit timeUnit) throws ExecutionException, TimeoutException {
    return value.getResultOrThrowError();
  }

  @Override
  public void _setValue(@Nonnull PromiseValue<T> value) {
  }

  @Nullable
  @Override
  protected PromiseValue<T> getValue() {
    return value;
  }

  @Override
  public void cancel() {
  }
}