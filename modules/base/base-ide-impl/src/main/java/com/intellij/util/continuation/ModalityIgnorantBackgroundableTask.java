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
package com.intellij.util.continuation;

import consulo.logging.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 8/19/11
 * Time: 12:14 PM
 */
public abstract class ModalityIgnorantBackgroundableTask extends Task.Backgroundable {
  private final static Logger LOG = Logger.getInstance(ModalityIgnorantBackgroundableTask.class);
  private Consumer<Task.Backgroundable> myRunner;
  private int myCnt;

  public ModalityIgnorantBackgroundableTask(@javax.annotation.Nullable Project project,
                                            @Nonnull String title,
                                            boolean canBeCancelled,
                                            @Nullable PerformInBackgroundOption backgroundOption) {
    super(project, title, canBeCancelled, backgroundOption);
  }

  public ModalityIgnorantBackgroundableTask(@javax.annotation.Nullable Project project,
                                            @Nonnull String title,
                                            boolean canBeCancelled) {
    super(project, title, canBeCancelled);
  }

  public ModalityIgnorantBackgroundableTask(@javax.annotation.Nullable Project project, @Nonnull String title) {
    super(project, title);
  }

  @RequiredUIAccess
  protected abstract void doInAwtIfFail(final Exception e);
  @RequiredUIAccess
  protected abstract void doInAwtIfCancel();
  @RequiredUIAccess
  protected abstract void doInAwtIfSuccess();
  protected abstract void runImpl(@Nonnull ProgressIndicator indicator);

  public void runSteadily(final Consumer<Backgroundable> consumer) {
    myRunner = consumer;
    myCnt = 100;
    consumer.consume(this);
  }

  @Override
  public void run(@Nonnull final ProgressIndicator indicator) {
    try {
      runImpl(indicator);
    } catch (final ToBeRepeatedException tbre) {
      if (myRunner != null && myCnt > 0) {
        -- myCnt;
        // we are on some background thread and do not want to reschedule too often
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException e) {
          //
        }
        myRunner.consume(this);
        return;
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          doInAwtIfFail(tbre);
        }
      });
    } catch (final Exception e) {
      LOG.info(e);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          doInAwtIfFail(e);
        }
      });
      return;
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (indicator.isCanceled()) {
          doInAwtIfCancel();
        } else {
          doInAwtIfSuccess();
        }
      }
    });
  }

  public static class ToBeRepeatedException extends RuntimeException {}
}
