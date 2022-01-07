// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application;

import consulo.disposer.Disposable;
import org.jetbrains.annotations.Contract;
import javax.annotation.Nonnull;
import org.jetbrains.concurrency.CancellablePromise;

import java.util.concurrent.Callable;

/**
 * DO NOT USE DIRECTLY
 *
 * @see ExpirableExecutor
 * @see AppUIExecutor
 */
//@ApiStatus.Internal
public interface BaseExpirableExecutor<E extends BaseExpirableExecutor<E>> {
  /**
   * @return an executor that no longer invokes the given runnable after the supplied Disposable is disposed
   */
  @Nonnull
  @Contract(pure = true)
  E expireWith(@Nonnull Disposable parentDisposable);

  /**
   * Schedule execution of the given task.
   */
  void execute(@Nonnull Runnable command);

  /**
   * Schedule the given task's execution and return a Promise that allows to get the result when the task is complete,
   * or cancel the task if it's no longer needed.
   */
  <T> CancellablePromise<T> submit(@Nonnull Callable<T> task);

  /**
   * Schedule the given task's execution and return a Promise that allows to check if the task is complete,
   * or cancel the task if it's no longer needed.
   */
  CancellablePromise<?> submit(@Nonnull Runnable task);
}
