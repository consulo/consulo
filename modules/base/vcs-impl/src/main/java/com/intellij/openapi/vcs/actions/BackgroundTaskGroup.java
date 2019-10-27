/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.application.ModalityState;
import consulo.logging.Logger;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.util.ThrowableConsumer;
import com.intellij.util.ThrowableRunnable;
import javax.annotation.Nonnull;

import java.util.List;

import static com.intellij.util.containers.ContainerUtil.createLockFreeCopyOnWriteList;

/**
 * Provides common cancellation and error handling behaviour for group of dependent tasks.
 */
public class BackgroundTaskGroup extends BackgroundTaskQueue {

  private static final Logger LOG = Logger.getInstance(BackgroundTaskGroup.class);

  @Nonnull
  protected final List<VcsException> myExceptions = createLockFreeCopyOnWriteList();
  @Nonnull
  private final Project myProject;

  public BackgroundTaskGroup(@Nonnull Project project, @Nonnull String title) {
    super(project, title);
    myProject = project;
  }

  @Override
  public void run(@Nonnull Task.Backgroundable task, @javax.annotation.Nullable ModalityState modalityState, @javax.annotation.Nullable ProgressIndicator indicator) {
    throw new UnsupportedOperationException();
  }

  public void runInBackground(@Nonnull String title, @Nonnull ThrowableConsumer<ProgressIndicator, VcsException> task) {
    myProcessor.add(continuation -> new Task.Backgroundable(myProject, title, true) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        try {
          task.consume(indicator);
        }
        catch (VcsException e) {
          addError(e);
          if (!e.isWarning()) {
            indicator.cancel();
          }
        }
      }

      @Override
      public void onCancel() {
        end();
      }

      @Override
      public void onThrowable(@Nonnull Throwable e) {
        LOG.error(e);
        end();
      }

      @Override
      public void onFinished() {
        continuation.run();
      }
    }.queue());
  }

  public void runInEdt(@Nonnull ThrowableRunnable<VcsException> task) {
    myProcessor.add(continuation -> {
      boolean isSuccess = false;
      try {
        task.run();
        isSuccess = true;
      }
      catch (VcsException e) {
        addError(e);
        isSuccess = e.isWarning();
      }
      finally {
        if (!isSuccess) {
          end();
        }
        continuation.run();
      }
    });
  }

  public void addError(@Nonnull VcsException e) {
    myExceptions.add(e);
  }

  public void showErrors() {
    if (!myExceptions.isEmpty()) {
      AbstractVcsHelper.getInstance(myProject).showErrors(myExceptions, myTitle);
    }
  }

  public void end() {
    myProcessor.clear();
    showErrors();
  }
}
