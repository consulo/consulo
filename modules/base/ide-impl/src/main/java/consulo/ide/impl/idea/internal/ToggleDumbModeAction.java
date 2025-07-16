/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.internal;

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class ToggleDumbModeAction extends AnAction implements DumbAware {
  private volatile boolean myDumb = false;

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (myDumb) {
      myDumb = false;
    }
    else {
      myDumb = true;
      final Project project = e.getRequiredData(Project.KEY);

      DumbService.getInstance(project).queueTask(new DumbModeTask() {
        @Override
        public void performInDumbMode(@Nonnull ProgressIndicator indicator, Exception trace) {
          while (myDumb) {
            indicator.checkCanceled();
            TimeoutUtil.sleep(100);
          }
        }
      });
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Project project = e.getData(Project.KEY);
    presentation.setEnabled(project != null && myDumb == DumbService.getInstance(project).isDumb());
    presentation.setText(myDumb ? "Exit Dumb Mode" : "Enter Dumb Mode");
  }
}
