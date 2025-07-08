/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import consulo.project.internal.RecentProjectsManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

public class CloseProjectAction extends AnAction implements DumbAware {
    private WelcomeFrameManager myWelcomeFrameManager;
    private final ProjectManager myProjectManager;
    private final RecentProjectsManager myRecentProjectsManager;

    @Inject
    public CloseProjectAction(
        WelcomeFrameManager welcomeFrameManager,
        ProjectManager projectManager,
        RecentProjectsManager recentProjectsManager
    ) {
        myWelcomeFrameManager = welcomeFrameManager;
        myProjectManager = projectManager;
        myRecentProjectsManager = recentProjectsManager;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent event) {
        Project project = event.getRequiredData(Project.KEY);

        myProjectManager.closeAndDisposeAsync(project, UIAccess.current()).doWhenDone(() -> {
            myRecentProjectsManager.updateLastProjectPath();
            myWelcomeFrameManager.showIfNoProjectOpened();
        });
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getData(Project.KEY) != null);
    }
}
