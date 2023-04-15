// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.application.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.*;
import consulo.application.event.ApplicationListener;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ui.ModalityState;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.annotation.Nonnull;

import java.util.concurrent.Callable;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class AsyncExecutionServiceImpl extends AsyncExecutionService {
  private static long ourWriteActionCounter = 0;

  private final Application myApplication;

  @Inject
  public AsyncExecutionServiceImpl(Application app) {
    myApplication = app;
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
    return new AppUIExecutorImpl((IdeaModalityState)modalityState, ExecutionThread.EDT);
  }

  @Nonnull
  @Override
  protected AppUIExecutor createWriteThreadExecutor(@Nonnull ModalityState modalityState) {
    return new AppUIExecutorImpl((IdeaModalityState)modalityState, ExecutionThread.WT);
  }

  @Nonnull
  @Override
  public <T> NonBlockingReadAction<T> buildNonBlockingReadAction(@Nonnull Callable<T> computation) {
    return new NonBlockingReadActionImpl<>(myApplication, computation);
  }

  static long getWriteActionCounter() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return ourWriteActionCounter;
  }
}
