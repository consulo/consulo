/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.application.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.EdtReplacementThread;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ref.Ref;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ApplicationUtil {
  // throws exception if can't grab read action right now
  public static <T> T tryRunReadAction(@Nonnull final Supplier<T> computable) throws CannotRunReadActionException {
    final SimpleReference<T> result = new SimpleReference<>();
    tryRunReadAction(() -> result.set(computable.get()));
    return result.get();
  }

  public static void tryRunReadAction(@Nonnull final Runnable computable) throws CannotRunReadActionException {
    if (!((Application)ApplicationManager.getApplication()).tryRunReadAction(computable)) {
      throw CannotRunReadActionException.create();
    }
  }

  /**
   * Allows to interrupt a process which does not performs checkCancelled() calls by itself.
   * Note that the process may continue to run in background indefinitely - so <b>avoid using this method unless absolutely needed</b>.
   */
  public static <T> T runWithCheckCanceled(@Nonnull final Callable<T> callable,
                                           @Nonnull final ProgressIndicator indicator) throws Exception {
    final Ref<T> result = Ref.create();
    final Ref<Throwable> error = Ref.create();

    Future<?> future = PooledThreadExecutor.INSTANCE.submit(() -> ProgressManager.getInstance().executeProcessUnderProgress(() -> {
      try {
        result.set(callable.call());
      }
      catch (Throwable t) {
        error.set(t);
      }
    }, indicator));

    while (true) {
      try {
        indicator.checkCanceled();
      }
      catch (ProcessCanceledException e) {
        future.cancel(true);
        throw e;
      }

      try {
        future.get(200, TimeUnit.MILLISECONDS);
        ExceptionUtil.rethrowAll(error.get());
        return result.get();
      }
      catch (TimeoutException ignored) {
      }
    }
  }

  public static void invokeLaterSomewhere(@Nonnull Application application,
                                          @Nonnull EdtReplacementThread thread,
                                          @Nonnull ModalityState modalityState,
                                          @Nonnull Runnable r) {
    switch (thread) {
      case EDT:
        application.getLastUIAccess().give(r);
        break;
      case WT:
        application.invokeLaterOnWriteThread(r, modalityState);
        break;
      case EDT_WITH_IW:
        application.invokeLater(r, modalityState);
        break;
    }
  }

  public static void invokeAndWaitSomewhere(@Nonnull Application application,
                                            @Nonnull EdtReplacementThread thread,
                                            @Nonnull ModalityState modalityState,
                                            @Nonnull Runnable r) {
    switch (thread) {
      case EDT:
        if (!UIAccess.isUIThread() && application.isWriteThread()) {
          Logger.getInstance(ApplicationUtil.class).error("Can't invokeAndWait from WT to EDT: probably leads to deadlock");
        }
        Application.get().getLastUIAccess().giveAndWaitIfNeed(r);
        break;
      case WT:
        if (application.isWriteThread()) {
          r.run();
        }
        else if (SwingUtilities.isEventDispatchThread()) {
          Logger.getInstance(ApplicationUtil.class).error("Can't invokeAndWait from EDT to WT");
        }
        else {
          Semaphore s = new Semaphore(1);
          AtomicReference<Throwable> throwable = new AtomicReference<>();
          application.invokeLaterOnWriteThread(() -> {
            try {
              r.run();
            }
            catch (Throwable t) {
              throwable.set(t);
            }
            finally {
              s.up();
            }
          }, modalityState);
          s.waitFor();

          if (throwable.get() != null) {
            ExceptionUtil.rethrow(throwable.get());
          }
        }
        break;
      case EDT_WITH_IW:
        if (!UIAccess.isUIThread() && application.isWriteThread()) {
          Logger.getInstance(ApplicationUtil.class).error("Can't invokeAndWait from WT to EDT: probably leads to deadlock");
        }
        application.invokeAndWait(r, modalityState);
        break;
    }
  }

  public static void showDialogAfterWriteAction(@Nonnull Runnable runnable) {
    Application application = ApplicationManager.getApplication();
    if (application.isWriteAccessAllowed()) {
      application.invokeLater(runnable);
    }
    else {
      runnable.run();
    }
  }

  public static class CannotRunReadActionException extends ProcessCanceledException {
    // When ForkJoinTask joins task which was exceptionally completed from the other thread
    // it tries to re-create that exception (by reflection) and sets its cause to the original exception.
    // That horrible hack causes all sorts of confusion when we try to analyze the exception cause, e.g. in GlobalInspectionContextImpl.inspectFile().
    // To prevent creation of unneeded wrapped exception we restrict constructor visibility to private so that stupid ForkJoinTask has no choice
    // but to use the original exception. (see ForkJoinTask.getThrowableException())
    public static CannotRunReadActionException create() {
      return new CannotRunReadActionException();
    }

    private CannotRunReadActionException() {
    }
  }
}