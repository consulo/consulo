// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.util.concurrent;

import consulo.util.concurrent.internal.DonePromise;
import consulo.util.concurrent.internal.InternalPromiseUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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