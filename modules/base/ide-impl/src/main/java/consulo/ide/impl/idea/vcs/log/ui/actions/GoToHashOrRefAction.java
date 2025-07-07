/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.ide.impl.idea.vcs.log.impl.VcsGoToRefComparator;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Set;

public class GoToHashOrRefAction extends DumbAwareAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    VcsLogUtil.triggerUsage(e);

    Project project = e.getRequiredData(Project.KEY);
    final VcsLog log = e.getRequiredData(VcsLog.KEY);
    final VcsLogUiImpl logUi = (VcsLogUiImpl)e.getRequiredData(VcsLogUi.KEY);

    Set<VirtualFile> visibleRoots = VcsLogUtil.getVisibleRoots(logUi);
    GoToHashOrRefPopup popup = new GoToHashOrRefPopup(
      project,
      logUi.getDataPack().getRefs(),
      visibleRoots,
      log::jumpToReference,
      vcsRef -> logUi.jumpToCommit(vcsRef.getCommitHash(), vcsRef.getRoot()),
      logUi.getColorManager(),
      new VcsGoToRefComparator(logUi.getDataPack().getLogProviders())
    );
    popup.show(logUi.getTable());
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    VcsLog log = e.getData(VcsLog.KEY);
    VcsLogUi logUi = e.getData(VcsLogUi.KEY);
    e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && log != null && logUi != null);
  }
}
