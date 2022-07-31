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

import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.vcs.FilePath;
import consulo.ide.impl.idea.openapi.vcs.VcsDataKeys;
import consulo.vcs.change.ChangeListManager;
import consulo.vcs.history.DiffFromHistoryHandler;
import consulo.vcs.history.VcsFileRevision;
import consulo.vcs.history.VcsHistorySession;
import consulo.vcs.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.ObjectUtil;
import javax.annotation.Nonnull;

public class ShowDiffWithLocalAction extends DumbAwareAction {

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
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
    VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    VcsHistorySession historySession = e.getData(VcsDataKeys.HISTORY_SESSION);

    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(selectedRevisions != null && selectedRevisions.length == 1 && virtualFile != null &&
                                   historySession != null && historySession.getCurrentRevisionNumber() != null &&
                                   historySession.isContentAvailable(selectedRevisions[0]) &&
                                   e.getData(VcsDataKeys.FILE_PATH) != null && e.getData(VcsDataKeys.HISTORY_PROVIDER) != null &&
                                   e.getData(CommonDataKeys.PROJECT) != null);
  }

}
