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
package consulo.ide.impl.idea.openapi.diff.impl.patch.formove;

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Computable;
import consulo.ide.impl.idea.openapi.diff.impl.patch.ApplyPatchContext;
import consulo.ide.impl.idea.openapi.diff.impl.patch.ApplyPatchStatus;
import consulo.ide.impl.idea.openapi.diff.impl.patch.FilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.ApplyFilePatchBase;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.ApplyTextFilePatch;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.ApplyPatchAction;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.localHistory.Label;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.project.util.WaitForProgressToShow;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.internal.VcsFileListenerContextHelper;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * for patches. for shelve.
 */
public class PatchApplier<BinaryType extends FilePatch> {
    private final Project myProject;
    private final VirtualFile myBaseDirectory;
    @Nonnull
    private final List<FilePatch> myPatches;
    private final CustomBinaryPatchApplier<BinaryType> myCustomForBinaries;
    private final CommitContext myCommitContext;
    private final Consumer<Collection<FilePath>> myToTargetListsMover;
    @Nonnull
    private final List<FilePatch> myRemainingPatches;
    @Nonnull
    private final List<FilePatch> myFailedPatches;
    private final PathsVerifier<BinaryType> myVerifier;
    private boolean mySystemOperation;

    private final boolean myReverseConflict;
    @Nullable
    private final String myLeftConflictPanelTitle;
    @Nullable
    private final String myRightConflictPanelTitle;

    public PatchApplier(
        @Nonnull Project project,
        final VirtualFile baseDirectory,
        @Nonnull final List<FilePatch> patches,
        @Nullable final Consumer<Collection<FilePath>> toTargetListsMover,
        final CustomBinaryPatchApplier<BinaryType> customForBinaries,
        final CommitContext commitContext,
        boolean reverseConflict,
        @Nullable String leftConflictPanelTitle,
        @Nullable String rightConflictPanelTitle
    ) {
        myProject = project;
        myBaseDirectory = baseDirectory;
        myPatches = patches;
        myToTargetListsMover = toTargetListsMover;
        myCustomForBinaries = customForBinaries;
        myCommitContext = commitContext;
        myReverseConflict = reverseConflict;
        myLeftConflictPanelTitle = leftConflictPanelTitle;
        myRightConflictPanelTitle = rightConflictPanelTitle;
        myRemainingPatches = new ArrayList<>();
        myFailedPatches = new ArrayList<>();
        myVerifier = new PathsVerifier<>(myProject, myBaseDirectory, myPatches, new PathsVerifier.BaseMapper() {
            @Override
            @Nullable
            public VirtualFile getFile(FilePatch patch, String path) {
                return PathMerger.getFile(myBaseDirectory, path);
            }

            @Override
            public FilePath getPath(FilePatch patch, String path) {
                return PathMerger.getFile(VcsUtil.getFilePath(myBaseDirectory), path);
            }
        });
    }

    public PatchApplier(
        final Project project,
        final VirtualFile baseDirectory,
        @Nonnull final List<FilePatch> patches,
        final LocalChangeList targetChangeList,
        final CustomBinaryPatchApplier<BinaryType> customForBinaries,
        final CommitContext commitContext,
        boolean reverseConflict,
        @Nullable String leftConflictPanelTitle,
        @Nullable String rightConflictPanelTitle
    ) {
        this(
            project,
            baseDirectory,
            patches,
            createMover(project, targetChangeList),
            customForBinaries,
            commitContext,
            reverseConflict,
            leftConflictPanelTitle,
            rightConflictPanelTitle
        );
    }

    public void setIgnoreContentRootsCheck() {
        myVerifier.setIgnoreContentRootsCheck(true);
    }

    public PatchApplier(
        final Project project,
        final VirtualFile baseDirectory,
        @Nonnull final List<FilePatch> patches,
        final LocalChangeList targetChangeList,
        final CustomBinaryPatchApplier<BinaryType> customForBinaries,
        final CommitContext commitContext
    ) {
        this(project, baseDirectory, patches, targetChangeList, customForBinaries, commitContext, false, null, null);
    }

