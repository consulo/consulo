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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "MaximizeToolWindow")
public class MaximizeToolWindowAction extends AnAction implements DumbAware {
    public MaximizeToolWindowAction() {
        super(ActionLocalize.actionResizetoolwindowmaximizeText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ToolWindow toolWindow = e.getRequiredData(ToolWindow.KEY);
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        manager.setMaximized(toolWindow, !manager.isMaximized(toolWindow));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(true);
        Project project = e.getData(Project.KEY);
        ToolWindow toolWindow = e.getData(ToolWindow.KEY);
        if (project == null || project.isDisposed() || toolWindow == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        e.getPresentation().setTextValue(
            manager.isMaximized(toolWindow)
                ? ActionLocalize.actionResizetoolwindowmaximizeTextAlternative()
                : ActionLocalize.actionResizetoolwindowmaximizeText()
        );
    }
}
