
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
package consulo.project.ui.impl.internal.wm.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "JumpToLastWindow")
public class JumpToLastWindowAction extends AnAction implements DumbAware {
    public JumpToLastWindowAction() {
        super(ActionLocalize.actionJumptolastwindowText(), ActionLocalize.actionJumptolastwindowDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);
        String id = manager.getLastActiveToolWindowId();
        if (id == null || !manager.getToolWindow(id).isAvailable()) {
            return;
        }
        manager.getToolWindow(id).activate(null);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        ToolWindowManagerEx manager = (ToolWindowManagerEx) ToolWindowManager.getInstance(project);
        String id = manager.getLastActiveToolWindowId();
        presentation.setEnabled(id != null && manager.getToolWindow(id).isAvailable());
    }
}
