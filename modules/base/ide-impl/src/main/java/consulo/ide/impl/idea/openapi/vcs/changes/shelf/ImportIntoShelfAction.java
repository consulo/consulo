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
package consulo.ide.impl.idea.openapi.vcs.changes.shelf;

import consulo.annotation.component.ActionImpl;
import consulo.application.progress.ProgressManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author irengrig
 * @since 2011-02-25
 */
@ActionImpl(id = "ShelvedChanges.ImportPatches")
public class ImportIntoShelfAction extends DumbAwareAction {
    public ImportIntoShelfAction() {
        super(LocalizeValue.localizeTODO("Import patches..."), LocalizeValue.localizeTODO("Copies patch file to shelf"));
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);
        FileChooser.chooseFiles(descriptor, project, null).doWhenDone(files -> {
            //gatherPatchFiles
            ProgressManager pm = ProgressManager.getInstance();
            ShelveChangesManager shelveChangesManager = ShelveChangesManager.getInstance(project);

            List<VirtualFile> patchTypeFiles = new ArrayList<>();
            boolean filesFound = pm.runProcessWithProgressSynchronously(
                (Runnable) () -> patchTypeFiles.addAll(shelveChangesManager.gatherPatchFiles(Arrays.asList(files))),
                LocalizeValue.localizeTODO("Looking for patch files..."),
                true,
                project
            );
            if (!filesFound || patchTypeFiles.isEmpty()) {
                return;
            }
            if (!patchTypeFiles.equals(files)) {
                String message = "Found " + (
                    patchTypeFiles.size() == 1
                        ? "one patch file (" + patchTypeFiles.get(0).getPath() + ")."
                        : patchTypeFiles.size() + " patch files."
                ) + "\nContinue with import?";
                int toImport = Messages.showYesNoDialog(project, message, "Import Patches", UIUtil.getQuestionIcon());
                if (toImport == Messages.NO) {
                    return;
                }
            }
            pm.runProcessWithProgressSynchronously(
                () -> {
                    List<VcsException> exceptions = new ArrayList<>();
                    List<ShelvedChangeList> lists = shelveChangesManager.importChangeLists(patchTypeFiles, exceptions::add);
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
        });
    }
}
