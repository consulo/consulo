/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.versionControlSystem.change.shelf;

import consulo.annotation.component.ActionImpl;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.ApplyPatchExecutor;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.ApplyPatchMode;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelvedChangeList;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelvedChangesViewManager;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.util.Collections;

/**
 * @author irengrig
 * Date: 2/25/11
 * Time: 5:50 PM
 */
@ActionImpl(id = "ShelveChanges.UnshelveWithDialog")
public class UnshelveWithDialogAction extends AnAction {
  public UnshelveWithDialogAction() {
    super("Unshelve...", "Correct paths where to apply patches and unshelve", null);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    final ShelvedChangeList[] changeLists = e.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    if (project == null || changeLists == null || changeLists.length != 1) return;

    FileDocumentManager.getInstance().saveAllDocuments();

    final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(changeLists[0].PATH));
    if (virtualFile == null) {
      VcsBalloonProblemNotifier.showOverChangesView(project, "Can not find path file", NotificationType.ERROR);
      return;
    }
    if (!changeLists[0].getBinaryFiles().isEmpty()) {
      VcsBalloonProblemNotifier.showOverChangesView(project, "Binary file(s) would be skipped.", NotificationType.WARNING);
    }
    final ApplyPatchDifferentiatedDialog dialog = new ApplyPatchDifferentiatedDialog(
      project,
      new ApplyPatchDefaultExecutor(project),
      Collections.<ApplyPatchExecutor>emptyList(),
      ApplyPatchMode.UNSHELVE,
      virtualFile
    );
    dialog.setHelpId("reference.dialogs.vcs.unshelve");
    dialog.show();
  }

  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    final ShelvedChangeList[] changes = e.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    e.getPresentation().setEnabled(project != null && changes != null && changes.length == 1);
  }
}
