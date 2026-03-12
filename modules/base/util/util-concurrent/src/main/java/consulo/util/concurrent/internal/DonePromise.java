// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.concurrent.internal;

import consulo.util.concurrent.CancellablePromise;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.internal.InternalPromiseUtil.PromiseValue;

import org.jspecify.annotations.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.util.concurrent.internal.InternalPromiseUtil.CANCELLED_PROMISE;
import static consulo.util.concurrent.internal.InternalPromiseUtil.isHandlerObsolete;

public class DonePromise<T> extends InternalPromiseUtil.BasePromise<T> {
  private final PromiseValue<T> value;

  public DonePromise(PromiseValue<T> value) {
    this.value = value;
  }

  @Override
  public CancellablePromise<T> onSuccess(Consumer<? super T> handler) {
    if (value.error != null) {
      return this;
    }

    if (!isHandlerObsolete(handler)) {
      handler.accept(value.result);
    }
    return this;
  }

  @Override
  public CancellablePromise<T> processed(Promise<? super T> child) {
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

  @Override
  public CancellablePromise<T> onProcessed(Consumer<? super T> handler) {
    if (value.error == null) {
      onSuccess(handler);
    }
    else if (!isHandlerObsolete(handler)) {
      handler.accept(null);
    }
    return this;
  }

  @Override
  public CancellablePromise<T> onError(Consumer<Throwable> handler) {
    if (value.error != null && !isHandlerObsolete(handler)) {
      handler.accept(value.error);
    }
    return this;
  }

  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> then(Function<? super T, ? extends SUB_RESULT> done) {
    if (value.error != null) {
      //noinspection unchecked
      return (Promise<SUB_RESULT>)this;
    }
    else if (isHandlerObsolete(done)) {
      //noinspection unchecked
      return (Promise<SUB_RESULT>)CANCELLED_PROMISE.get();
    }
    else {
      return new DonePromise<>(PromiseValue.createFulfilled(done.apply(value.result)));
    }
  }

  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> thenAsync(Function<? super T, Promise<SUB_RESULT>> done) {
    if (value.error == null) {
      return done.apply(value.result);
    }
    else {
      //noinspection unchecked
      return (Promise<SUB_RESULT>)this;
    }
  }

  @Nullable
  @Override
  public T blockingGet(int timeout, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
    return value.getResultOrThrowError();
  }

  @Override
  public void _setValue(PromiseValue<T> value) {
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