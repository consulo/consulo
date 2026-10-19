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
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.application.Application;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.localHistory.Label;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchAction;
import consulo.versionControlSystem.impl.internal.patch.PathMerger;
import consulo.versionControlSystem.internal.VcsFileListenerContextHelper;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * for patches. for shelve.
 */
public class PatchApplier<BinaryType extends FilePatch> {
    
    private final Project myProject;
    private final VirtualFile myBaseDirectory;
    
    private final List<FilePatch> myPatches;
    private final CustomBinaryPatchApplier<BinaryType> myCustomForBinaries;
    private final CommitContext myCommitContext;
    private final Consumer<Collection<FilePath>> myToTargetListsMover;
    
    private final List<FilePatch> myRemainingPatches;
    
    private final List<FilePatch> myFailedPatches;
    private final PathsVerifier<BinaryType> myVerifier;
    private boolean mySystemOperation;

    private final boolean myReverseConflict;
    private final @Nullable String myLeftConflictPanelTitle;
    private final @Nullable String myRightConflictPanelTitle;

    private final List<ApplyPatchAttempt> myPendingMerges = new ArrayList<>();
    private List<Pair<VirtualFile, ApplyTextFilePatch>> myInternalTextPatches = List.of();

    public PatchApplier(
        Project project,
        VirtualFile baseDirectory,
        List<FilePatch> patches,
        @Nullable Consumer<Collection<FilePath>> toTargetListsMover,
        CustomBinaryPatchApplier<BinaryType> customForBinaries,
        CommitContext commitContext,
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
            public @Nullable VirtualFile getFile(FilePatch patch, String path) {
                return PathMerger.getFile(myBaseDirectory, path);
            }

            @Override
            public FilePath getPath(FilePatch patch, String path) {
                return PathMerger.getFile(VcsUtil.getFilePath(myBaseDirectory), path);
            }
        });
    }

    public PatchApplier(
        Project project,
        VirtualFile baseDirectory,
        List<FilePatch> patches,
        LocalChangeList targetChangeList,
        CustomBinaryPatchApplier<BinaryType> customForBinaries,
        CommitContext commitContext,
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
        Project project,
        VirtualFile baseDirectory,
        List<FilePatch> patches,
        LocalChangeList targetChangeList,
        CustomBinaryPatchApplier<BinaryType> customForBinaries,
        CommitContext commitContext
    ) {
        this(project, baseDirectory, patches, targetChangeList, customForBinaries, commitContext, false, null, null);
    }

    public void setIsSystemOperation(boolean systemOperation) {
        mySystemOperation = systemOperation;
    }

    private static @Nullable Consumer<Collection<FilePath>> createMover(Project project, LocalChangeList targetChangeList) {
        ChangeListManager clm = ChangeListManager.getInstance(project);
        if (targetChangeList == null || clm.getDefaultListName().equals(targetChangeList.getName())) {
            return null;
        }
        return new FilesMover(clm, targetChangeList);
    }

    
    public List<FilePatch> getPatches() {
        return myPatches;
    }

    
    private Collection<FilePatch> getFailedPatches() {
        return myFailedPatches;
    }

    
    public List<BinaryType> getBinaryPatches() {
        return ContainerUtil.mapNotNull(myVerifier.getBinaryPatches(), patchInfo -> patchInfo.getSecond().getPatch());
    }

    @RequiredUIAccess
    public CompletableFuture<ApplyPatchStatus> execute() {
        return execute(true, false);
    }

    public class ApplyPatchTask {
        private ApplyPatchStatus myStatus;
        private final boolean myShowNotification;
        private final boolean mySystemOperation;
        private VcsShowConfirmationOption.Value myAddconfirmationvalue;
        private VcsShowConfirmationOption.Value myDeleteconfirmationvalue;

        public ApplyPatchTask(boolean showNotification, boolean systemOperation) {
            myShowNotification = showNotification;
            mySystemOperation = systemOperation;
        }

        @RequiredUIAccess
        public CompletableFuture<ApplyPatchStatus> run() {
            UIAccess uiAccess = UIAccess.current();

            myRemainingPatches.addAll(myPatches);

            ApplyPatchStatus patchStatus = nonWriteActionPreCheck();
            Label beforeLabel = LocalHistory.getInstance().putSystemLabel(myProject, LocalizeValue.localizeTODO("Before patch"));
            TriggerAdditionOrDeletion trigger = new TriggerAdditionOrDeletion(myProject);
            return getApplyPatchStatus(trigger, uiAccess).thenApplyAsync(applyStatus -> {
                myStatus = ApplyPatchStatus.SUCCESS.equals(patchStatus) ? applyStatus : ApplyPatchStatus.and(patchStatus, applyStatus);
                // listeners finished, all 'legal' file additions/deletions with VCS are done
                trigger.processIt();
                // insert a label to be visible in local history dialog
                LocalHistory.getInstance().putSystemLabel(myProject, LocalizeValue.localizeTODO("After patch"));
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
                return myStatus;
            }, uiAccess);
        }

        @RequiredUIAccess
        private CompletableFuture<ApplyPatchStatus> getApplyPatchStatus(TriggerAdditionOrDeletion trigger, UIAccess uiAccess) {
            SimpleReference<ApplyPatchStatus> refStatus = SimpleReference.create(null);
            SimpleReference<ApplyPatchStatus> preparedStatus = SimpleReference.create(null);
            setConfirmationToDefault();
            try {
                CommandProcessor.getInstance().newCommand()
                    .project(myProject)
                    .name(VcsLocalize.patchApplyCommand())
                    .run(() -> {
                        //consider pre-check status only if not successful, otherwise we could not detect already applied status
                        if (createFiles() != ApplyPatchStatus.SUCCESS) {
                            refStatus.set(createFiles());
                        }
                        addSkippedItems(trigger);
                        trigger.prepare();
                        preparedStatus.set(prepareWritable());
                    });
            }
            catch (RuntimeException e) {
                returnConfirmationBack();
                VcsFileListenerContextHelper.getInstance(myProject).clearContext();
                throw e;
            }

            return finishWritable(preparedStatus.get(), uiAccess)
                .whenCompleteAsync((status, error) -> {
                    returnConfirmationBack();
                    VcsFileListenerContextHelper.getInstance(myProject).clearContext();
                }, uiAccess)
                .thenApply(status -> {
                    ApplyPatchStatus result = ApplyPatchStatus.and(refStatus.get(), status);
                    return result == null ? ApplyPatchStatus.ALREADY_APPLIED : result;
                });
        }

        private void returnConfirmationBack() {
            if (mySystemOperation) {
                ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
                VcsShowConfirmationOption addConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, null);
                addConfirmation.setValue(myAddconfirmationvalue);
                VcsShowConfirmationOption deleteConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, null);
                deleteConfirmation.setValue(myDeleteconfirmationvalue);
            }
        }

        private void setConfirmationToDefault() {
            if (mySystemOperation) {
                ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
                VcsShowConfirmationOption addConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, null);
                myAddconfirmationvalue = addConfirmation.getValue();
                addConfirmation.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);

                VcsShowConfirmationOption deleteConfirmation =
                    vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, null);
                myDeleteconfirmationvalue = deleteConfirmation.getValue();
                deleteConfirmation.setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
            }
        }

        public ApplyPatchStatus getStatus() {
            return myStatus;
        }
    }

    public ApplyPatchTask createApplyPart(boolean showSuccessNotification, boolean silentAddDelete) {
        return new ApplyPatchTask(showSuccessNotification, silentAddDelete);
    }

    @RequiredUIAccess
    public CompletableFuture<ApplyPatchStatus> execute(boolean showSuccessNotification, boolean silentAddDelete) {
        return createApplyPart(showSuccessNotification, silentAddDelete).run();
    }

    @RequiredUIAccess
    public static CompletableFuture<ApplyPatchStatus> executePatchGroup(Collection<PatchApplier> group, LocalChangeList localChangeList) {
        if (group.isEmpty()) {
            return CompletableFuture.completedFuture(ApplyPatchStatus.SUCCESS); //?
        }
        UIAccess uiAccess = UIAccess.current();
        Project project = group.iterator().next().myProject;

        ApplyPatchStatus result = ApplyPatchStatus.SUCCESS;
        for (PatchApplier patchApplier : group) {
            result = ApplyPatchStatus.and(result, patchApplier.nonWriteActionPreCheck());
        }
        Label beforeLabel = LocalHistory.getInstance().putSystemLabel(project, LocalizeValue.localizeTODO("Before patch"));
        TriggerAdditionOrDeletion trigger = new TriggerAdditionOrDeletion(project);
        SimpleReference<ApplyPatchStatus> refStatus = new SimpleReference<>(result);
        List<PatchApplier> prepared = new ArrayList<>();
        try {
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .name(VcsLocalize.patchApplyCommand())
                .run(() -> {
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
                        refStatus.set(ApplyPatchStatus.and(refStatus.get(), applier.prepareWritable()));
                        prepared.add(applier);
                        if (refStatus.get() == ApplyPatchStatus.ABORT) {
                            break;
                        }
                    }
                });
        }
        catch (RuntimeException e) {
            VcsFileListenerContextHelper.getInstance(project).clearContext();
            LocalHistory.getInstance().putSystemLabel(project, LocalizeValue.localizeTODO("After patch"));
            throw e;
        }

        CompletableFuture<@Nullable ApplyPatchStatus> chain = CompletableFuture.completedFuture(refStatus.get());
        for (PatchApplier applier : prepared) {
            chain = chain.thenComposeAsync(status -> applier.finishWritable(status, uiAccess), uiAccess);
        }

        return chain
            .whenCompleteAsync((status, error) -> {
                VcsFileListenerContextHelper.getInstance(project).clearContext();
                LocalHistory.getInstance().putSystemLabel(project, LocalizeValue.localizeTODO("After patch"));
            }, uiAccess)
            .thenApplyAsync(status -> {
                ApplyPatchStatus groupResult = status == null ? ApplyPatchStatus.FAILURE : status;

                trigger.processIt();
                Set<FilePath> directlyAffected = new HashSet<>();
                Set<VirtualFile> indirectlyAffected = new HashSet<>();
                for (PatchApplier applier : group) {
                    directlyAffected.addAll(applier.getDirectlyAffected());
                    indirectlyAffected.addAll(applier.getIndirectlyAffected());
                }
                directlyAffected.addAll(trigger.getAffected());
                Consumer<Collection<FilePath>> mover = localChangeList == null ? null : createMover(project, localChangeList);
                refreshPassedFilesAndMoveToChangelist(project, directlyAffected, indirectlyAffected, mover);
                if (groupResult == ApplyPatchStatus.FAILURE) {
                    suggestRollback(project, group, beforeLabel);
                }
                else if (groupResult == ApplyPatchStatus.ABORT) {
                    rollbackUnderProgress(project, project.getBaseDir(), beforeLabel);
                }
                showApplyStatus(project, groupResult);
                return groupResult;
            }, uiAccess);
    }

    @RequiredUIAccess
    private static void suggestRollback(Project project, Collection<PatchApplier> group, Label beforeLabel) {
        Collection<FilePatch> allFailed = ContainerUtil.concat(group, PatchApplier::getFailedPatches);
        boolean shouldInformAboutBinaries = ContainerUtil.exists(group, applier -> !applier.getBinaryPatches().isEmpty());

        StringBuilder text = new StringBuilder();
        if (!allFailed.isEmpty()) {
            text.append(String.format("Failed to apply %s below.<br>", StringUtil.pluralize("file", allFailed.size())));
            for (FilePatch filePatch : allFailed) {
                String path = filePatch.getAfterName() == null ? filePatch.getBeforeName() : filePatch.getAfterName();
                text.append(XmlStringUtil.escapeString(path)).append("<br>");
            }
        }
        text.append("Would you like to rollback all applied?");
        if (shouldInformAboutBinaries) {
            text.append("<br>Rollback will not affect binaries");
        }

        UIAccess uiAccess = UIAccess.current();
        MessageBoxes.yesNo()
            .asWarning()
            .title(LocalizeValue.localizeTODO("Patch Applying Partly Failed"))
            .text(LocalizeValue.localizeTODO(text.toString()))
            .showAsync(project)
            .whenCompleteAsync((rollback, error) -> {
                if (Boolean.TRUE.equals(rollback)) {
                    rollbackUnderProgress(project, project.getBaseDir(), beforeLabel);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private static void rollbackUnderProgress(
        Project project,
        VirtualFile virtualFile,
        Label labelToRevert
    ) {
        UIAccess uiAccess = UIAccess.current();
        Application.get().getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(project, LocalizeValue.localizeTODO("Rollback Applied Changes..."))
            .cancelable()
            .modal()
            .execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, Object>apply((input, continuation) -> {
                NotificationService notificationService = NotificationService.getInstance();
                try {
                    labelToRevert.revert(project, virtualFile);
                    notificationService.newWarn(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION)
                        .title(LocalizeValue.localizeTODO("Apply Patch Aborted"))
                        .content(LocalizeValue.localizeTODO("All files changed during apply patch action were rolled back"))
                        .notify(project);
                }
                catch (LocalHistoryException e) {
                    notificationService.newWarn(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION)
                        .title(LocalizeValue.localizeTODO("Rollback Failed"))
                        .content(LocalizeValue.localizeTODO(String.format(
                            "Try using local history dialog for %s and perform revert manually.",
                            virtualFile.getName()
                        )))
                        .notify(project);
                }
                return input;
            })));
    }

    protected void addSkippedItems(TriggerAdditionOrDeletion trigger) {
        trigger.addExisting(myVerifier.getToBeAdded());
        trigger.addDeleted(myVerifier.getToBeDeleted());
    }

    
    public ApplyPatchStatus nonWriteActionPreCheck() {
        List<FilePatch> failedPreCheck = myVerifier.nonWriteActionPreCheck();
        myFailedPatches.addAll(failedPreCheck);
        myPatches.removeAll(failedPreCheck);
        List<FilePatch> skipped = myVerifier.getSkipped();
        boolean applyAll = skipped.isEmpty();
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

    @RequiredUIAccess
    protected @Nullable ApplyPatchStatus prepareWritable() {
        myPendingMerges.clear();
        ReadonlyStatusHandler.OperationStatus readOnlyFilesStatus = getReadOnlyFilesStatus(myVerifier.getWritableFiles());
        if (readOnlyFilesStatus.hasReadonlyFiles()) {
            showError(myProject, readOnlyFilesStatus.getReadonlyFilesMessage(), true);
            return ApplyPatchStatus.ABORT;
        }
        myFailedPatches.addAll(myVerifier.filterBadFileTypePatches());
        ApplyPatchStatus result = myFailedPatches.isEmpty() ? null : ApplyPatchStatus.FAILURE;
        myInternalTextPatches = myVerifier.getTextPatches();
        markInternalOperation(myInternalTextPatches, true);
        return ApplyPatchStatus.and(result, actualApply(myInternalTextPatches, myVerifier.getBinaryPatches(), myCommitContext));
    }

    @RequiredUIAccess
    protected CompletableFuture<@Nullable ApplyPatchStatus> finishWritable(@Nullable ApplyPatchStatus status, UIAccess uiAccess) {
        List<ApplyPatchAttempt> merges = new ArrayList<>(myPendingMerges);
        myPendingMerges.clear();

        CompletableFuture<@Nullable ApplyPatchStatus> chain = CompletableFuture.completedFuture(status);
        if (status != ApplyPatchStatus.ABORT) {
            for (ApplyPatchAttempt merge : merges) {
                chain = chain.thenComposeAsync(current -> {
                    if (current == ApplyPatchStatus.ABORT) {
                        return CompletableFuture.completedFuture(current);
                    }
                    return merge.runMerge().thenApplyAsync(mergeStatus -> onMerged(merge, current, mergeStatus), uiAccess);
                }, uiAccess);
            }
        }

        List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches = myInternalTextPatches;
        return chain.whenCompleteAsync((result, error) -> markInternalOperation(textPatches, false), uiAccess);
    }

    @RequiredUIAccess
    private @Nullable ApplyPatchStatus onMerged(ApplyPatchAttempt merge, @Nullable ApplyPatchStatus status, @Nullable ApplyPatchStatus mergeStatus) {
        if (mergeStatus == ApplyPatchStatus.ABORT) {
            return mergeStatus;
        }
        ApplyPatchStatus result = ApplyPatchStatus.and(status, mergeStatus);
        if (mergeStatus == ApplyPatchStatus.FAILURE) {
            myFailedPatches.add(merge.getPatch().getPatch());
            return result;
        }
        if (mergeStatus != ApplyPatchStatus.SKIP) {
            try {
                myVerifier.doMoveIfNeeded(merge.getFile());
            }
            catch (IOException e) {
                showError(myProject, e.getMessage(), true);
                return ApplyPatchStatus.ABORT;
            }
            myRemainingPatches.remove(merge.getPatch().getPatch());
        }
        return result;
    }

    
    @RequiredUIAccess
    private ApplyPatchStatus createFiles() {
        Boolean isSuccess = myProject.getApplication().runWriteAction((Supplier<Boolean>)() -> {
            List<FilePatch> filePatches = myVerifier.execute();
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

    protected void refreshFiles(Collection<FilePath> additionalDirectly) {
        List<FilePath> directlyAffected = myVerifier.getDirectlyAffected();
        List<VirtualFile> indirectlyAffected = myVerifier.getAllAffected();
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
        Project project,
        Collection<FilePath> directlyAffected,
        Collection<VirtualFile> indirectlyAffected,
        Consumer<Collection<FilePath>> targetChangelistMover
    ) {
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        for (FilePath filePath : directlyAffected) {
            lfs.refreshAndFindFileByIoFile(filePath.getIOFile());
        }
        if (project.isDisposed()) {
            return;
        }

        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
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
        VcsDirtyScopeManager vcsDirtyScopeManager,
        Collection<FilePath> directlyAffected,
        Collection<VirtualFile> indirectlyAffected
    ) {
        vcsDirtyScopeManager.filePathsDirty(directlyAffected, null);
        vcsDirtyScopeManager.filesDirty(indirectlyAffected, null);
    }

    @RequiredUIAccess
    private @Nullable ApplyPatchStatus actualApply(
        List<Pair<VirtualFile, ApplyTextFilePatch>> textPatches,
        List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> binaryPatches,
        CommitContext commitContext
    ) {
        ApplyPatchContext context = new ApplyPatchContext(myBaseDirectory, 0, true, true);
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
                List<FilePatch> appliedPatches = myCustomForBinaries.getAppliedPatches();
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
        List<Pair<VirtualFile, ApplyFilePatchBase<BinaryType>>> patches,
        List<FilePatch> appliedPatches
    ) throws IOException {
        for (Pair<VirtualFile, ApplyFilePatchBase<BinaryType>> patch : patches) {
            if (appliedPatches.contains(patch.getSecond().getPatch())) {
                myVerifier.doMoveIfNeeded(patch.getFirst());
            }
        }
    }

    @RequiredUIAccess
    private <V extends FilePatch, T extends ApplyFilePatchBase<V>> ApplyPatchStatus applyList(
        List<Pair<VirtualFile, T>> patches,
        ApplyPatchContext context,
        ApplyPatchStatus status,
        CommitContext commiContext
    ) throws IOException {
        for (Pair<VirtualFile, T> patch : patches) {
            ApplyPatchAttempt attempt = ApplyPatchAction.applyOnly(
                myProject,
                patch.getSecond(),
                context,
                patch.getFirst(),
                commiContext,
                myReverseConflict,
                myLeftConflictPanelTitle,
                myRightConflictPanelTitle
            );
            if (attempt.isMergeNeeded()) {
                myPendingMerges.add(attempt);
                continue;
            }

            ApplyPatchStatus patchStatus = attempt.getStatus();
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
    protected static void showApplyStatus(Project project, ApplyPatchStatus status) {
        if (status == ApplyPatchStatus.ALREADY_APPLIED) {
            showError(project, VcsLocalize.patchApplyAlreadyApplied(), false);
        }
        else if (status == ApplyPatchStatus.PARTIAL) {
            showError(project, VcsLocalize.patchApplyPartiallyApplied(), false);
        }
        else if (ApplyPatchStatus.SUCCESS.equals(status)) {
            NotificationService.getInstance()
                .newInfo(VcsBalloonProblemNotifier.NOTIFICATION_GROUP)
                .content(VcsLocalize.patchApplySuccessAppliedText())
                .notify(project);
        }
    }

    
    public List<FilePatch> getRemainingPatches() {
        return myRemainingPatches;
    }

    private ReadonlyStatusHandler.OperationStatus getReadOnlyFilesStatus(List<VirtualFile> filesToMakeWritable) {
        VirtualFile[] fileArray = VirtualFileUtil.toVirtualFileArray(filesToMakeWritable);
        return ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(fileArray);
    }

    @RequiredUIAccess
    public static void showError(Project project, LocalizeValue message, boolean error) {
        showError(project, message.get(), error);
    }

    @RequiredUIAccess
    public static void showError(Project project, String message, boolean error) {
        Application application = Application.get();
        if (application.isUnitTestMode()) {
            return;
        }
        LocalizeValue title = VcsLocalize.patchApplyDialogTitle();
        LocalizeValue text = LocalizeValue.ofNullable(message);
        project.getUIAccess().give(() -> {
            if (error) {
                MessageBoxes.okError(text).title(title).showAsync(project);
            }
            else {
                MessageBoxes.okInfo(text).title(title).showAsync(project);
            }
        });
    }

    private static class FilesMover implements Consumer<Collection<FilePath>> {
        private final ChangeListManager myChangeListManager;
        private final LocalChangeList myTargetChangeList;

        public FilesMover(ChangeListManager changeListManager, LocalChangeList targetChangeList) {
            myChangeListManager = changeListManager;
            myTargetChangeList = targetChangeList;
        }

        @Override
        public void accept(Collection<FilePath> directlyAffected) {
            List<Change> changes = new ArrayList<>();
            for (FilePath file : directlyAffected) {
                Change change = myChangeListManager.getChange(file);
                if (change != null) {
                    changes.add(change);
                }
            }

            myChangeListManager.moveChangesTo(myTargetChangeList, changes.toArray(new Change[changes.size()]));
        }
    }
}
