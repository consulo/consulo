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

package consulo.ide.impl.idea.openapi.progress.impl;

import consulo.application.progress.DumbModeAction;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.Task;
import consulo.application.progress.TaskInfo;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindow;
import consulo.ide.impl.idea.openapi.wm.ex.StatusBarEx;
import consulo.ide.impl.idea.openapi.wm.ex.WindowManagerEx;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BackgroundableProcessIndicator extends ProgressWindow {
  protected StatusBarEx myStatusBar;

  private PerformInBackgroundOption myOption;
  private TaskInfo myInfo;

  private boolean myDisposed;
  private DumbModeAction myDumbModeAction = DumbModeAction.NOTHING;

  public BackgroundableProcessIndicator(@Nonnull Task.Backgroundable task) {
    this((Project)task.getProject(), task, task);

    myDumbModeAction = task.getDumbModeAction();
    if (myDumbModeAction == DumbModeAction.CANCEL) {
      task.getProject().getMessageBus().connect(this).subscribe(DumbModeListener.class, new DumbModeListener() {

        @Override
        public void enteredDumbMode() {
          cancel();
        }
      });
    }
  }

  public BackgroundableProcessIndicator(@Nullable final Project project, @Nonnull TaskInfo info, @Nonnull PerformInBackgroundOption option) {
    super(info.isCancellable(), true, project, info.getCancelText());
    setOwnerTask(info);
    myOption = option;
    myInfo = info;
    setTitle(info.getTitle());
    final Project nonDefaultProject = project == null || project.isDisposed() ? null : project.isDefault() ? null : project;
    final IdeFrame frame = ((WindowManagerEx)WindowManager.getInstance()).findFrameFor(nonDefaultProject);
    myStatusBar = frame != null ? (StatusBarEx)frame.getStatusBar() : null;
    myBackgrounded = shouldStartInBackground();
    if (myBackgrounded) {
      doBackground();
    }
  }

  private boolean shouldStartInBackground() {
    return myOption.shouldStartInBackground() && myStatusBar != null;
  }

  public BackgroundableProcessIndicator(Project project,
                                        @Nls final String progressTitle,
                                        @Nonnull PerformInBackgroundOption option,
                                        @Nls final String cancelButtonText,
                                        @Nls final String backgroundStopTooltip, final boolean cancellable) {
    this(project, new TaskInfo() {

      @Override
      @Nonnull
      public String getTitle() {
        return progressTitle;
      }

      @Override
      public String getCancelText() {
        return cancelButtonText;
      }

      @Override
      public String getCancelTooltipText() {
        return backgroundStopTooltip;
      }

      @Override
      public boolean isCancellable() {
        return cancellable;
      }
    }, option);
  }

  /**
   * to remove in IDEA 16
   */
  @Deprecated
  public DumbModeAction getDumbModeAction() {
    return myDumbModeAction;
  }

  @Override
  protected void showDialog() {
    if (myDisposed) return;

    if (shouldStartInBackground()) {
      return;
    }

    super.showDialog();
  }

  @Override
  public void background() {
    if (myDisposed) return;

    myOption.processSentToBackground();
    doBackground();
    super.background();
  }

  private void doBackground() {
    if (myStatusBar != null) { //not welcome screen
      myStatusBar.addProgress(this, myInfo);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    myDisposed = true;
    myInfo = null;
    myStatusBar = null;
    myOption = null;
  }

  @Override
  public boolean isShowing() {
    return isModal() || ! isBackgrounded();
  }

  @Override
  public String toString() {
    return super.toString() + "; task=" + myInfo;
  }
}
