/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WaitForProgressToShow {
  private WaitForProgressToShow() {
  }

  public static void runOrInvokeAndWaitAboveProgress(final Runnable command) {
    runOrInvokeAndWaitAboveProgress(command, IdeaModalityState.defaultModalityState());
  }

  public static void runOrInvokeAndWaitAboveProgress(final Runnable command, @Nullable final IdeaModalityState modalityState) {
    final Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      command.run();
    }
    else {
      final ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
      if (pi != null) {
        execute(pi);
        application.invokeAndWait(command, pi.getModalityState());
      }
      else {
        final IdeaModalityState notNullModalityState = modalityState == null ? IdeaModalityState.NON_MODAL : modalityState;
        application.invokeAndWait(command, notNullModalityState);
      }
    }
  }

  public static void runOrInvokeLaterAboveProgress(final Runnable command, @Nullable final consulo.ui.ModalityState modalityState, @Nonnull final Project project) {
    final Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      command.run();
    }
    else {
      final ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
      if (pi != null) {
        execute(pi);
        application.invokeLater(command, pi.getModalityState(), () -> (!project.isOpen()) || project.isDisposed());
      }
      else {
        final IdeaModalityState notNullModalityState = modalityState == null ? IdeaModalityState.NON_MODAL : (IdeaModalityState)modalityState;
        application.invokeLater(command, notNullModalityState, project.getDisposed());
      }
    }
  }

  public static void execute(ProgressIndicator pi) {
    if (pi.isShowing()) {
      final long maxWait = 3000;
      final long start = System.currentTimeMillis();
      while ((!pi.isPopupWasShown()) && (pi.isRunning()) && (System.currentTimeMillis() - maxWait < start)) {
        final Object lock = new Object();
        synchronized (lock) {
          try {
            lock.wait(100);
          }
          catch (InterruptedException e) {
            //
          }
        }
      }
      ProgressManager.checkCanceled();
    }
  }
}