// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util;

import org.jetbrains.annotations.Contract;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Executes tasks synchronously immediately after they submitted
 */
class SameThreadExecutorService extends AbstractExecutorService {
  private volatile boolean isTerminated;

  @Override
  public void shutdown() {
    isTerminated = true;
  }

  @Override
  public boolean isShutdown() {
    return isTerminated;
  }

  @Override
  public boolean isTerminated() {
    return isTerminated;
  }

  @Override
  public boolean awaitTermination(long theTimeout, @Nonnull TimeUnit theUnit) {
    shutdown();
    return true;
  }

  @Nonnull
  @Contract(pure = true)
  @Override
  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    command.run();
  }
}
