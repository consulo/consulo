/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;

/**
 * Shows a tool window balloon and a status bar balloon, each run with the next notification type.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
@ActionImpl(id = "TestBalloonsAction", parents = @ActionParentRef(@ActionRef(id = IdeActions.TOOLS_MENU)))
public class TestBalloonsAction extends DumbAwareAction {
    private int myIndex;

    public TestBalloonsAction() {
        super(LocalizeValue.localizeTODO("Test Balloons"));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        NotificationType[] types = NotificationType.values();
        NotificationType type = types[myIndex++ % types.length];

        ToolWindowManager.getInstance(project).notifyByBalloon(
            ToolWindowId.PROJECT_VIEW,
            type,
            "Project view says <b>" + type + "</b>.\nSecond line with a <a href=\"tw\">link</a>.",
            null,
            null
        );

        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.notifyByBalloon(
                StatusBar.PROGRESS_WIDGET_ID,
                type,
                "Status bar says <b>" + type + "</b>. <a href=\"sb\">Browse</a>",
                null,
                null
            );
        }
    }
}
