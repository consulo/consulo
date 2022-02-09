// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.concurrent;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.AppScheduledExecutorService;
import consulo.ui.ModalityState;
import consulo.util.concurrent.SchedulingWrapper;

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
    super(EdtExecutorServiceImpl.INSTANCE, ((AppScheduledExecutorService)AppExecutorUtil.getAppScheduledExecutorService()).getDelayQueue());
  }


  @Nonnull
  @Override
  public ScheduledFuture<?> schedule(@Nonnull Runnable command, @Nonnull ModalityState modalityState, long delay, TimeUnit unit) {
    MyScheduledFutureTask<?> task = new MyScheduledFutureTask<Void>(command, null, SchedulingWrapper.triggerTime(delayQueue, delay, unit)) {
      @Override
      public void executeMeInBackendExecutor() {
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
