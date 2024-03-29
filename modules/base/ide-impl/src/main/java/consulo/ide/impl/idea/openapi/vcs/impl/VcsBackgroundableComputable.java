/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.ThrowableComputable;
import consulo.ide.impl.idea.openapi.vcs.changes.BackgroundFromStartOption;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;

public class VcsBackgroundableComputable<T> extends Task.Backgroundable {
  private final String myErrorTitle;

  private boolean mySilent;
  private final Project myProject;
  private final BackgroundableActionEnabledHandler myHandler;
  private final Object myActionParameter;
  private final ThrowableComputable<T, VcsException> myBackgroundable;

  private final Consumer<T> myAwtSuccessContinuation;
  private final Runnable myAwtErrorContinuation;

  private VcsException myException;
  private T myResult;

  private VcsBackgroundableComputable(final Project project, final String title,
                                  final String errorTitle,
                                  final ThrowableComputable<T, VcsException> backgroundable,
                                  final Consumer<T> awtSuccessContinuation,
                                  final Runnable awtErrorContinuation,
                                  final BackgroundableActionEnabledHandler handler,
                                  final Object actionParameter) {
    super(project, title, true, BackgroundFromStartOption.getInstance());
    myErrorTitle = errorTitle;
    myBackgroundable = backgroundable;
    myAwtSuccessContinuation = awtSuccessContinuation;
    myAwtErrorContinuation = awtErrorContinuation;
    myProject = project;
    myHandler = handler;
    myActionParameter = actionParameter;
  }

  public static <T> void createAndRunSilent(final Project project, @jakarta.annotation.Nullable final VcsBackgroundableActions actionKey,
                                 @Nullable final Object actionParameter, final String title,
                                 final ThrowableComputable<T, VcsException> backgroundable,
                                 @Nullable final Consumer<T> awtSuccessContinuation) {
    createAndRun(project, actionKey, actionParameter, title, null, backgroundable, awtSuccessContinuation, null, true);
  }

  public static <T> void createAndRun(final Project project, @Nullable final VcsBackgroundableActions actionKey,
                                 @Nullable final Object actionParameter,
                                 final String title,
                                 final String errorTitle,
                                 final ThrowableComputable<T, VcsException> backgroundable) {
    createAndRun(project, actionKey, actionParameter, title, errorTitle, backgroundable, null, null);
  }

  public static <T> void createAndRun(final Project project, @jakarta.annotation.Nullable final VcsBackgroundableActions actionKey,
                                 @Nullable final Object actionParameter,
                                 final String title,
                                 final String errorTitle,
                                 final ThrowableComputable<T, VcsException> backgroundable,
                                 @Nullable final Consumer<T> awtSuccessContinuation,
                                 @Nullable final Runnable awtErrorContinuation) {
    createAndRun(project, actionKey, actionParameter, title, errorTitle, backgroundable, awtSuccessContinuation, awtErrorContinuation, false);
  }

  private static <T> void createAndRun(final Project project, @jakarta.annotation.Nullable final VcsBackgroundableActions actionKey,
                                 @jakarta.annotation.Nullable final Object actionParameter,
                                 final String title,
                                 final String errorTitle,
                                 final ThrowableComputable<T, VcsException> backgroundable,
                                 @jakarta.annotation.Nullable final Consumer<T> awtSuccessContinuation,
                                 @Nullable final Runnable awtErrorContinuation, final boolean silent) {
    final ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
    final BackgroundableActionEnabledHandler handler;
    if (actionKey != null) {
      handler = vcsManager.getBackgroundableActionHandler(actionKey);
      // fo not start same action twice
      if (handler.isInProgress(actionParameter)) return;
    } else {
      handler = null;
    }

    final VcsBackgroundableComputable<T> backgroundableComputable =
      new VcsBackgroundableComputable<T>(project, title, errorTitle, backgroundable, awtSuccessContinuation, awtErrorContinuation,
                                  handler, actionParameter);
    backgroundableComputable.setSilent(silent);
    if (handler != null) {
      handler.register(actionParameter);
    }
    ProgressManager.getInstance().run(backgroundableComputable);
  }

  public void run(@Nonnull ProgressIndicator indicator) {
    try {
      myResult = myBackgroundable.compute();
    }
    catch (VcsException e) {
      myException = e;
    }
  }

  @RequiredUIAccess
  @Override
  public void onCancel() {
    commonFinish();
  }

  @RequiredUIAccess
  @Override
  public void onSuccess() {
    commonFinish();
    if (myException == null) {
      if (myAwtSuccessContinuation != null) {
        myAwtSuccessContinuation.accept(myResult);
      }
    } else {
      if (myAwtErrorContinuation != null) {
        myAwtErrorContinuation.run();
      }
    }
  }

  private void commonFinish() {
    if (myHandler != null) {
      myHandler.completed(myActionParameter);
    }
    
    if ((! mySilent) && (myException != null)) {
      AbstractVcsHelperImpl.getInstance(myProject).showError(myException, myErrorTitle);
    }
  }

  public void setSilent(boolean silent) {
    mySilent = silent;
  }
}
