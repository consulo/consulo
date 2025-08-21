/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.versionControlSystem.VcsToolWindow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.util.lang.ObjectUtil.assertNotNull;

public abstract class VcsShowToolWindowTabAction extends DumbAwareAction {
    protected VcsShowToolWindowTabAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ToolWindow toolWindow = assertNotNull(getToolWindow(project));
        ChangesViewContentManager changesViewContentManager =
            (ChangesViewContentManager) ChangesViewContentManager.getInstance(project);
        String tabName = getTabName();

        boolean contentAlreadySelected = changesViewContentManager.isContentSelected(tabName);
        if (toolWindow.isActive() && contentAlreadySelected) {
            toolWindow.hide(null);
        }
        else {
            Runnable runnable = contentAlreadySelected ? null : () -> changesViewContentManager.selectContent(tabName, true);
            toolWindow.activate(runnable, true, true);
        }
    }

    @Nullable
    private static ToolWindow getToolWindow(@Nullable Project project) {
        if (project == null) {
            return null;
        }
        return ToolWindowManager.getInstance(project).getToolWindow(VcsToolWindow.ID);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(getToolWindow(e.getData(Project.KEY)) != null);
    }

    @Nonnull
    protected abstract String getTabName();
}
