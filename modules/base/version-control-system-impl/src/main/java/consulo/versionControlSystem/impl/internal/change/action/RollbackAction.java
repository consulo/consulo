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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.document.FileDocumentManager;
import consulo.versionControlSystem.impl.internal.action.RollbackDeletionAction;
import consulo.versionControlSystem.impl.internal.change.ui.awt.RollbackChangesDialog;
import consulo.versionControlSystem.impl.internal.change.ui.RollbackProgressModifier;
import consulo.versionControlSystem.impl.internal.util.RollbackUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Streams;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListViewImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author yole
 * @since 2006-11-02
 */
@ActionImpl(id = "ChangesView.Revert")
public class RollbackAction extends AnAction implements DumbAware {
    public RollbackAction() {
        super(
            ActionLocalize.actionChangesviewRevertText(),
            ActionLocalize.actionChangesviewRevertDescription(),
            PlatformIconGroup.actionsRollback()
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        boolean visible = project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss();
        e.getPresentation().setEnabledAndVisible(visible);
        if (!visible) {
            return;
        }

        Change[] leadSelection = e.getData(VcsDataKeys.CHANGE_LEAD_SELECTION);
        boolean isEnabled = (leadSelection != null && leadSelection.length > 0)
            || Boolean.TRUE.equals(e.getData(VcsDataKeys.HAVE_LOCALLY_DELETED))
            || Boolean.TRUE.equals(e.getData(VcsDataKeys.HAVE_MODIFIED_WITHOUT_EDITING))
            || Boolean.TRUE.equals(e.getData(VcsDataKeys.HAVE_SELECTED_CHANGES))
            || ReadAction.compute(() -> hasReversibleFiles(e))
            || currentChangelistNotEmpty(project);
        e.getPresentation().setEnabled(isEnabled);
        String operationName = RollbackUtil.getRollbackOperationName(project);
        e.getPresentation().setText(operationName + "...");
        if (isEnabled) {
            e.getPresentation().setDescription(UIUtil.removeMnemonic(operationName) + " selected changes");
        }
    }

    @RequiredReadAction
    private static boolean hasReversibleFiles(@Nonnull AnActionEvent e) {
        ChangeListManager manager = ChangeListManager.getInstance(e.getRequiredData(Project.KEY));
        Set<VirtualFile> modifiedWithoutEditing = new HashSet<>(manager.getModifiedWithoutEditing());

        return Streams.notNullize(e.getData(VcsDataKeys.VIRTUAL_FILE_STREAM)).anyMatch(
            file -> manager.haveChangesUnder(file) != ThreeState.NO
                || manager.isFileAffected(file)
                || modifiedWithoutEditing.contains(file)
        );
    }

    private static boolean currentChangelistNotEmpty(Project project) {
        ChangeListManager clManager = ChangeListManager.getInstance(project);
        ChangeList list = clManager.getDefaultChangeList();
        return list != null && !list.getChanges().isEmpty();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        String title = ActionPlaces.CHANGES_VIEW_TOOLBAR.equals(e.getPlace())
            ? null : "Can not " + RollbackUtil.getRollbackOperationName(project) + " now";
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(title)) {
            return;
        }
        FileDocumentManager.getInstance().saveAllDocuments();

        List<FilePath> missingFiles = e.getData(ChangesListViewImpl.MISSING_FILES_DATA_KEY);
        boolean hasChanges = false;
        if (missingFiles != null && !missingFiles.isEmpty()) {
            hasChanges = true;
            new RollbackDeletionAction().actionPerformed(e);
        }

        List<Change> changes = getChanges(project, e);

        Set<VirtualFile> modifiedWithoutEditing = getModifiedWithoutEditing(e, project);
        if (modifiedWithoutEditing != null && !modifiedWithoutEditing.isEmpty()) {
            hasChanges = true;
            rollbackModifiedWithoutEditing(project, modifiedWithoutEditing);
        }

        if (modifiedWithoutEditing != null) {
            changes = ContainerUtil.filter(changes, change -> !modifiedWithoutEditing.contains(change.getVirtualFile()));
        }

        if (!changes.isEmpty()) {
            RollbackChangesDialog.rollbackChanges(project, changes);
        }
        else if (!hasChanges) {
            LocalChangeList currentChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
            RollbackChangesDialog.rollbackChanges(project, currentChangeList);
        }
    }

