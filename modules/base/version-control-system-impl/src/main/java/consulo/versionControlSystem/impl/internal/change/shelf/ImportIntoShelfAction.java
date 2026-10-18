/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.application.progress.ProgressManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.shelf.ShelvedChangesViewManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelveChangesManagerImpl;
import consulo.versionControlSystem.impl.internal.change.shelf.ShelvedChangeListImpl;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author irengrig
 * @since 2011-02-25
 */
@ActionImpl(id = "ShelvedChanges.ImportPatches")
public class ImportIntoShelfAction extends LegacyDumbAwareAction {
    public ImportIntoShelfAction() {
        super(LocalizeValue.localizeTODO("Import patches…"), LocalizeValue.localizeTODO("Copies patch file to shelf"));
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);
        UIAccess uiAccess = UIAccess.current();
        FileChooser.chooseFiles(descriptor, project, null).whenCompleteAsync((files, error) -> {
            if (error != null) {
                return;
            }

            //gatherPatchFiles
            ProgressManager pm = ProgressManager.getInstance();
            ShelveChangesManagerImpl shelveChangesManager = ShelveChangesManagerImpl.getInstance(project);

            List<VirtualFile> patchTypeFiles = new ArrayList<>();
            boolean filesFound = pm.runProcessWithProgressSynchronously(
                (Runnable) () -> patchTypeFiles.addAll(shelveChangesManager.gatherPatchFiles(Arrays.asList(files))),
                LocalizeValue.localizeTODO("Looking for patch files…"),
                true,
                project
            );
            if (!filesFound || patchTypeFiles.isEmpty()) {
                return;
            }
            if (!patchTypeFiles.equals(files)) {
                LocalizeValue message = patchTypeFiles.size() == 1
                    ? VcsLocalize.shelveImportOnePatchFilePrompt(patchTypeFiles.get(0).getPath())
                    : VcsLocalize.shelveImportPatchesPrompt(patchTypeFiles.size());
                MessageBoxes.yesNo()
                    .title(VcsLocalize.shelveImportPatchesTitle())
                    .text(message)
                    .showAsync(project)
                    .whenCompleteAsync((yes, answerError) -> {
                        if (Boolean.TRUE.equals(yes)) {
                            importPatches(project, shelveChangesManager, patchTypeFiles);
                        }
                    }, uiAccess);
                return;
            }
            importPatches(project, shelveChangesManager, patchTypeFiles);
        }, uiAccess);
    }

    @RequiredUIAccess
    private static void importPatches(Project project, ShelveChangesManagerImpl shelveChangesManager, List<VirtualFile> patchTypeFiles) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> {
                List<VcsException> exceptions = new ArrayList<>();
                List<ShelvedChangeListImpl> lists = shelveChangesManager.importChangeLists(patchTypeFiles, exceptions::add);
                if (!lists.isEmpty()) {
                    ShelvedChangesViewManager.getInstance(project).activateView(lists.get(lists.size() - 1));
                }
                if (!exceptions.isEmpty()) {
                    AbstractVcsHelper.getInstance(project).showErrors(exceptions, "Import patches into shelf");
                }
                if (lists.isEmpty() && exceptions.isEmpty()) {
                    VcsBalloonProblemNotifier.showOverChangesView(project, "No patches found", NotificationType.WARNING);
                }
            },
            LocalizeValue.localizeTODO("Import patches into shelf"),
            true,
            project
        );
    }
}
