// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.concurent;

import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Allows to {@link #schedule(Callable, long, TimeUnit)} tasks later
 * and execute them in parallel in the {@code backendExecutor} with not more than at {@code maxSimultaneousTasks} at a time.
 */
class BoundedScheduledExecutorService extends SchedulingWrapper {
  BoundedScheduledExecutorService(@Nonnull String name,
                                  @Nonnull ExecutorService backendExecutor,
                                  int maxThreads,
                                  AppDelayQueue appDelayQueue) {
    super(new BoundedTaskExecutor(name, backendExecutor, maxThreads, true), appDelayQueue);
    assert !(backendExecutor instanceof ScheduledExecutorService) : "backendExecutor is already ScheduledExecutorService: " + backendExecutor;
  }

  @Override
  public void shutdown() {
    super.shutdown();
    cancelAndRemoveTasksFromQueue();
    backendExecutorService.shutdown();
  }

  @Nonnull
  @Override
  public List<Runnable> shutdownNow() {
    return ContainerUtil.concat(super.shutdownNow(), backendExecutorService.shutdownNow());
  }

  @Override
  public boolean isShutdown() {
    return super.isShutdown() && backendExecutorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return super.isTerminated() && backendExecutorService.isTerminated();
  }
}
