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

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.AbstractVcs;
import consulo.ide.impl.idea.openapi.vcs.ProjectLevelVcsManager;
import consulo.ide.impl.idea.openapi.vcs.diff.DiffProvider;
import consulo.ide.impl.idea.openapi.vcs.diff.RevisionSelector;
import consulo.ide.impl.idea.openapi.vcs.history.VcsRevisionNumber;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author lesya
 */
public class SelectAndCompareWithSelectedRevisionAction extends AbstractVcsAction{
  protected void actionPerformed(VcsContext vcsContext) {

    final VirtualFile file = vcsContext.getSelectedFiles()[0];
    final Project project = vcsContext.getProject();
    final AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) {
      return;
    }
    RevisionSelector selector = vcs.getRevisionSelector();
    final DiffProvider diffProvider = vcs.getDiffProvider();

    if (selector != null) {
      final VcsRevisionNumber vcsRevisionNumber = selector.selectNumber(file);

      if (vcsRevisionNumber != null) {
        DiffActionExecutor.showDiff(diffProvider, vcsRevisionNumber, file, project, VcsBackgroundableActions.COMPARE_WITH);
      }
    }

  }

  

  protected void update(VcsContext vcsContext, Presentation presentation) {
    AbstractShowDiffAction.updateDiffAction(presentation, vcsContext, VcsBackgroundableActions.COMPARE_WITH);
  }

  protected boolean forceSyncUpdate(final AnActionEvent e) {
    return true;
  }
}
