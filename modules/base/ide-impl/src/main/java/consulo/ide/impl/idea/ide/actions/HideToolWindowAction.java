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
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.toolWindow.ToolWindow;

public class HideToolWindowAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ToolWindowManagerEx toolWindowManager = ToolWindowManagerEx.getInstanceEx(project);
        String id = toolWindowManager.getActiveToolWindowId();
        if (id == null) {
            id = toolWindowManager.getLastActiveToolWindowId();
        }
        toolWindowManager.getToolWindow(id).hide(null);
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
        String id = toolWindowManager.getActiveToolWindowId();
        if (id != null) {
            presentation.setEnabled(true);
            return;
        }

        id = toolWindowManager.getLastActiveToolWindowId();
        if (id == null) {
            presentation.setEnabled(false);
            return;
        }

        ToolWindow toolWindow = toolWindowManager.getToolWindow(id);
        presentation.setEnabled(toolWindow.isVisible());
    }
}