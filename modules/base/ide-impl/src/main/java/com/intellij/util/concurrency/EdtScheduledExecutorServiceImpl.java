// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.concurrency;

import com.intellij.openapi.application.ModalityState;
import javax.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorService} implementation which
 * delegates tasks to the EDT for execution.
 */
class EdtScheduledExecutorServiceImpl extends SchedulingWrapper implements EdtScheduledExecutorService {
  private EdtScheduledExecutorServiceImpl() {
    super(EdtExecutorServiceImpl.INSTANCE, ((AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService()).delayQueue);
  }


  @Nonnull
  @Override
  public ScheduledFuture<?> schedule(@Nonnull Runnable command, @Nonnull ModalityState modalityState, long delay, TimeUnit unit) {
    MyScheduledFutureTask<?> task = new MyScheduledFutureTask<Void>(command, null, triggerTime(delayQueue, delay, unit)) {
      @Override
      void executeMeInBackendExecutor() {
        EdtExecutorService.getInstance().execute(this, modalityState);
      }
    };
    return delayedExecute(task);
  }

  // stubs
  @Override
  public void shutdown() {
    AppScheduledExecutorService.error();
  }

  @Nonnull
  @Override
  public List<Runnable> shutdownNow() {
    return AppScheduledExecutorService.error();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) {
    AppScheduledExecutorService.error();
    return false;
  }

  static final EdtScheduledExecutorService INSTANCE = new EdtScheduledExecutorServiceImpl();
}
