// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.application.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.*;
import consulo.application.event.ApplicationListener;
import consulo.ui.ModalityState;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
      public void writeActionStarted(Object action) {
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
  
  @Override
  protected AppUIExecutor createUIExecutor(ModalityState modalityState) {
    return new AppUIExecutorImpl(modalityState, ExecutionThread.EDT);
  }

  
  @Override
  protected AppUIExecutor createWriteThreadExecutor(ModalityState modalityState) {
    return new AppUIExecutorImpl(modalityState, ExecutionThread.WT);
  }

  
  @Override
  public <T> NonBlockingReadAction<T> buildNonBlockingReadAction(Callable<T> computation) {
    return new NonBlockingReadActionImpl<>(myApplication, computation);
  }

  static long getWriteActionCounter() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    return ourWriteActionCounter;
  }
}
