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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.diff.RevisionSelector;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author lesya
 */
@ActionImpl(id = "Compare.Specified")
public class SelectAndCompareWithSelectedRevisionAction extends AbstractVcsAction {
    public SelectAndCompareWithSelectedRevisionAction() {
        super(LocalizeValue.localizeTODO("Com_pare with Specified Revision..."));
    }

    @Override
    @RequiredUIAccess
    protected void actionPerformed(@Nonnull VcsContext vcsContext) {
        VirtualFile file = vcsContext.getSelectedFiles()[0];
        Project project = vcsContext.getProject();
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
        if (vcs == null) {
            return;
        }
        RevisionSelector selector = vcs.getRevisionSelector();
        DiffProvider diffProvider = vcs.getDiffProvider();

        if (selector != null) {
            VcsRevisionNumber vcsRevisionNumber = selector.selectNumber(file);

            if (vcsRevisionNumber != null) {
                DiffActionExecutor.showDiff(diffProvider, vcsRevisionNumber, file, project, VcsBackgroundableActions.COMPARE_WITH);
            }
        }
    }

    @Override
    protected void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation) {
        AbstractShowDiffAction.updateDiffAction(presentation, vcsContext, VcsBackgroundableActions.COMPARE_WITH);
    }

    @Override
    protected boolean forceSyncUpdate(@Nonnull AnActionEvent e) {
        return true;
    }
}
