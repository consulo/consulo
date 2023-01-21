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

import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogDataKeys;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.vcs.log.impl.VcsGoToRefComparator;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;

import java.util.Set;

public class GoToHashOrRefAction extends DumbAwareAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    VcsLogUtil.triggerUsage(e);

    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    final VcsLog log = e.getRequiredData(VcsLogDataKeys.VCS_LOG);
    final VcsLogUiImpl logUi = (VcsLogUiImpl)e.getRequiredData(VcsLogDataKeys.VCS_LOG_UI);

    Set<VirtualFile> visibleRoots = VcsLogUtil.getVisibleRoots(logUi);
    GoToHashOrRefPopup popup =
      new GoToHashOrRefPopup(project, logUi.getDataPack().getRefs(), visibleRoots, text -> log.jumpToReference(text),
                                                      vcsRef -> logUi.jumpToCommit(vcsRef.getCommitHash(), vcsRef.getRoot()),
                             logUi.getColorManager(),
                             new VcsGoToRefComparator(logUi.getDataPack().getLogProviders()));
    popup.show(logUi.getTable());
  }

  @Override
  public void update(AnActionEvent e) {
    VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
    VcsLogUi logUi = e.getData(VcsLogDataKeys.VCS_LOG_UI);
    e.getPresentation().setEnabledAndVisible(e.getData(CommonDataKeys.PROJECT) != null && log != null && logUi != null);
  }
}
