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

package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.application.Application;
import consulo.diff.DiffManager;
import consulo.diff.InvalidDiffRequestException;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.diff.impl.patch.*;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.ApplyFilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.ApplyFilePatchBase;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.GenericPatchApplier;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.change.patch.PatchEP;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static consulo.ide.impl.idea.openapi.vcs.changes.patch.PatchFileType.isPatchFile;

public class ApplyPatchAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ApplyPatchAction.class);

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (isProjectOrScopeView(e.getPlace())) {
            VirtualFile vFile = e.getData(VirtualFile.KEY);
            e.getPresentation().setEnabledAndVisible(project != null && isPatchFile(vFile));
        }
        else {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(project != null);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not apply patch now")) {
            return;
        }
        FileDocumentManager.getInstance().saveAllDocuments();

        VirtualFile vFile = null;
        String place = e.getPlace();
        if (isProjectOrScopeView(place) || ActionPlaces.MAIN_MENU.equals(place)) {
            vFile = e.getData(VirtualFile.KEY);
        }
        if (isPatchFile(vFile)) {
            showApplyPatch(project, vFile);
        }
        else {
            FileChooserDescriptor descriptor = getDialogFactory(project).createSelectPatchDescriptor();
            VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
            VirtualFile toSelect = settings.PATCH_STORAGE_LOCATION == null
                ? null
                : LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(settings.PATCH_STORAGE_LOCATION));

            FileChooser.chooseFile(descriptor, project, toSelect).doWhenDone(file -> {
                VirtualFile parent = file.getParent();
                if (parent != null) {
                    settings.PATCH_STORAGE_LOCATION = FileUtil.toSystemDependentName(parent.getPath());
                }
                showApplyPatch(project, file);
            });
        }
    }

    private static boolean isProjectOrScopeView(@Nonnull String place) {
        return place.equals(ActionPlaces.PROJECT_VIEW_POPUP) || place.equals(ActionPlaces.SCOPE_VIEW_POPUP);
    }

    // used by TeamCity plugin
    @RequiredUIAccess
    public static void showApplyPatch(@Nonnull Project project, @Nonnull VirtualFile file) {
        DialogWrapper dialog = getDialogFactory(project).create(
            project,
            new ApplyPatchDefaultExecutor(project),
            Collections.<ApplyPatchExecutor>singletonList(new ImportToShelfExecutor(project)),
            ApplyPatchMode.APPLY,
            file
        );
        dialog.show();
    }

    @RequiredUIAccess
    public static Boolean showAndGetApplyPatch(@Nonnull Project project, @Nonnull File file) {
        VirtualFile vFile = VfsUtil.findFileByIoFile(file, true);
        String patchPath = file.getPath();
        if (vFile == null) {
            NotificationService.getInstance()
                .newError(VcsNotifier.NOTIFICATION_GROUP_ID)
                .content(LocalizeValue.localizeTODO("Can't find patch file " + patchPath))
                .notify(project);
            return false;
        }
        if (!isPatchFile(file)) {
            NotificationService.getInstance()
                .newError(VcsNotifier.NOTIFICATION_GROUP_ID)
                .content(LocalizeValue.localizeTODO("Selected file " + patchPath + " is not patch type file "))
                .notify(project);
            return false;
        }

        DialogWrapper dialog = getDialogFactory(project).create(
            project,
            new ApplyPatchDefaultExecutor(project),
            Collections.emptyList(),
            ApplyPatchMode.APPLY_PATCH_IN_MEMORY,
            vFile
        );
        dialog.setModal(true);
        return dialog.showAndGet();
    }

    public static void applySkipDirs(List<FilePatch> patches, int skipDirs) {
        if (skipDirs < 1) {
            return;
        }
        for (FilePatch patch : patches) {
            patch.setBeforeName(skipN(patch.getBeforeName(), skipDirs));
            patch.setAfterName(skipN(patch.getAfterName(), skipDirs));
        }
    }

    private static String skipN(String path, int num) {
        String[] pieces = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = num; i < pieces.length; i++) {
            String piece = pieces[i];
            sb.append('/').append(piece);
        }
        return sb.toString();
    }

    @Nonnull
    @RequiredUIAccess
    public static ApplyPatchStatus applyOnly(
        @Nullable Project project,
        @Nonnull ApplyFilePatchBase patch,
        @Nullable ApplyPatchContext context,
        @Nonnull VirtualFile file,
        @Nullable CommitContext commitContext,
        boolean reverse,
        @Nullable String leftPanelTitle,
        @Nullable String rightPanelTitle
    ) {
        ApplyFilePatch.Result result = tryApplyPatch(project, patch, context, file, commitContext);

        ApplyPatchStatus status = result.getStatus();
        if (ApplyPatchStatus.ALREADY_APPLIED.equals(status) || ApplyPatchStatus.SUCCESS.equals(status)) {
            return status;
        }

        ApplyPatchForBaseRevisionTexts mergeData = result.getMergeData();
        if (mergeData == null) {
            return status;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return ApplyPatchStatus.FAILURE;
        }

        String baseContent = toString(mergeData.getBase());
        String localContent = toString(mergeData.getLocal());
        String patchedContent = mergeData.getPatched();

        if (localContent == null) {
            return ApplyPatchStatus.FAILURE;
        }

        SimpleReference<ApplyPatchStatus> applyPatchStatusReference = SimpleReference.create();
        Consumer<MergeResult> callback = result1 -> {
            FileDocumentManager.getInstance().saveDocument(document);
            applyPatchStatusReference.setIfNull(
                result1 != MergeResult.CANCEL ? ApplyPatchStatus.SUCCESS : ApplyPatchStatus.FAILURE
            );
        };

        try {
            MergeRequest request;
            if (baseContent != null) {
                if (reverse) {
                    if (leftPanelTitle == null) {
                        leftPanelTitle = VcsLocalize.patchApplyConflictPatchedVersion().get();
                    }
                    if (rightPanelTitle == null) {
                        rightPanelTitle = VcsLocalize.patchApplyConflictLocalVersion().get();
                    }

                    List<String> contents = ContainerUtil.list(patchedContent, baseContent, localContent);
                    List<String> titles = ContainerUtil.list(leftPanelTitle, null, rightPanelTitle);

                    request = PatchDiffRequestFactory
                        .createMergeRequest(project, document, file, contents, null, titles, callback);
                }
                else {
                    request = PatchDiffRequestFactory
                        .createMergeRequest(project, document, file, baseContent, localContent, patchedContent, callback);
                }
            }
            else {
                TextFilePatch textPatch = (TextFilePatch)patch.getPatch();
                GenericPatchApplier applier = new GenericPatchApplier(localContent, textPatch.getHunks());
                applier.execute();

                AppliedTextPatch appliedTextPatch = AppliedTextPatch.create(applier.getAppliedInfo());
                request = PatchDiffRequestFactory.createBadMergeRequest(
                    project,
                    document,
                    file,
                    localContent,
                    appliedTextPatch,
                    callback
                );
            }
            request.putUserData(
                DiffUserDataKeysEx.MERGE_ACTION_CAPTIONS,
                result12 -> result12.equals(MergeResult.CANCEL) ? "Abort..." : null
            );
            request.putUserData(
                DiffUserDataKeysEx.MERGE_CANCEL_HANDLER,
                viewer -> {
                    int result13 = Messages.showYesNoCancelDialog(
                        viewer.getComponent().getRootPane(),
                        "Would you like to (A)bort&Rollback applying patch action or (S)kip this file?",
                        "Close Merge",
                        "_Abort",
                        "_Skip",
                        CommonLocalize.buttonCancel().get(),
                        UIUtil.getQuestionIcon()
                    );

                    if (result13 == Messages.YES) {
                        applyPatchStatusReference.set(ApplyPatchStatus.ABORT);
                    }
                    else if (result13 == Messages.NO) {
                        applyPatchStatusReference.set(ApplyPatchStatus.SKIP);
                    }
                    return result13 != Messages.CANCEL;
                }
            );
            DiffManager.getInstance().showMerge(project, request);
            return applyPatchStatusReference.get();
        }
        catch (InvalidDiffRequestException e) {
            LOG.warn(e);
            return ApplyPatchStatus.FAILURE;
        }
    }

    @Nonnull
    @RequiredUIAccess
    private static ApplyFilePatch.Result tryApplyPatch(
        @Nullable Project project,
        @Nonnull ApplyFilePatchBase patch,
        @Nullable ApplyPatchContext context,
        @Nonnull VirtualFile file,
        @Nullable CommitContext commitContext
    ) {
        FilePatch patchBase = patch.getPatch();
        return Application.get().runWriteAction((Supplier<ApplyFilePatch.Result>)() -> {
            try {
                return patch.apply(
                    file,
                    context,
                    project,
                    VcsUtil.getFilePath(file),
                    () -> {
                        BaseRevisionTextPatchEP baseRevisionTextPatchEP =
                            PatchEP.EP_NAME.findExtensionOrFail(project, BaseRevisionTextPatchEP.class);
                        String path = ObjectUtil.chooseNotNull(patchBase.getBeforeName(), patchBase.getAfterName());
                        return baseRevisionTextPatchEP.provideContent(path, commitContext);
                    },
                    commitContext
                );
            }
            catch (IOException e) {
                LOG.error(e);
                return ApplyFilePatch.Result.createThrow(e);
            }
        });
    }

    @Nullable
    private static String toString(@Nullable CharSequence charSequence) {
        return charSequence != null ? StringUtil.convertLineSeparators(charSequence.toString()) : null;
    }

    @Nonnull
    private static ApplyPatchDifferentiatedDialogFactory getDialogFactory(@Nonnull Project project) {
        return project.getApplication().getInstance(ApplyPatchDifferentiatedDialogFactory.class);
    }
}
