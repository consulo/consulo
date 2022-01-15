// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.constraints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * @author eldar
 */
public class ConstrainedTaskExecutor implements Executor {
  private final
  @Nonnull
  ConstrainedExecutionScheduler myExecutionScheduler;
  private final
  @Nullable
  BooleanSupplier myCancellationCondition;
  private final
  @Nullable
  Expiration myExpiration;

  public ConstrainedTaskExecutor(@Nonnull ConstrainedExecutionScheduler executionScheduler, @Nullable BooleanSupplier cancellationCondition, @Nullable Expiration expiration) {
    myExecutionScheduler = executionScheduler;
    myCancellationCondition = cancellationCondition;
    myExpiration = expiration;
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    final BooleanSupplier condition = ((myExpiration == null) && (myCancellationCondition == null)) ? null : () -> {
      if (myExpiration != null && myExpiration.isExpired()) return false;
      if (myCancellationCondition != null && myCancellationCondition.getAsBoolean()) return false;
      return true;
    };
    myExecutionScheduler.scheduleWithinConstraints(command, condition);
  }

  public CancellablePromise<Void> submit(@Nonnull Runnable task) {
    return submit(() -> {
      task.run();
      return null;
    });
  }

  public <T> CancellablePromise<T> submit(@Nonnull Callable<? extends T> task) {
    final AsyncPromise<T> promise = new AsyncPromise<>();
    if (myExpiration != null) {
      final Expiration.Handle expirationHandle = myExpiration.invokeOnExpiration(promise::cancel);
      promise.onProcessed(value -> expirationHandle.unregisterHandler());
    }

    final BooleanSupplier condition = () -> {
      if (promise.isCancelled()) return false;
      if (myExpiration != null && myExpiration.isExpired()) return false;
      if (myCancellationCondition != null && myCancellationCondition.getAsBoolean()) {
        promise.cancel();
        return false;
      }
      return true;
    };
    myExecutionScheduler.scheduleWithinConstraints(() -> {
      try {
        final T result = task.call();
        promise.setResult(result);
      }
      catch (Throwable e) {
        promise.setError(e);
      }
    }, condition);
    return promise;
  }
}
