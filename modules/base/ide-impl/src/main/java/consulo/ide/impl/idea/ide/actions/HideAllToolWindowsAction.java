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

import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ide.impl.idea.openapi.wm.ex.ToolWindowManagerEx;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

public class HideAllToolWindowsAction extends AnAction implements DumbAware {
  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }

    performAction(project);
  }

  @RequiredUIAccess
  public static void performAction(final Project project) {
    ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(project);

    ToolWindowLayout layout = new ToolWindowLayout();
    layout.copyFrom(toolWindowManager.getLayout());

    // to clear windows stack
    toolWindowManager.clearSideStack();
    //toolWindowManager.activateEditorComponent();


    String[] ids = toolWindowManager.getToolWindowIds();
    boolean hasVisible = false;
    for (String id : ids) {
      ToolWindow toolWindow = toolWindowManager.getToolWindow(id);
      if (toolWindow.isVisible()) {
        toolWindow.hide(null);
        hasVisible = true;
      }
    }

    if (hasVisible) {
      toolWindowManager.setLayoutToRestoreLater(layout);
      toolWindowManager.activateEditorComponent();
    }
    else {
      final ToolWindowLayout restoredLayout = toolWindowManager.getLayoutToRestoreLater();
      if (restoredLayout != null) {
        toolWindowManager.setLayoutToRestoreLater(null);
        toolWindowManager.setLayout(restoredLayout);
      }
    }
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    Project project = event.getData(Project.KEY);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(project);
    String[] ids = toolWindowManager.getToolWindowIds();
    for (String id : ids) {
      if (toolWindowManager.getToolWindow(id).isVisible()) {
        presentation.setEnabled(true);
        presentation.setText(IdeLocalize.actionHideAllWindows().get(), true);
        return;
      }
    }

    final ToolWindowLayout layout = toolWindowManager.getLayoutToRestoreLater();
    if (layout != null) {
      presentation.setEnabled(true);
      presentation.setTextValue(IdeLocalize.actionRestoreWindows());
      return;
    }

    presentation.setEnabled(false);
  }
}
