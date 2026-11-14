/*
 * Copyright 2013-2026 consulo.io
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
package consulo.fileEditor.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.project.startup.PostStartupActivity;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.UIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Offers the changes kept in local copies - of a close while the machine of the files was not connected, or of a crash -
 * when their project opens again.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class OfflineDocumentRestoreActivity implements PostStartupActivity, DumbAware {
    private record Restorable(OfflineDocumentBackups.Backup backup, VirtualFile file) {
    }

    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (!(FileDocumentManager.getInstance() instanceof FileDocumentManagerImpl manager)) {
            return;
        }
        OfflineDocumentBackups backups = manager.getOfflineBackups();

        List<Restorable> restorables = new ArrayList<>();
        for (OfflineDocumentBackups.Backup backup : backups.list()) {
            VirtualFile file = VirtualFileManager.getInstance().refreshAndFindFileByUrl(backup.url());
            if (file != null && ProjectLocator.getInstance().guessProjectForFile(file) == project) {
                restorables.add(new Restorable(backup, file));
            }
        }
        if (restorables.isEmpty()) {
            return;
        }

        NotificationService.getInstance()
            .newWarn(UnsavedFilesNotificationGroupContributor.TARGET_DISCONNECTED)
            .title(FileEditorLocalize.unsavedFilesRestoreTitle())
            .content(FileEditorLocalize.unsavedFilesRestoreContent(restorables.size()))
            .addClosingAction(FileEditorLocalize.unsavedFilesRestoreRestore(), () -> restore(project, manager, restorables))
            .addClosingAction(FileEditorLocalize.unsavedFilesRestoreDiscard(), () -> {
                for (Restorable restorable : restorables) {
                    backups.remove(restorable.backup().url());
                }
            })
            .notify(project);
    }

    private static void restore(Project project, FileDocumentManagerImpl manager, List<Restorable> restorables) {
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(FileEditorLocalize.unsavedFilesRestoreCommand())
            .inWriteAction()
            .run(() -> {
                for (Restorable restorable : restorables) {
                    Document document = manager.getDocument(restorable.file());
                    if (document != null && document.isWritable()) {
                        // the document is unsaved now - it is saved as any other change
                        document.setText(restorable.backup().text());
                    }
                    manager.getOfflineBackups().remove(restorable.backup().url());
                }
            });
    }
}
