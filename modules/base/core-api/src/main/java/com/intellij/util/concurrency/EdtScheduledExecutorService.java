// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.concurrency;

import com.intellij.openapi.application.ModalityState;
import javax.annotation.Nonnull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorService} implementation which
 * schedules tasks to the EDT for execution.
 */
public interface EdtScheduledExecutorService extends ScheduledExecutorService {
  @Nonnull
  static EdtScheduledExecutorService getInstance() {
    return EdtScheduledExecutorServiceImpl.INSTANCE;
  }

  @Nonnull
  ScheduledFuture<?> schedule(@Nonnull Runnable command, @Nonnull ModalityState modalityState, long delay, TimeUnit unit);
}