    @Nonnull
    private static List<Change> getChanges(Project project, AnActionEvent e) {
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (changes == null) {
            VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
            if (files != null) {
                ChangeListManager clManager = ChangeListManager.getInstance(project);
                List<Change> changesList = new ArrayList<>();
                for (VirtualFile vf : files) {
                    changesList.addAll(clManager.getChangesIn(vf));
                }
                if (!changesList.isEmpty()) {
                    changes = changesList.toArray(new Change[changesList.size()]);
                }
            }
        }
        if (changes != null && changes.length > 0) {
            return List.of(changes);
        }
        return Collections.emptyList();
    }

    @Nullable
    private static Set<VirtualFile> getModifiedWithoutEditing(AnActionEvent e, Project project) {
        List<VirtualFile> modifiedWithoutEditing = e.getData(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY);
        if (modifiedWithoutEditing != null && modifiedWithoutEditing.size() > 0) {
            return new LinkedHashSet<>(modifiedWithoutEditing);
        }

        VirtualFile[] virtualFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (virtualFiles != null && virtualFiles.length > 0) {
            Set<VirtualFile> result = new LinkedHashSet<>(Arrays.asList(virtualFiles));
            result.retainAll(ChangeListManager.getInstance(project).getModifiedWithoutEditing());
            return result;
        }

        return null;
    }

    @RequiredUIAccess
    private static void rollbackModifiedWithoutEditing(Project project, Set<VirtualFile> modifiedWithoutEditing) {
        String operationName = StringUtil.decapitalize(UIUtil.removeMnemonic(RollbackUtil.getRollbackOperationName(project)));
        LocalizeValue message = modifiedWithoutEditing.size() == 1
            ? VcsLocalize.rollbackModifiedWithoutEditingConfirmSingle(
            operationName,
            modifiedWithoutEditing.iterator().next().getPresentableUrl()
        )
            : VcsLocalize.rollbackModifiedWithoutEditingConfirmMultiple(
            operationName,
            modifiedWithoutEditing.size()
        );
        int rc = Messages.showYesNoDialog(
            project,
            message.get(),
            VcsLocalize.changesActionRollbackTitle(operationName).get(),
            UIUtil.getQuestionIcon()
        );
        if (rc != Messages.YES) {
            return;
        }
        List<VcsException> exceptions = new ArrayList<>();

        ProgressManager progressManager = ProgressManager.getInstance();
        Runnable action = () -> {
            ProgressIndicator indicator = progressManager.getProgressIndicator();
            try {
                ChangesUtil.processVirtualFilesByVcs(project, modifiedWithoutEditing, (vcs, items) -> {
                    RollbackEnvironment rollbackEnvironment = vcs.getRollbackEnvironment();
                    if (rollbackEnvironment != null) {
                        if (indicator != null) {
                            indicator.setText(
                                vcs.getDisplayName() + ": performing " +
                                    UIUtil.removeMnemonic(rollbackEnvironment.getRollbackOperationName()).toLowerCase() + "..."
                            );
                            indicator.setIndeterminate(false);
                        }
                        rollbackEnvironment.rollbackModifiedWithoutCheckout(
                            items,
                            exceptions,
                            new RollbackProgressModifier(items.size(), indicator)
                        );
                        if (indicator != null) {
                            indicator.setText2Value(LocalizeValue.empty());
                        }
                    }
                });
            }
            catch (ProcessCanceledException e) {
                // for files refresh
            }
            if (!exceptions.isEmpty()) {
                AbstractVcsHelper.getInstance(project).showErrors(
                    exceptions,
                    VcsLocalize.rollbackModifiedWithoutCheckoutErrorTab(operationName)
                );
            }

            VirtualFileUtil.markDirty(true, false, VirtualFileUtil.toVirtualFileArray(modifiedWithoutEditing));

            VirtualFileManager.getInstance().asyncRefresh(() -> {
                for (VirtualFile virtualFile : modifiedWithoutEditing) {
                    VcsDirtyScopeManager.getInstance(project).fileDirty(virtualFile);
                }
            });
        };
        progressManager.runProcessWithProgressSynchronously(action, operationName, true, project);
    }
}
