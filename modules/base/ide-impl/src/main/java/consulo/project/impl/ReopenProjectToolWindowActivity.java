/*
 * Copyright 2013-2020 consulo.io
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
package consulo.project.impl;

import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.startup.IdeaStartupActivity;
import consulo.util.lang.EmptyRunnable;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-22
 */
public class ReopenProjectToolWindowActivity implements IdeaStartupActivity.Background, DumbAware {
  @Override
  public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);

    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);

    if (toolWindow != null) {
      uiAccess.give(() -> {
        if (!toolWindow.isActive()) {
          toolWindow.activate(EmptyRunnable.getInstance());
        }
      });
    }
  }
}
