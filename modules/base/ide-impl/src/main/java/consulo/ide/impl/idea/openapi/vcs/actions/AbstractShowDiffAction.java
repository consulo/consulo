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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class AbstractShowDiffAction extends AbstractVcsAction{

  @Override
  protected void update(@Nonnull VcsContext vcsContext, @Nonnull Presentation presentation) {
    updateDiffAction(presentation, vcsContext, getKey());
  }

  protected static void updateDiffAction(Presentation presentation, VcsContext vcsContext,
                                         VcsBackgroundableActions actionKey) {
    presentation.setEnabled(isEnabled(vcsContext, actionKey) != null);
    presentation.setVisible(isVisible(vcsContext));
  }

  @Override
  protected boolean forceSyncUpdate(AnActionEvent e) {
    return true;
  }

  protected abstract VcsBackgroundableActions getKey();

  protected static boolean isVisible(VcsContext vcsContext) {
    Project project = vcsContext.getProject();
    if (project == null) return false;
    AbstractVcs[] vcss = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
    for (AbstractVcs vcs : vcss) {
      if (vcs.getDiffProvider() != null) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  protected static AbstractVcs isEnabled(VcsContext vcsContext, @Nullable VcsBackgroundableActions actionKey) {
    if (!(isVisible(vcsContext))) return null;

    Project project = vcsContext.getProject();
    if (project == null) return null;
    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);

    VirtualFile[] selectedFilePaths = vcsContext.getSelectedFiles();
    if (selectedFilePaths.length != 1) return null;

    VirtualFile selectedFile = selectedFilePaths[0];
    if (selectedFile.isDirectory()) return null;

    if (actionKey != null) {
      BackgroundableActionEnabledHandler handler = ((ProjectLevelVcsManagerImpl)vcsManager).getBackgroundableActionHandler(actionKey);
      if (handler.isInProgress(VcsBackgroundableActions.keyFrom(selectedFile))) return null;
    }

    AbstractVcs vcs = vcsManager.getVcsFor(selectedFile);
    if (vcs == null) return null;

    DiffProvider diffProvider = vcs.getDiffProvider();

    if (diffProvider == null) return null;

    if (AbstractVcs.fileInVcsByFileStatus(project, new FilePathImpl(selectedFile))) {
      return vcs;
    }
    return null;
  }


  @Override
  protected void actionPerformed(VcsContext vcsContext) {
    Project project = vcsContext.getProject();
    if (project == null) return;
    if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not " + vcsContext.getActionName() + " now")) return;
    VirtualFile selectedFile = vcsContext.getSelectedFiles()[0];

    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    AbstractVcs vcs = vcsManager.getVcsFor(selectedFile);
    DiffProvider diffProvider = vcs.getDiffProvider();

    DiffActionExecutor actionExecutor = getExecutor(diffProvider, selectedFile, project);
    actionExecutor.showDiff();
  }

  protected DiffActionExecutor getExecutor(DiffProvider diffProvider, VirtualFile selectedFile, Project project) {
    return new DiffActionExecutor.CompareToCurrentExecutor(diffProvider, selectedFile, project, getKey());
  }
}
