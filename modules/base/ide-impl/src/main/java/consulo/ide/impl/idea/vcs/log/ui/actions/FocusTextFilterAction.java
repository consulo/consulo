/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogActionPlaces;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.ide.impl.idea.vcs.log.ui.frame.MainFrame;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;

import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = VcsLogActionPlaces.VCS_LOG_FOCUS_TEXT_FILTER)
public class FocusTextFilterAction extends DumbAwareAction {
    public FocusTextFilterAction() {
        super(VersionControlSystemLogLocalize.actionFocusTextFilterText(), VersionControlSystemLogLocalize.actionFocusTextFilterDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY) && e.getData(VcsLogUi.KEY) instanceof VcsLogUiImpl);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        MainFrame mainFrame = ((VcsLogUiImpl) e.getRequiredData(VcsLogUi.KEY)).getMainFrame();
        if (mainFrame.getTextFilter().getTextEditor().hasFocus()) {
            ProjectIdeFocusManager.getInstance(project).requestFocus(mainFrame.getGraphTable(), true);
        }
        else {
            ProjectIdeFocusManager.getInstance(project).requestFocus(mainFrame.getTextFilter(), true);
        }
    }
}
