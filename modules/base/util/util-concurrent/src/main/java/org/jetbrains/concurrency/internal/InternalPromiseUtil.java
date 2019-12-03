// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.concurrency.internal;

import consulo.util.lang.ExceptionUtil;
import org.jetbrains.concurrency.CancellablePromise;
import org.jetbrains.concurrency.Obsolescent;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Only internal usage.
 */
public class InternalPromiseUtil {
  public static final RuntimeException OBSOLETE_ERROR = new MessageError("Obsolete", false);

  public static final Supplier<Promise<Object>> CANCELLED_PROMISE = new LazyValue<>(() -> new DonePromise<>(PromiseValue.createRejected(OBSOLETE_ERROR)));

  public static final Supplier<CancellablePromise<Object>> FULFILLED_PROMISE = new LazyValue<>(() -> new DonePromise<>(PromiseValue.createFulfilled(null)));

  public static class LazyValue<T> implements Supplier<T> {
    private final Supplier<T> myFactory;
    private T myValue;

    public LazyValue(Supplier<T> factory) {
      myFactory = factory;
    }

    @Override
    public T get() {
      if (myValue == null) {
        return myValue = myFactory.get();
      }
      return myValue;
    }
  }

  public static boolean isHandlerObsolete(@Nonnull Object handler) {
    return handler instanceof Obsolescent && ((Obsolescent)handler).isObsolete();
  }

  public interface PromiseImpl<T> {
    void _setValue(@Nonnull PromiseValue<T> value);
  }

  public interface CompletablePromise<T> extends Promise<T> {
    void setResult(@Nullable T t);

    boolean setError(@Nonnull Throwable error);
  }

  @SuppressWarnings("ExceptionClassNameDoesntEndWithException")
  public static class MessageError extends RuntimeException {
    public final boolean log;

    public MessageError(@Nonnull String message, boolean isLog) {
      super(message);

      log = isLog;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }

  public static class PromiseValue<T> {
    public final T result;
    public final Throwable error;

    public static <T> PromiseValue<T> createFulfilled(@Nullable T result) {
      return new PromiseValue<>(result, null);
    }

    public static <T> PromiseValue<T> createRejected(@Nullable Throwable error) {
      return new PromiseValue<>(null, error);
    }

    private PromiseValue(@Nullable T result, @Nullable Throwable error) {
      this.result = result;
      this.error = error;
    }

    @Nonnull
    public Promise.State getState() {
      return error == null ? Promise.State.SUCCEEDED : Promise.State.REJECTED;
    }

    public boolean isCancelled() {
      return error == OBSOLETE_ERROR;
    }

    @Nullable
    public T getResultOrThrowError() throws ExecutionException, TimeoutException {
      if (error == null) {
        return result;
      }

      if (error == OBSOLETE_ERROR) {
        return null;
      }

      ExceptionUtil.rethrowUnchecked(error);
      if (error instanceof ExecutionException) {
        throw ((ExecutionException)error);
      }
      if (error instanceof TimeoutException) {
        throw ((TimeoutException)error);
      }
      throw new ExecutionException(error);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PromiseValue<?> value = (PromiseValue<?>)o;
      return Objects.equals(result, value.result) && Objects.equals(error, value.error);
    }

    @Override
    public int hashCode() {
      int result1 = result != null ? result.hashCode() : 0;
      result1 = 31 * result1 + (error != null ? error.hashCode() : 0);
      return result1;
    }
  }

  public abstract static class BasePromise<T> implements Promise<T>, Future<T>, InternalPromiseUtil.PromiseImpl<T>, CancellablePromise<T> {
    @Nullable
    protected abstract PromiseValue<T> getValue();

    /**
     * The same as @{link Future{@link Future#isDone()}}.
     * Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method will return true.
     */
    @Override
    public final boolean isDone() {
      return getValue() != null;
    }

    @Nonnull
    @Override
    public final State getState() {
      PromiseValue<T> value = getValue();
      return value == null ? State.PENDING : value.getState();
    }

    @Override
    public final boolean isCancelled() {
      PromiseValue<T> value = getValue();
      return value != null && value.isCancelled();
    }

    @Override
    public final T get() throws ExecutionException {
      try {
        return blockingGet(-1);
      }
      catch (TimeoutException e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public final T get(long timeout, @Nonnull TimeUnit unit) throws ExecutionException, TimeoutException {
      return blockingGet((int)timeout, unit);
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
      if (getState() == State.PENDING) {
        cancel();
        return true;
      }
      else {
        return false;
      }
    }
  }
}
