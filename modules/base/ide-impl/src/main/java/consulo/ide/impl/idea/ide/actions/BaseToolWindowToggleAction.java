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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.wm.ex.ToolWindowManagerEx;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;

public abstract class BaseToolWindowToggleAction extends ToggleAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public final boolean isSelected(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null || project.isDisposed()) {
      return false;
    }
    ToolWindowManager mgr = ToolWindowManager.getInstance(project);
    String id = mgr.getActiveToolWindowId();
    return id != null && isSelected(mgr.getToolWindow(id));
  }

  protected abstract boolean isSelected(ToolWindow window);

  @Override
  @RequiredUIAccess
  public final void setSelected(AnActionEvent e, boolean state) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }
    String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
    if (id == null) {
      return;
    }

    ToolWindowManagerEx mgr = ToolWindowManagerEx.getInstanceEx(project);
    ToolWindowEx toolWindow = (ToolWindowEx)mgr.getToolWindow(id);

    setSelected(toolWindow, state);
  }

  protected abstract void setSelected(ToolWindow window, boolean state);

  @Override
  @RequiredUIAccess
  public final void update(@Nonnull AnActionEvent e) {
    super.update(e);
    Presentation presentation = e.getPresentation();
    Project project = e.getData(Project.KEY);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    ToolWindowManager mgr = ToolWindowManager.getInstance(project);
    String id = mgr.getActiveToolWindowId();

    if (id == null) {
      presentation.setEnabled(false);
      return;
    }

    ToolWindow window = mgr.getToolWindow(id);

    if (window == null) {
      presentation.setEnabled(false);
      return;
    }

    update(window, presentation);
  }

  protected abstract void update(ToolWindow window, Presentation presentation);
}
