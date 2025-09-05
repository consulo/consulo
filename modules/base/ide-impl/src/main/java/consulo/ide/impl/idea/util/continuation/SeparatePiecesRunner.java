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
package consulo.ide.impl.idea.util.continuation;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.ide.impl.idea.openapi.progress.impl.BackgroundableProcessIndicator;
import consulo.project.Project;
import consulo.versionControlSystem.internal.BackgroundFromStartOption;
import jakarta.annotation.Nonnull;

import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author irengrig
 * @since 2011-04-07
 */
public class SeparatePiecesRunner extends GeneralRunner {
  private final AtomicReference<TaskWrapper> myCurrentWrapper;

  public SeparatePiecesRunner(Project project, boolean cancellable) {
    super(project, cancellable);
    myCurrentWrapper = new AtomicReference<TaskWrapper>();
  }

  @Override
  @RequiredUIAccess
  public void ping() {
    clearSuspend();
    Application application = ApplicationManager.getApplication();
    if (! application.isDispatchThread()) {
      Runnable command = new Runnable() {
        @Override
        public void run() {
          pingImpl();
        }
      };
      SwingUtilities.invokeLater(command);
    } else {
      pingImpl();
    }
  }

  @RequiredUIAccess
  private void pingImpl() {
    while (true) {
      myCurrentWrapper.set(null);
      // stop if project is being disposed
      if (!myProject.isDefault() && !myProject.isOpen()) return;
      if (getSuspendFlag()) return;
      TaskDescriptor current = getNextMatching();
      if (current == null) {
        return;
      }

      if (Where.AWT.equals(current.getWhere())) {
        setIndicator(null);
        try {
          current.run(this);
        }
        catch (RuntimeException th) {
          handleException(th, true);
        }
      }
      else {
        TaskWrapper task = new TaskWrapper(myProject, current.getName(), myCancellable, current);
        myCurrentWrapper.set(task);
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          setIndicator(new EmptyProgressIndicator());
        }
        else {
          setIndicator(new BackgroundableProcessIndicator(task));
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, getIndicator());
        return;
      }
    }
  }

  @Override
  public void suspend() {
    super.suspend();
    TaskWrapper wrapper = myCurrentWrapper.get();
    if (wrapper != null){
      wrapper.mySuspended = true;
    }
  }

  class TaskWrapper extends ModalityIgnorantBackgroundableTask {
    private final TaskDescriptor myTaskDescriptor;
    private volatile boolean mySuspended;

    TaskWrapper(@jakarta.annotation.Nullable Project project,
                @Nonnull String title,
                boolean canBeCancelled,
                TaskDescriptor taskDescriptor) {
      super(project, title, canBeCancelled, BackgroundFromStartOption.getInstance());
      myTaskDescriptor = taskDescriptor;
      mySuspended = false;
    }

    @Override
    @RequiredUIAccess
    protected void doInAwtIfFail(Exception e) {
      doInAwtIfCancel();
    }

    @Override
    @RequiredUIAccess
    protected void doInAwtIfCancel() {
      onCancel();
    }

    @Override
    @RequiredUIAccess
    protected void doInAwtIfSuccess() {
      if (! mySuspended) {
        ping();
      }
    }

    @Override
    protected void runImpl(@Nonnull ProgressIndicator indicator) {
      myTaskDescriptor.run(SeparatePiecesRunner.this);
    }
  }
}
