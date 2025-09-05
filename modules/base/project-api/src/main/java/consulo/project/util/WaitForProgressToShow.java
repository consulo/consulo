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
package consulo.project.util;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.ui.ModalityState;

import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class WaitForProgressToShow {
  private WaitForProgressToShow() {
  }

  public static void runOrInvokeAndWaitAboveProgress(@RequiredUIAccess Runnable command) {
    runOrInvokeAndWaitAboveProgress(command, Application.get().getDefaultModalityState());
  }

  public static void runOrInvokeAndWaitAboveProgress(@RequiredUIAccess Runnable command, @Nullable ModalityState modalityState) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      command.run();
    }
    else {
      ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
      if (pi != null) {
        execute(pi);
        application.invokeAndWait(command, pi.getModalityState());
      }
      else {
        ModalityState notNullModalityState = modalityState == null ? application.getNoneModalityState() : modalityState;
        application.invokeAndWait(command, notNullModalityState);
      }
    }
  }

  public static void runOrInvokeLaterAboveProgress(@RequiredUIAccess Runnable command, @Nullable ModalityState modalityState, @Nonnull Project project) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      command.run();
    }
    else {
      ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
      if (pi != null) {
        execute(pi);
        application.invokeLater(command, pi.getModalityState(), () -> (!project.isOpen()) || project.isDisposed());
      }
      else {
        ModalityState notNullModalityState = modalityState == null ? application.getNoneModalityState() : modalityState;
        application.invokeLater(command, notNullModalityState, project.getDisposed());
      }
    }
  }

  public static void execute(ProgressIndicator pi) {
    if (pi.isShowing()) {
      long maxWait = 3000;
      long start = System.currentTimeMillis();
      while ((!pi.isPopupWasShown()) && (pi.isRunning()) && (System.currentTimeMillis() - maxWait < start)) {
        Object lock = new Object();
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