// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.application.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.*;
import consulo.application.event.ApplicationListener;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class AsyncExecutionServiceImpl extends AsyncExecutionService {
  private static AtomicLong ourWriteActionCounter = new AtomicLong();

  private final Application myApplication;

  @Inject
  public AsyncExecutionServiceImpl(Application app) {
    myApplication = app;
    app.addApplicationListener(new ApplicationListener() {
      @Override
      public void writeActionStarted(@Nonnull Object action) {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        ourWriteActionCounter.incrementAndGet();
      }
    }, app);
  }

  @Nonnull
  @Override
  protected AppUIExecutor createUIExecutor(@Nonnull ModalityState modalityState) {
    return new AppUIExecutorImpl(myApplication.getLastUIAccess());
  }

  @Nonnull
  @Override
  protected AppUIExecutor createWriteThreadExecutor() {
    return new AppUIExecutorImpl(myApplication.getLock().writeExecutor());
  }

  @Nonnull
  @Override
  public <T> NonBlockingReadAction<T> buildNonBlockingReadAction(@Nonnull Callable<T> computation) {
    return new NonBlockingReadActionImpl<>(myApplication, computation);
  }

  static long getWriteActionCounter() {
    return ourWriteActionCounter.get();
  }
}
