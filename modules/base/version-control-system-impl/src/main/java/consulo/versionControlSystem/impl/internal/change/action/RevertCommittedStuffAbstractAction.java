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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.project.util.WaitForProgressToShow;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.patch.BinaryFilePatch;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.IdeaTextPatchBuilder;
import consulo.versionControlSystem.impl.internal.change.ChangesPreprocess;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangeListChooser;
import consulo.versionControlSystem.impl.internal.patch.apply.PatchApplier;
import consulo.versionControlSystem.internal.BackgroundFromStartOption;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

abstract class RevertCommittedStuffAbstractAction extends AnAction implements DumbAware {
    private final Function<AnActionEvent, Change[]> myForUpdateConvertor;
    private final Function<AnActionEvent, Change[]> myForPerformConvertor;

    public RevertCommittedStuffAbstractAction(
        Function<AnActionEvent, Change[]> forUpdateConvertor,
        Function<AnActionEvent, Change[]> forPerformConvertor
    ) {
        myForUpdateConvertor = forUpdateConvertor;
        myForPerformConvertor = forPerformConvertor;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getRequiredData(Project.KEY);
        final VirtualFile baseDir = project.getBaseDir();
        assert baseDir != null;
        Change[] changes = myForPerformConvertor.apply(e);
        if (changes == null || changes.length == 0) {
            return;
        }
        final List<Change> changesList = new ArrayList<>();
        Collections.addAll(changesList, changes);
        FileDocumentManager.getInstance().saveAllDocuments();

        String defaultName = null;
        ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
        if (changeLists != null && changeLists.length > 0) {
            defaultName = VcsLocalize.revertChangesDefaultName(changeLists[0].getName()).get();
        }

        final ChangeListChooser chooser = new ChangeListChooser(
            project,
            ChangeListManager.getInstance(project).getChangeListsCopy(),
            null,
            "Select Target Changelist",
            defaultName
        );
        chooser.show();
        if (!chooser.isOK()) {
            return;
        }

        final List<FilePatch> patches = new ArrayList<>();
        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            VcsLocalize.revertChangesTitle().get(),
            true,
            BackgroundFromStartOption.getInstance()
        ) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                try {
                    List<Change> preprocessed = ChangesPreprocess.preprocessChangesRemoveDeletedForDuplicateMoved(changesList);
                    patches.addAll(IdeaTextPatchBuilder.buildPatch(project, preprocessed, baseDir.getPresentableUrl(), true));
                }
                catch (VcsException ex) {
                    WaitForProgressToShow.runOrInvokeLaterAboveProgress(
                        () -> Messages.showErrorDialog(
                            project,
                            "Failed to revert changes: " + ex.getMessage(),
                            VcsLocalize.revertChangesTitle().get()
                        ),
                        null,
                        (Project) myProject
                    );
                    indicator.cancel();
                }
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                new PatchApplier<BinaryFilePatch>(project, baseDir, patches, chooser.getSelectedList(), null, null).execute();
            }
        });
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Change[] changes = myForUpdateConvertor.apply(e);
        e.getPresentation().setEnabled(e.hasData(Project.KEY) && changes != null && changes.length > 0);
    }
}
