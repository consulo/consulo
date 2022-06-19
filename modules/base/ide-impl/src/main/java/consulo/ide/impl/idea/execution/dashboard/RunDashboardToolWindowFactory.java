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
package consulo.ide.impl.idea.execution.dashboard;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.lang.function.Condition;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author konstantin.aleev
 */
public class RunDashboardToolWindowFactory implements ToolWindowFactory, Condition<Project>, DumbAware {
  @Override
  public boolean value(Project project) {
    return RunDashboardManager.getInstance(project).isToolWindowAvailable();
  }

  @Nonnull
  @Override
  public String getId() {
    return ToolWindowId.RUN_DASHBOARD;
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    RunDashboardManager.getInstance(project).createToolWindowContent(toolWindow);
  }

  @Nonnull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.BOTTOM;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.toolwindowsToolwindowrun();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Run Dashboard");
  }
}
