/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.ide.impl.idea.tasks.actions;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.webBrowser.BrowserUtil;
import consulo.project.Project;
import consulo.task.TaskManager;
import consulo.task.impl.internal.action.BaseTaskAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "tasks.open.in.browser")
public class OpenTaskInBrowserAction extends BaseTaskAction {
    public OpenTaskInBrowserAction() {
        super(LocalizeValue.localizeTODO("Open in _Browser"));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        String url = getIssueUrl(e);
        if (url != null) {
            BrowserUtil.launchBrowser(url);
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        if (e.getPresentation().isEnabled()) {
            Presentation presentation = e.getPresentation();
            String url = getIssueUrl(e);
            presentation.setEnabled(url != null);
            Project project = e.getData(Project.KEY);
            if (project == null || !TaskManager.getManager(project).getActiveTask().isIssue()) {
                presentation.setTextValue(getTemplatePresentation().getTextValue());
            }
            else {
                presentation.setText("Open '" + TaskManager.getManager(project).getActiveTask().getPresentableName() + "' In _Browser");
            }
        }
    }

    @Nullable
    private static String getIssueUrl(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        return project == null ? null : TaskManager.getManager(project).getActiveTask().getIssueUrl();
    }
}
