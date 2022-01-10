// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.concurrency;

import org.jetbrains.concurrency.internal.DonePromise;
import org.jetbrains.concurrency.internal.InternalPromiseUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface CancellablePromise<T> extends Promise<T>, Future<T> {
  @Nonnull
  @Override
  CancellablePromise<T> onSuccess(@Nonnull Consumer<? super T> handler);

  @Nonnull
  @Override
  CancellablePromise<T> onError(@Nonnull Consumer<Throwable> rejected);

  @Nonnull
  @Override
  CancellablePromise<T> onProcessed(@Nonnull Consumer<? super T> processed);

  void cancel();

  /**
   * Create a promise that is resolved with the given value.
   */
  @Nonnull
  static <T> CancellablePromise<T> resolve(@Nullable T result) {
    if (result == null) {
      //noinspection unchecked
      return (CancellablePromise<T>)InternalPromiseUtil.FULFILLED_PROMISE.get();
    }
    else {
      return new DonePromise<>(InternalPromiseUtil.PromiseValue.createFulfilled(result));
    }
  }
}