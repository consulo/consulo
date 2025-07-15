/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class ToggleLaggingModeAction extends AnAction implements DumbAware {
  private volatile boolean myLagging = false;
  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD); 

  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    if (myLagging) {
      myLagging = false;
      myAlarm.cancelAllRequests();
    }
    else {
      myLagging = true;
      for (int i = 0; i < 100; i++) {
        new Runnable() {
          @Override
          public void run() {
            myAlarm.addRequest(this, 1);
          }
        }.run();
      }
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Project project = e.getData(Project.KEY);
    presentation.setEnabled(project != null && myLagging == DumbService.getInstance(project).isDumb());
    presentation.setText(myLagging ? "Exit lagging mode" : "Enter lagging mode");
  }
}