    public void setIsSystemOperation(boolean systemOperation) {
        mySystemOperation = systemOperation;
    }

    @Nullable
    private static Consumer<Collection<FilePath>> createMover(final Project project, final LocalChangeList targetChangeList) {
        final ChangeListManager clm = ChangeListManager.getInstance(project);
        if (targetChangeList == null || clm.getDefaultListName().equals(targetChangeList.getName())) {
            return null;
        }
        return new FilesMover(clm, targetChangeList);
    }

    @Nonnull
    public List<FilePatch> getPatches() {
        return myPatches;
    }

    @Nonnull
    private Collection<FilePatch> getFailedPatches() {
        return myFailedPatches;
    }

    @Nonnull
    public List<BinaryType> getBinaryPatches() {
        return ContainerUtil.mapNotNull(myVerifier.getBinaryPatches(), patchInfo -> patchInfo.getSecond().getPatch());
    }

    @RequiredUIAccess
    public void execute() {
        execute(true, false);
    }

    public class ApplyPatchTask {
        private ApplyPatchStatus myStatus;
        private final boolean myShowNotification;
        private final boolean mySystemOperation;
        private VcsShowConfirmationOption.Value myAddconfirmationvalue;
        private VcsShowConfirmationOption.Value myDeleteconfirmationvalue;

        public ApplyPatchTask(final boolean showNotification, boolean systemOperation) {
            myShowNotification = showNotification;
            mySystemOperation = systemOperation;
        }

        @RequiredUIAccess
        public void run() {
            myRemainingPatches.addAll(myPatches);

            final ApplyPatchStatus patchStatus = nonWriteActionPreCheck();
            final Label beforeLabel = LocalHistory.getInstance().putSystemLabel(myProject, "Before patch");
            final TriggerAdditionOrDeletion trigger = new TriggerAdditionOrDeletion(myProject);
            final ApplyPatchStatus applyStatus = getApplyPatchStatus(trigger);
            myStatus = ApplyPatchStatus.SUCCESS.equals(patchStatus) ? applyStatus : ApplyPatchStatus.and(patchStatus, applyStatus);
            // listeners finished, all 'legal' file additions/deletions with VCS are done
            trigger.processIt();
            LocalHistory.getInstance().putSystemLabel(myProject, "After patch"); // insert a label to be visible in local history dialog
            if (myStatus == ApplyPatchStatus.FAILURE) {
                suggestRollback(myProject, Collections.singletonList(PatchApplier.this), beforeLabel);
            }
            else if (myStatus == ApplyPatchStatus.ABORT) {
                rollbackUnderProgress(myProject, myProject.getBaseDir(), beforeLabel);
            }
            if (myShowNotification || !ApplyPatchStatus.SUCCESS.equals(myStatus)) {
                showApplyStatus(myProject, myStatus);
            }
            refreshFiles(trigger.getAffected());
        }

        @Nonnull
        @RequiredUIAccess
        private ApplyPatchStatus getApplyPatchStatus(@Nonnull final TriggerAdditionOrDeletion trigger) {
            final SimpleReference<ApplyPatchStatus> refStatus = SimpleReference.create(null);
            try {
                setConfirmationToDefault();
                CommandProcessor.getInstance().newCommand(() -> {
                        //consider pre-check status only if not successful, otherwise we could not detect already applied status
                        if (createFiles() != ApplyPatchStatus.SUCCESS) {
                            refStatus.set(createFiles());
                        }
                        addSkippedItems(trigger);
                        trigger.prepare();
                        refStatus.set(ApplyPatchStatus.and(refStatus.get(), executeWritable()));
                    })
                    .withProject(myProject)
                    .withName(VcsLocalize.patchApplyCommand())
                    .execute();
            }
            finally {
                returnConfirmationBack();
                VcsFileListenerContextHelper.getInstance(myProject).clearContext();
            }
            final ApplyPatchStatus status = refStatus.get();
            return status == null ? ApplyPatchStatus.ALREADY_APPLIED : status;
        }

