/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.debug.frame;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.Semaphore;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;

public class HeadlessValueEvaluationCallback implements XFullValueEvaluator.XFullValueEvaluationCallback {
  private final XValueNode myNode;
  private final Project myProject;

  private volatile boolean myEvaluated;
  private volatile boolean myCanceled;
  private final Semaphore mySemaphore;

  public HeadlessValueEvaluationCallback(@Nonnull XValueNode node, @Nonnull Project project) {
    myNode = node;
    myProject = project;
    mySemaphore = new Semaphore();
    mySemaphore.down();
  }

  public void startFetchingValue(@Nonnull XFullValueEvaluator fullValueEvaluator) {
    fullValueEvaluator.startEvaluation(this);

    new Alarm().addRequest(this::showProgress, 500);
  }

  @Override
  public void evaluated(@Nonnull String fullValue) {
    evaluationComplete(fullValue);
  }

  @Override
  public void evaluated(@Nonnull String fullValue, @Nullable Font font) {
    evaluated(fullValue);
  }

  @Override
  public void errorOccurred(@Nonnull String errorMessage) {
    try {
      XDebuggerUIConstants.NOTIFICATION_GROUP.newError()
        .content(XDebuggerLocalize.loadValueTaskError(errorMessage))
        .notify(myProject);
    }
    finally {
      evaluationComplete(errorMessage);
    }
  }

  private void evaluationComplete(@Nonnull String value) {
    try {
      myEvaluated = true;
      mySemaphore.up();
    }
    finally {
      evaluationComplete(value, myProject);
    }
  }

  public XValueNode getNode() {
    return myNode;
  }

  protected void evaluationComplete(@Nonnull String value, @Nonnull Project project) {

  }

  @Override
  public boolean isObsolete() {
    return myCanceled;
  }

  public void showProgress() {
    if (myEvaluated || myNode.isObsolete()) {
      return;
    }

    new Task.Backgroundable(myProject, XDebuggerBundle.message("load.value.task.text")) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        int i = 0;
        while (!myCanceled && !myEvaluated) {
          indicator.checkCanceled();
          indicator.setFraction(((i++) % 100) * 0.01);
          mySemaphore.waitFor(300);
        }
      }

      @Override
      public boolean shouldStartInBackground() {
        return false;
      }

      @RequiredUIAccess
      @Override
      public void onCancel() {
        myCanceled = true;
      }
    }.queue();
  }
}