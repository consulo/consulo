// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.*;
import jakarta.inject.Singleton;
import javax.annotation.Nonnull;

import java.util.concurrent.Callable;

/**
 * @author peter
 */
@Singleton
public class AsyncExecutionServiceImpl extends AsyncExecutionService {
  private static long ourWriteActionCounter = 0;

  public AsyncExecutionServiceImpl() {
    Application app = ApplicationManager.getApplication();
    app.addApplicationListener(new ApplicationListener() {
      @Override
      public void writeActionStarted(@Nonnull Object action) {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourWriteActionCounter++;
      }
    }, app);
  }

  //@NotNull
  //@Override
  //protected ExpirableExecutor createExecutor(@NotNull Executor executor) {
  //  return new ExpirableExecutorImpl(executor);
  //}
  //
  @Nonnull
  @Override
  protected AppUIExecutor createUIExecutor(@Nonnull ModalityState modalityState) {
    return new AppUIExecutorImpl(modalityState, ExecutionThread.EDT);
  }

  @Nonnull
  @Override
  protected AppUIExecutor createWriteThreadExecutor(@Nonnull ModalityState modalityState) {
    return new AppUIExecutorImpl(modalityState, ExecutionThread.WT);
  }

  @Nonnull
  @Override
  public <T> NonBlockingReadAction<T> buildNonBlockingReadAction(@Nonnull Callable<T> computation) {
    return new NonBlockingReadActionImpl<>(computation);
  }

  static long getWriteActionCounter() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return ourWriteActionCounter;
  }
}