        private void returnConfirmationBack() {
            if (mySystemOperation) {
                final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
                final VcsShowConfirmationOption addConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, null);
                addConfirmation.setValue(myAddconfirmationvalue);
                final VcsShowConfirmationOption deleteConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, null);
                deleteConfirmation.setValue(myDeleteconfirmationvalue);
            }
        }

        private void setConfirmationToDefault() {
            if (mySystemOperation) {
                final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
                final VcsShowConfirmationOption addConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, null);
                myAddconfirmationvalue = addConfirmation.getValue();
                addConfirmation.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);

                final VcsShowConfirmationOption deleteConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, null);
                myDeleteconfirmationvalue = deleteConfirmation.getValue();
                deleteConfirmation.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
            }
        }

        public ApplyPatchStatus getStatus() {
            return myStatus;
        }
    }

    public ApplyPatchTask createApplyPart(final boolean showSuccessNotification, boolean silentAddDelete) {
        return new ApplyPatchTask(showSuccessNotification, silentAddDelete);
    }

    @RequiredUIAccess
    public void execute(boolean showSuccessNotification, boolean silentAddDelete) {
        createApplyPart(showSuccessNotification, silentAddDelete).run();
    }

    @RequiredUIAccess
    public static ApplyPatchStatus executePatchGroup(final Collection<PatchApplier> group, final LocalChangeList localChangeList) {
        if (group.isEmpty()) {
            return ApplyPatchStatus.SUCCESS; //?
        }
        final Project project = group.iterator().next().myProject;

        ApplyPatchStatus result = ApplyPatchStatus.SUCCESS;
        for (PatchApplier patchApplier : group) {
            result = ApplyPatchStatus.and(result, patchApplier.nonWriteActionPreCheck());
        }
        final Label beforeLabel = LocalHistory.getInstance().putSystemLabel(project, "Before patch");
        final TriggerAdditionOrDeletion trigger = new TriggerAdditionOrDeletion(project);
        final SimpleReference<ApplyPatchStatus> refStatus = new SimpleReference<>(result);
        try {
            CommandProcessor.getInstance().newCommand(() -> {
                    for (PatchApplier applier : group) {
                        refStatus.set(ApplyPatchStatus.and(refStatus.get(), applier.createFiles()));
                        applier.addSkippedItems(trigger);
                    }
                    trigger.prepare();
                    if (refStatus.get() == ApplyPatchStatus.SUCCESS) {
                        // all pre-check results are valuable only if not successful; actual status we can receive after executeWritable
                        refStatus.set(null);
                    }
                    for (PatchApplier applier : group) {
                        refStatus.set(ApplyPatchStatus.and(refStatus.get(), applier.executeWritable()));
                        if (refStatus.get() == ApplyPatchStatus.ABORT) {
                            break;
                        }
                    }
                })
                .withProject(project)
                .withName(VcsLocalize.patchApplyCommand())
                .execute();
        }
        finally {
            VcsFileListenerContextHelper.getInstance(project).clearContext();
            LocalHistory.getInstance().putSystemLabel(project, "After patch");
        }
        result = refStatus.get();
        result = result == null ? ApplyPatchStatus.FAILURE : result;

        trigger.processIt();
        final Set<FilePath> directlyAffected = new HashSet<>();
        final Set<VirtualFile> indirectlyAffected = new HashSet<>();
        for (PatchApplier applier : group) {
            directlyAffected.addAll(applier.getDirectlyAffected());
            indirectlyAffected.addAll(applier.getIndirectlyAffected());
        }
        directlyAffected.addAll(trigger.getAffected());
        final Consumer<Collection<FilePath>> mover = localChangeList == null ? null : createMover(project, localChangeList);
        refreshPassedFilesAndMoveToChangelist(project, directlyAffected, indirectlyAffected, mover);
        if (result == ApplyPatchStatus.FAILURE) {
            suggestRollback(project, group, beforeLabel);
        }
        else if (result == ApplyPatchStatus.ABORT) {
            rollbackUnderProgress(project, project.getBaseDir(), beforeLabel);
        }
        showApplyStatus(project, result);
        return result;
    }

    @RequiredUIAccess
    private static void suggestRollback(@Nonnull Project project, @Nonnull Collection<PatchApplier> group, @Nonnull Label beforeLabel) {
        Collection<FilePatch> allFailed = ContainerUtil.concat(group, PatchApplier::getFailedPatches);
        boolean shouldInformAboutBinaries = ContainerUtil.exists(group, applier -> !applier.getBinaryPatches().isEmpty());
        final UndoApplyPatchDialog undoApplyPatchDialog = new UndoApplyPatchDialog(
            project,
            ContainerUtil.map(
                allFailed,
                filePatch -> {
                    String path = filePatch.getAfterName() == null ? filePatch.getBeforeName() : filePatch.getAfterName();
                    return VcsUtil.getFilePath(path);
                }
            ),
            shouldInformAboutBinaries
        );
        undoApplyPatchDialog.show();
        if (undoApplyPatchDialog.isOK()) {
            rollbackUnderProgress(project, project.getBaseDir(), beforeLabel);
        }
    }

    private static void rollbackUnderProgress(
        @Nonnull final Project project,
        @Nonnull final VirtualFile virtualFile,
        @Nonnull final Label labelToRevert
    ) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                labelToRevert.revert(project, virtualFile);
                VcsNotifier.getInstance(project).notifyImportantWarning(
                    "Apply Patch Aborted",
                    "All files changed during apply patch action were rolled back"
                );
            }
            catch (LocalHistoryException e) {
                VcsNotifier.getInstance(project).notifyImportantWarning(
                    "Rollback Failed",
                    String.format("Try using local history dialog for %s and perform revert manually.", virtualFile.getName())
                );
            }
        }, "Rollback Applied Changes...", true, project);
    }


    protected void addSkippedItems(final TriggerAdditionOrDeletion trigger) {
        trigger.addExisting(myVerifier.getToBeAdded());
        trigger.addDeleted(myVerifier.getToBeDeleted());
    }

    @Nonnull
    public ApplyPatchStatus nonWriteActionPreCheck() {
        final List<FilePatch> failedPreCheck = myVerifier.nonWriteActionPreCheck();
        myFailedPatches.addAll(failedPreCheck);
        myPatches.removeAll(failedPreCheck);
        final List<FilePatch> skipped = myVerifier.getSkipped();
        final boolean applyAll = skipped.isEmpty();
        myPatches.removeAll(skipped);
        if (!failedPreCheck.isEmpty()) {
            return ApplyPatchStatus.FAILURE;
        }
        return applyAll
            ? ApplyPatchStatus.SUCCESS
            : skipped.size() == myPatches.size()
            ? ApplyPatchStatus.ALREADY_APPLIED
            : ApplyPatchStatus.PARTIAL;
    }

    @Nullable
    @RequiredUIAccess
    protected ApplyPatchStatus executeWritable() {
        final ReadonlyStatusHandler.OperationStatus readOnlyFilesStatus = getReadOnlyFilesStatus(myVerifier.getWritableFiles());
        if (readOnlyFilesStatus.hasReadonlyFiles()) {
            showError(myProject, readOnlyFilesStatus.getReadonlyFilesMessage(), true);
            return ApplyPatchStatus.ABORT;
        }
        myFailedPatches.addAll(myVerifier.filterBadFileTypePatches());
        ApplyPatchStatus result = myFailedPatches.isEmpty() ? null : ApplyPatchStatus.FAILURE;
        final List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches = myVerifier.getTextPatches();
        try {
            markInternalOperation(textPatches, true);
            return ApplyPatchStatus.and(result, actualApply(textPatches, myVerifier.getBinaryPatches(), myCommitContext));
        }
        finally {
            markInternalOperation(textPatches, false);
        }
    }

    @Nonnull
    @RequiredUIAccess
    private ApplyPatchStatus createFiles() {
        Boolean isSuccess = Application.get().runWriteAction((Computable<Boolean>)() -> {
            final List<FilePatch> filePatches = myVerifier.execute();
            myFailedPatches.addAll(filePatches);
            myPatches.removeAll(filePatches);
            return myFailedPatches.isEmpty();
        });
        return isSuccess ? ApplyPatchStatus.SUCCESS : ApplyPatchStatus.FAILURE;
    }

    private static void markInternalOperation(List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches, boolean set) {
        for (Pair<VirtualFile, ApplyTextFilePatch> patch : textPatches) {
            ChangesUtil.markInternalOperation(patch.getFirst(), set);
        }
    }

    protected void refreshFiles(final Collection<FilePath> additionalDirectly) {
        final List<FilePath> directlyAffected = myVerifier.getDirectlyAffected();
        final List<VirtualFile> indirectlyAffected = myVerifier.getAllAffected();
        directlyAffected.addAll(additionalDirectly);

        refreshPassedFilesAndMoveToChangelist(myProject, directlyAffected, indirectlyAffected, myToTargetListsMover);
    }

    public List<FilePath> getDirectlyAffected() {
        return myVerifier.getDirectlyAffected();
    }

    public List<VirtualFile> getIndirectlyAffected() {
        return myVerifier.getAllAffected();
    }

    public static void refreshPassedFilesAndMoveToChangelist(
        @Nonnull final Project project,
        final Collection<FilePath> directlyAffected,
        final Collection<VirtualFile> indirectlyAffected,
        final Consumer<Collection<FilePath>> targetChangelistMover
    ) {
        final LocalFileSystem lfs = LocalFileSystem.getInstance();
        for (FilePath filePath : directlyAffected) {
            lfs.refreshAndFindFileByIoFile(filePath.getIOFile());
        }
        if (project.isDisposed()) {
            return;
        }

        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        if (!directlyAffected.isEmpty() && targetChangelistMover != null) {
            changeListManager.invokeAfterUpdate(
                () -> targetChangelistMover.accept(directlyAffected),
                InvokeAfterUpdateMode.SYNCHRONOUS_CANCELLABLE,
                VcsLocalize.changeListsManagerMoveChangesToList().get(),
                vcsDirtyScopeManager -> markDirty(vcsDirtyScopeManager, directlyAffected, indirectlyAffected),
                null
            );
        }
        else {
            markDirty(VcsDirtyScopeManager.getInstance(project), directlyAffected, indirectlyAffected);
        }
    }

    private static void markDirty(
        @Nonnull VcsDirtyScopeManager vcsDirtyScopeManager,
        @Nonnull Collection<FilePath> directlyAffected,
        @Nonnull Collection<VirtualFile> indirectlyAffected
    ) {
        vcsDirtyScopeManager.filePathsDirty(directlyAffected, null);
        vcsDirtyScopeManager.filesDirty(indirectlyAffected, null);
    }

    @Nullable
    @RequiredUIAccess
    private ApplyPatchStatus actualApply(
        final List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches,
        final List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> binaryPatches,
        final CommitContext commitContext
    ) {
        final ApplyPatchContext context = new ApplyPatchContext(myBaseDirectory, 0, true, true);
        ApplyPatchStatus status;

        try {
            status = applyList(textPatches, context, null, commitContext);

            if (status == ApplyPatchStatus.ABORT) {
                return status;
            }

            if (myCustomForBinaries == null) {
                status = applyList(binaryPatches, context, status, commitContext);
            }
            else {
                ApplyPatchStatus patchStatus = myCustomForBinaries.apply(binaryPatches);
                final List<FilePatch> appliedPatches = myCustomForBinaries.getAppliedPatches();
                moveForCustomBinaries(binaryPatches, appliedPatches);

                status = ApplyPatchStatus.and(status, patchStatus);
                myRemainingPatches.removeAll(appliedPatches);
            }
        }
        catch (IOException e) {
            showError(myProject, e.getMessage(), true);
            return ApplyPatchStatus.ABORT;
        }
        return status;
    }

    @RequiredUIAccess
    private void moveForCustomBinaries(
        final List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> patches,
        final List<FilePatch> appliedPatches
    ) throws IOException {
        for (Pair<VirtualFile, ApplyFilePatchBase<BinaryType>> patch : patches) {
            if (appliedPatches.contains(patch.getSecond().getPatch())) {
                myVerifier.doMoveIfNeeded(patch.getFirst());
            }
        }
    }

    @RequiredUIAccess
    private <V extends FilePatch, T extends ApplyFilePatchBase<V>> ApplyPatchStatus applyList(
        final List<Pair<VirtualFile, T>> patches,
        final ApplyPatchContext context,
        ApplyPatchStatus status,
        CommitContext commiContext
    ) throws IOException {
        for (Pair<VirtualFile, T> patch : patches) {
            ApplyPatchStatus patchStatus = ApplyPatchAction.applyOnly(
                myProject,
                patch.getSecond(),
                context,
                patch.getFirst(),
                commiContext,
                myReverseConflict,
                myLeftConflictPanelTitle,
                myRightConflictPanelTitle
            );

            if (patchStatus == ApplyPatchStatus.ABORT) {
                return patchStatus;
            }
            status = ApplyPatchStatus.and(status, patchStatus);
            if (patchStatus == ApplyPatchStatus.FAILURE) {
                myFailedPatches.add(patch.getSecond().getPatch());
                continue;
            }
            if (patchStatus != ApplyPatchStatus.SKIP) {
                myVerifier.doMoveIfNeeded(patch.getFirst());
                myRemainingPatches.remove(patch.getSecond().getPatch());
            }
        }
        return status;
    }

    @RequiredUIAccess
    protected static void showApplyStatus(@Nonnull Project project, final ApplyPatchStatus status) {
        if (status == ApplyPatchStatus.ALREADY_APPLIED) {
            showError(project, VcsLocalize.patchApplyAlreadyApplied().get(), false);
        }
        else if (status == ApplyPatchStatus.PARTIAL) {
            showError(project, VcsLocalize.patchApplyPartiallyApplied().get(), false);
        }
        else if (ApplyPatchStatus.SUCCESS.equals(status)) {
            final LocalizeValue message = VcsLocalize.patchApplySuccessAppliedText();
            VcsBalloonProblemNotifier.NOTIFICATION_GROUP.createNotification(message.get(), NotificationType.INFORMATION).notify(project);
        }
    }

    @Nonnull
    public List<FilePatch> getRemainingPatches() {
        return myRemainingPatches;
    }

    private ReadonlyStatusHandler.OperationStatus getReadOnlyFilesStatus(@Nonnull final List<VirtualFile> filesToMakeWritable) {
        final VirtualFile[] fileArray = VfsUtilCore.toVirtualFileArray(filesToMakeWritable);
        return ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(fileArray);
    }

    @RequiredUIAccess
    public static void showError(final Project project, final String message, final boolean error) {
        final Application application = Application.get();
        if (application.isUnitTestMode()) {
            return;
        }
        LocalizeValue title = VcsLocalize.patchApplyDialogTitle();
        final Runnable messageShower = () -> {
            if (error) {
                Messages.showErrorDialog(project, message, title.get());
            }
            else {
                Messages.showInfoMessage(project, message, title.get());
            }
        };
        WaitForProgressToShow.runOrInvokeLaterAboveProgress(messageShower::run, null, project);
    }

    private static class FilesMover implements Consumer<Collection<FilePath>> {
        private final ChangeListManager myChangeListManager;
        private final LocalChangeList myTargetChangeList;

        public FilesMover(final ChangeListManager changeListManager, final LocalChangeList targetChangeList) {
            myChangeListManager = changeListManager;
            myTargetChangeList = targetChangeList;
        }

        @Override
        public void accept(Collection<FilePath> directlyAffected) {
            List<Change> changes = new ArrayList<>();
            for (FilePath file : directlyAffected) {
                final Change change = myChangeListManager.getChange(file);
                if (change != null) {
                    changes.add(change);
                }
            }

            myChangeListManager.moveChangesTo(myTargetChangeList, changes.toArray(new Change[changes.size()]));
        }
    }
}
