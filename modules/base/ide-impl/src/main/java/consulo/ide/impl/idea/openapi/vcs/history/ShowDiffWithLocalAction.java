/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.history;

import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.history.DiffFromHistoryHandler;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsHistorySession;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public class ShowDiffWithLocalAction extends DumbAwareAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    if (ChangeListManager.getInstance(project).isFreezedWithNotification(null)) return;

    VcsRevisionNumber currentRevisionNumber = e.getRequiredData(VcsDataKeys.HISTORY_SESSION).getCurrentRevisionNumber();
    VcsFileRevision selectedRevision = e.getRequiredData(VcsDataKeys.VCS_FILE_REVISIONS)[0];
    FilePath filePath = e.getRequiredData(VcsDataKeys.FILE_PATH);

    if (currentRevisionNumber != null && selectedRevision != null) {
      DiffFromHistoryHandler diffHandler =
              ObjectUtil.notNull(e.getRequiredData(VcsDataKeys.HISTORY_PROVIDER).getHistoryDiffHandler(), new StandardDiffFromHistoryHandler());
      diffHandler.showDiffForTwo(project, filePath, selectedRevision, new CurrentRevision(filePath.getVirtualFile(), currentRevisionNumber));
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    VcsFileRevision[] selectedRevisions = e.getData(VcsDataKeys.VCS_FILE_REVISIONS);
    VirtualFile virtualFile = e.getData(VirtualFile.KEY);
    VcsHistorySession historySession = e.getData(VcsDataKeys.HISTORY_SESSION);

    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(
      selectedRevisions != null && selectedRevisions.length == 1 && virtualFile != null
        && historySession != null && historySession.getCurrentRevisionNumber() != null
        && historySession.isContentAvailable(selectedRevisions[0])
        && e.getData(VcsDataKeys.FILE_PATH) != null && e.getData(VcsDataKeys.HISTORY_PROVIDER) != null
        && e.getData(Project.KEY) != null
    );
  }
}
