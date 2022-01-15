// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application;

import com.intellij.openapi.components.ServiceManager;
import javax.annotation.Nonnull;

import java.util.concurrent.Callable;

/**
 * An internal service not supposed to be used directly
 */
//@ApiStatus.Internal
public abstract class AsyncExecutionService {
  //@NotNull
  //protected abstract ExpirableExecutor createExecutor(@NotNull Executor executor);
  //
  @Nonnull
  protected abstract AppUIExecutor createUIExecutor(@Nonnull ModalityState modalityState);

  @Nonnull
  protected abstract AppUIExecutor createWriteThreadExecutor(@Nonnull ModalityState modalityState);

  @Nonnull
  protected abstract <T> NonBlockingReadAction<T> buildNonBlockingReadAction(@Nonnull Callable<T> computation);

  @Nonnull
  static AsyncExecutionService getService() {
    return ServiceManager.getService(AsyncExecutionService.class);
  }
}
