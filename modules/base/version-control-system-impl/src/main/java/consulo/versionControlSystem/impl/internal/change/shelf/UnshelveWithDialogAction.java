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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.annotation.component.ActionImpl;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchDefaultExecutor;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchDifferentiatedDialog;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchMode;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.util.List;

/**
 * @author irengrig
 * @since 2011-02-25
 */
@ActionImpl(id = "ShelveChanges.UnshelveWithDialog")
public class UnshelveWithDialogAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public UnshelveWithDialogAction() {
        super(VcsLocalize.actionUnshelveWithDialogText(), VcsLocalize.actionUnshelveWithDialogDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ShelvedChangeListImpl[] changeLists = e.getRequiredData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        if (changeLists.length != 1) {
            return;
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        ShelvedChangeListImpl changeList = changeLists[0];
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(changeList.PATH));
        if (virtualFile == null) {
            VcsBalloonProblemNotifier.showOverChangesView(
                project,
                VcsLocalize.patchApplyCannotFindPatchFileWarning(changeList.PATH).get(),
                NotificationType.ERROR
            );
            return;
        }
        if (!changeList.getBinaryFiles().isEmpty()) {
            VcsBalloonProblemNotifier.showOverChangesView(project, VcsLocalize.shelveBinaryFilesSkippedWarning().get(), NotificationType.WARNING);
        }

        ApplyPatchDifferentiatedDialog dialog = new ApplyPatchDifferentiatedDialog(
            project,
            new ApplyPatchDefaultExecutor(project),
            List.of(),
            ApplyPatchMode.UNSHELVE,
            virtualFile,
            null,
            ChangeListManager.getInstance(project).findChangeList(changeList.DESCRIPTION),
            null,
            null,
            changeList.DESCRIPTION,
            false
        );
        dialog.setHelpId("reference.dialogs.vcs.unshelve");
        dialog.setOKButtonText(VcsLocalize.unshelveChangesAction());
        dialog.showAsync();
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        ShelvedChangeListImpl[] changes = e.getData(ShelvedChangesViewManagerImpl.SHELVED_CHANGELIST_KEY);
        e.getPresentation().setEnabled(project != null && changes != null && changes.length == 1);
    }
}
