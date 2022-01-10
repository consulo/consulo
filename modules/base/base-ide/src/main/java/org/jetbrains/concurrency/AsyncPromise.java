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

import consulo.logging.Logger;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ExceptionUtil;
import org.jetbrains.concurrency.internal.InternalPromiseUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class AsyncPromise<T> implements CancellablePromise<T>, InternalPromiseUtil.CompletablePromise<T> {
  private static final Logger LOG = Logger.getInstance(AsyncPromise.class);

  private final CompletableFuture<T> f;
  private final AtomicBoolean hasErrorHandler;

  public AsyncPromise() {
    this(new CompletableFuture<>(), new AtomicBoolean());
  }

  private AsyncPromise(CompletableFuture<T> f, AtomicBoolean hasErrorHandler) {
    this.f = f;
    this.hasErrorHandler = hasErrorHandler;
  }

  @Override
  public boolean isDone() {
    return f.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    if (isCancelled()) {
      return null;
    }

    try {
      return f.get();
    }
    catch (CancellationException ignored) {
      return null;
    }
  }

  @Override
  public T get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (isCancelled()) {
      return null;
    }

    try {
      return f.get(timeout, unit);
    }
    catch (CancellationException ignored) {
      return null;
    }
  }

  @Override
  public boolean isCancelled() {
    return f.isCancelled();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return !isCancelled() && f.cancel(mayInterruptIfRunning);
  }

  @Override
  public void cancel() {
    cancel(false);
  }

  @Nonnull
  @Override
  public State getState() {
    if (!f.isDone()) {
      return State.PENDING;
    }
    else if (f.isCompletedExceptionally()) {
      return State.REJECTED;
    }
    else {
      return State.SUCCEEDED;
    }
  }

  @Nonnull
  @Override
  public CancellablePromise<T> onSuccess(@Nonnull Consumer<? super T> handler) {
    return new AsyncPromise<>(f.whenComplete((value, exception) -> {
      if (exception == null && !InternalPromiseUtil.isHandlerObsolete(handler)) {
        try {
          handler.accept(value);
        }
        catch (Throwable e) {
          if (!(e instanceof ControlFlowException)) {
            LOG.error(e);
          }
        }
      }
    }), hasErrorHandler);
  }

  @Nonnull
  @Override
  public CancellablePromise<T> onError(@Nonnull Consumer<Throwable> rejected) {
    hasErrorHandler.set(true);
    return new AsyncPromise<>(f.whenComplete((value, exception) -> {
      if (exception != null) {
        Throwable toReport = (exception instanceof CompletionException && exception.getCause() != null) ? exception.getCause() : exception;

        if (!InternalPromiseUtil.isHandlerObsolete(rejected)) {
          rejected.accept(toReport);
        }
      }
    }), hasErrorHandler);
  }

  @Nonnull
  @Override
  public CancellablePromise<T> onProcessed(@Nonnull Consumer<? super T> processed) {
    hasErrorHandler.set(true);
    return new AsyncPromise<>(f.whenComplete((value, exception) -> {
      if (!InternalPromiseUtil.isHandlerObsolete(processed)) {
        processed.accept(value);
      }
    }), hasErrorHandler);
  }

  @Nullable
  @Override
  public T blockingGet(int timeout, @Nonnull TimeUnit timeUnit) throws TimeoutException, ExecutionException {
    try {
      return get(timeout, timeUnit);
    }
    catch (ExecutionException e) {
      if (e.getCause() == InternalPromiseUtil.OBSOLETE_ERROR) {
        return null;
      }

      ExceptionUtil.rethrowUnchecked(e.getCause());
      throw e;
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> then(@Nonnull Function<? super T, ? extends SUB_RESULT> done) {
    return new AsyncPromise<>(f.thenApply(done::apply), hasErrorHandler);
  }

  @Nonnull
  @Override
  public <SUB_RESULT> Promise<SUB_RESULT> thenAsync(@Nonnull Function<? super T, Promise<SUB_RESULT>> doneF) {
    Function<T, CompletableFuture<SUB_RESULT>> convert = it -> {
      Promise<SUB_RESULT> promise = doneF.apply(it);
      CompletableFuture<SUB_RESULT> future = new CompletableFuture<>();

      promise.onSuccess(future::complete).onError(future::completeExceptionally);
      return future;
    };
    return new AsyncPromise<>(f.thenCompose(convert), hasErrorHandler);
  }

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public Promise<T> processed(@Nonnull Promise<? super T> child) {
    if (!(child instanceof AsyncPromise)) {
      return this;
    }
    return onSuccess(((AsyncPromise)child)::setResult).onError(((AsyncPromise)child)::setError);
  }

  @Override
  public void setResult(@Nullable T t) {
    f.complete(t);
  }

  @Override
  public boolean setError(@Nonnull Throwable error) {
    if (!f.completeExceptionally(error)) {
      return false;
    }

    if (!hasErrorHandler.get()) {
      Promises.errorIfNotMessage(LOG, error);
    }
    return true;
  }

  public void setError(@Nonnull String error) {
    setError(Promises.createError(error));
  }
}