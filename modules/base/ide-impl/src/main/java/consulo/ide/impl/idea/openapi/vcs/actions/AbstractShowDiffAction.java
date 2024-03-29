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
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.ide.impl.idea.openapi.vcs.impl.BackgroundableActionEnabledHandler;
import consulo.ide.impl.idea.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.versionControlSystem.action.VcsContext;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

public abstract class AbstractShowDiffAction extends AbstractVcsAction{

  protected void update(VcsContext vcsContext, Presentation presentation) {
    updateDiffAction(presentation, vcsContext, getKey());
  }

  protected static void updateDiffAction(final Presentation presentation, final VcsContext vcsContext,
                                         final VcsBackgroundableActions actionKey) {
    presentation.setEnabled(isEnabled(vcsContext, actionKey) != null);
    presentation.setVisible(isVisible(vcsContext));
  }

  protected boolean forceSyncUpdate(final AnActionEvent e) {
    return true;
  }

  protected abstract VcsBackgroundableActions getKey();

  protected static boolean isVisible(final VcsContext vcsContext) {
    final Project project = vcsContext.getProject();
    if (project == null) return false;
    final AbstractVcs[] vcss = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
    for (AbstractVcs vcs : vcss) {
      if (vcs.getDiffProvider() != null) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  protected static AbstractVcs isEnabled(final VcsContext vcsContext, @Nullable final VcsBackgroundableActions actionKey) {
    if (!(isVisible(vcsContext))) return null;

    final Project project = vcsContext.getProject();
    if (project == null) return null;
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);

    final VirtualFile[] selectedFilePaths = vcsContext.getSelectedFiles();
    if (selectedFilePaths == null || selectedFilePaths.length != 1) return null;

    final VirtualFile selectedFile = selectedFilePaths[0];
    if (selectedFile.isDirectory()) return null;

    if (actionKey != null) {
      final BackgroundableActionEnabledHandler handler = ((ProjectLevelVcsManagerImpl)vcsManager).getBackgroundableActionHandler(actionKey);
      if (handler.isInProgress(VcsBackgroundableActions.keyFrom(selectedFile))) return null;
    }

    final AbstractVcs vcs = vcsManager.getVcsFor(selectedFile);
    if (vcs == null) return null;

    final DiffProvider diffProvider = vcs.getDiffProvider();

    if (diffProvider == null) return null;

    if (AbstractVcs.fileInVcsByFileStatus(project, new FilePathImpl(selectedFile))) {
      return vcs;
    }
    return null;
  }


  protected void actionPerformed(VcsContext vcsContext) {
    final Project project = vcsContext.getProject();
    if (project == null) return;
    if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not " + vcsContext.getActionName() + " now")) return;
    final VirtualFile selectedFile = vcsContext.getSelectedFiles()[0];

    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    final AbstractVcs vcs = vcsManager.getVcsFor(selectedFile);
    final DiffProvider diffProvider = vcs.getDiffProvider();

    final DiffActionExecutor actionExecutor = getExecutor(diffProvider, selectedFile, project);
    actionExecutor.showDiff();
  }

  protected DiffActionExecutor getExecutor(final DiffProvider diffProvider, final VirtualFile selectedFile, final Project project) {
    return new DiffActionExecutor.CompareToCurrentExecutor(diffProvider, selectedFile, project, getKey());
  }
}
