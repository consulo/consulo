// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.concurrency;

import com.intellij.openapi.application.ModalityState;
import javax.annotation.Nonnull;

import java.util.concurrent.*;

/**
 * An {@link ExecutorService} implementation which
 * delegates tasks to the EDT for execution.
 */
public abstract class EdtExecutorService extends AbstractExecutorService {
  @Nonnull
  public static EdtExecutorService getInstance() {
    return EdtExecutorServiceImpl.INSTANCE;
  }

  @Nonnull
  public static ScheduledExecutorService getScheduledExecutorInstance() {
    return EdtScheduledExecutorService.getInstance();
  }

  public abstract void execute(@Nonnull Runnable command, @Nonnull ModalityState modalityState);

  @Nonnull
  public abstract Future<?> submit(@Nonnull Runnable command, @Nonnull ModalityState modalityState);

  @Nonnull
  public abstract <T> Future<T> submit(@Nonnull Callable<T> task, @Nonnull ModalityState modalityState);
}
