// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.concurrency;

import com.intellij.util.Consumer;
import com.intellij.util.Function;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The Promise represents the eventual completion (or failure) of an asynchronous operation, and its resulting value.
 * <p>
 * A Promise is a proxy for a value not necessarily known when the promise is created.
 * It allows you to associate handlers with an asynchronous action's eventual success value or failure reason.
 * This lets asynchronous methods return values like synchronous methods: instead of immediately returning the final value,
 * the asynchronous method returns a promise to supply the value at some point in the future.
 * <p>
 * A Promise is in one of these states:
 *
 * <ul>
 * <li>pending: initial state, neither fulfilled nor rejected.</li>
 * <li>succeeded: meaning that the operation completed successfully.</li>
 * <li>rejected: meaning that the operation failed.</li>
 * </ul>
 */
public interface Promise<T> {
  enum State {
    PENDING,
    SUCCEEDED,
    REJECTED
  }

  /**
   * @deprecated Use Promises.resolvedPromise
   */
  @Deprecated
  @Nonnull
  static <T> Promise<T> resolve(@Nullable T result) {
    if (result == null) {
      //noinspection unchecked
      return (Promise<T>)InternalPromiseUtil.FULFILLED_PROMISE.getValue();
    }
    else {
      return new DonePromise<>(InternalPromiseUtil.PromiseValue.createFulfilled(result));
    }
  }

  /**
   * Execute passed handler on promise resolve and return a promise with a transformed result value.
   *
   * <pre>
   * {@code
   *
   * somePromise
   *  .then(it -> transformOrProcessValue(it))
   * }
   * </pre>
   */
  @Nonnull
  <SUB_RESULT> Promise<SUB_RESULT> then(@Nonnull Function<? super T, ? extends SUB_RESULT> done);

  /**
   * The same as {@link #then(Function)}, but handler can be asynchronous.
   *
   * <pre>
   * {@code
   *
   * somePromise
   *  .then(it -> transformOrProcessValue(it))
   *  .thenAsync(it -> processValueAsync(it))
   * }
   * </pre>
   */
  @Nonnull
  <SUB_RESULT> Promise<SUB_RESULT> thenAsync(@Nonnull Function<? super T, Promise<SUB_RESULT>> done);

  /**
   * Execute passed handler on promise resolve.
   */
  @Nonnull
  Promise<T> onSuccess(@Nonnull java.util.function.Consumer<? super T> handler);

  /**
   * Execute passed handler on promise resolve.
   *
   * @deprecated Use {@link #onSuccess(java.util.function.Consumer)}
   */
  @Deprecated
  @Nonnull
  default Promise<T> done(@Nonnull Consumer<? super T> done) {
    return onSuccess(it -> done.consume(it));
  }

  /**
   * Execute passed handler on promise reject.
   */
  @Nonnull
  Promise<T> onError(@Nonnull java.util.function.Consumer<Throwable> rejected);

  /**
   * @deprecated Use {@link #onError(java.util.function.Consumer)}
   */
  @Deprecated
  @Nonnull
  default Promise<T> rejected(@Nonnull Consumer<Throwable> rejected) {
    return onError(it -> rejected.consume(it));
  }

  /**
   * Resolve or reject passed promise as soon as this promise resolved or rejected.
   */
  @Nonnull
  Promise<T> processed(@Nonnull Promise<? super T> child);

  /**
   * Execute passed handler on promise resolve (result value will be passed),
   * or on promise reject (null as result value will be passed).
   */
  @Nonnull
  Promise<T> onProcessed(@Nonnull java.util.function.Consumer<? super T> processed);

  /**
   * Execute passed handler on promise resolve (result value will be passed),
   * or on promise reject (null as result value will be passed).
   */
  @Deprecated
  @Nonnull
  default Promise<T> processed(@Nonnull Consumer<? super T> action) {
    return onProcessed(it -> action.consume(it));
  }

  /**
   * Get promise state.
   */
  @Nonnull
  State getState();

  @Nullable
  T blockingGet(int timeout, @Nonnull TimeUnit timeUnit) throws TimeoutException, ExecutionException;

  @Nullable
  default T blockingGet(int timeout) throws TimeoutException, ExecutionException {
    return blockingGet(timeout, TimeUnit.MILLISECONDS);
  }

  default boolean isSucceeded() {
    return getState() == State.SUCCEEDED;
  }
}