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

import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.diff.DiffManager;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.diff.InvalidDiffRequestException;
import consulo.ide.impl.idea.diff.merge.MergeTool;
import consulo.ide.impl.idea.diff.util.DiffUserDataKeysEx;
import consulo.ide.impl.idea.openapi.diff.impl.patch.*;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.ApplyFilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.ApplyFilePatchBase;
import consulo.ide.impl.idea.openapi.diff.impl.patch.apply.GenericPatchApplier;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.VcsNotifier;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.util.lang.ObjectUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.function.Condition;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static consulo.ide.impl.idea.openapi.vcs.changes.patch.PatchFileType.isPatchFile;

public class ApplyPatchAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(ApplyPatchAction.class);

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (isProjectOrScopeView(e.getPlace())) {
      VirtualFile vFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
      e.getPresentation().setEnabledAndVisible(project != null && isPatchFile(vFile));
    }
    else {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(project != null);
    }
  }

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not apply patch now")) return;
    FileDocumentManager.getInstance().saveAllDocuments();

    VirtualFile vFile = null;
    final String place = e.getPlace();
    if (isProjectOrScopeView(place) || ActionPlaces.MAIN_MENU.equals(place)) {
      vFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    }
    if (isPatchFile(vFile)) {
      showApplyPatch(project, vFile);
    }
    else {
      final FileChooserDescriptor descriptor = ApplyPatchDifferentiatedDialog.createSelectPatchDescriptor();
      final VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
      final VirtualFile toSelect = settings.PATCH_STORAGE_LOCATION == null ? null :
                                   LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(settings.PATCH_STORAGE_LOCATION));

      FileChooser.chooseFile(descriptor, project, toSelect).doWhenDone(file -> {
        final VirtualFile parent = file.getParent();
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
  public static void showApplyPatch(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    final ApplyPatchDifferentiatedDialog dialog = new ApplyPatchDifferentiatedDialog(
            project, new ApplyPatchDefaultExecutor(project),
            Collections.<ApplyPatchExecutor>singletonList(new ImportToShelfExecutor(project)), ApplyPatchMode.APPLY, file);
    dialog.show();
  }

  @RequiredUIAccess
  public static Boolean showAndGetApplyPatch(@Nonnull final Project project, @Nonnull final File file) {
    VirtualFile vFile = VfsUtil.findFileByIoFile(file, true);
    String patchPath = file.getPath();
    if (vFile == null) {
      VcsNotifier.getInstance(project).notifyWeakError("Can't find patch file " + patchPath);
      return false;
    }
    if (!isPatchFile(file)) {
      VcsNotifier.getInstance(project).notifyWeakError("Selected file " + patchPath + " is not patch type file ");
      return false;
    }
    final ApplyPatchDifferentiatedDialog dialog = new ApplyPatchDifferentiatedDialog(project, new ApplyPatchDefaultExecutor(project),
                                                                                     Collections.emptyList(),
                                                                                     ApplyPatchMode.APPLY_PATCH_IN_MEMORY, vFile);
    dialog.setModal(true);
    return dialog.showAndGet();
  }

  public static void applySkipDirs(final List<FilePatch> patches, final int skipDirs) {
    if (skipDirs < 1) {
      return;
    }
    for (FilePatch patch : patches) {
      patch.setBeforeName(skipN(patch.getBeforeName(), skipDirs));
      patch.setAfterName(skipN(patch.getAfterName(), skipDirs));
    }
  }

  private static String skipN(final String path, final int num) {
    final String[] pieces = path.split("/");
    final StringBuilder sb = new StringBuilder();
    for (int i = num; i < pieces.length; i++) {
      final String piece = pieces[i];
      sb.append('/').append(piece);
    }
    return sb.toString();
  }

  @Nonnull
  public static ApplyPatchStatus applyOnly(@javax.annotation.Nullable final Project project,
                                           @Nonnull final ApplyFilePatchBase patch,
                                           @javax.annotation.Nullable final ApplyPatchContext context,
                                           @Nonnull final VirtualFile file,
                                           @javax.annotation.Nullable final CommitContext commitContext,
                                           boolean reverse,
                                           @javax.annotation.Nullable String leftPanelTitle,
                                           @javax.annotation.Nullable String rightPanelTitle) {
    final ApplyFilePatch.Result result = tryApplyPatch(project, patch, context, file, commitContext);

    final ApplyPatchStatus status = result.getStatus();
    if (ApplyPatchStatus.ALREADY_APPLIED.equals(status) || ApplyPatchStatus.SUCCESS.equals(status)) {
      return status;
    }

    final ApplyPatchForBaseRevisionTexts mergeData = result.getMergeData();
    if (mergeData == null) return status;

    final Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return ApplyPatchStatus.FAILURE;

    String baseContent = toString(mergeData.getBase());
    String localContent = toString(mergeData.getLocal());
    String patchedContent = mergeData.getPatched();

    if (localContent == null) return ApplyPatchStatus.FAILURE;

    final Ref<ApplyPatchStatus> applyPatchStatusReference = new Ref<>();
    Consumer<MergeResult> callback = result1 -> {
      FileDocumentManager.getInstance().saveDocument(document);
      applyPatchStatusReference.setIfNull(result1 != MergeResult.CANCEL ? ApplyPatchStatus.SUCCESS : ApplyPatchStatus.FAILURE);
    };

    try {
      MergeRequest request;
      if (baseContent != null) {
        if (reverse) {
          if (leftPanelTitle == null) leftPanelTitle = VcsBundle.message("patch.apply.conflict.patched.version");
          if (rightPanelTitle == null) rightPanelTitle = VcsBundle.message("patch.apply.conflict.local.version");

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
        final GenericPatchApplier applier = new GenericPatchApplier(localContent, textPatch.getHunks());
        applier.execute();

        final AppliedTextPatch appliedTextPatch = AppliedTextPatch.create(applier.getAppliedInfo());
        request = PatchDiffRequestFactory.createBadMergeRequest(project, document, file, localContent, appliedTextPatch, callback);
      }
      request.putUserData(DiffUserDataKeysEx.MERGE_ACTION_CAPTIONS, result12 -> result12.equals(MergeResult.CANCEL) ? "Abort..." : null);
      request.putUserData(DiffUserDataKeysEx.MERGE_CANCEL_HANDLER, new Condition<MergeTool.MergeViewer>() {
        @Override
        public boolean value(MergeTool.MergeViewer viewer) {
          int result = Messages.showYesNoCancelDialog(viewer.getComponent().getRootPane(),
                                                      "Would you like to (A)bort&Rollback applying patch action or (S)kip this file?",
                                                      "Close Merge",
                                                      "_Abort", "_Skip", "Cancel", Messages.getQuestionIcon());

          if (result == Messages.YES) {
            applyPatchStatusReference.set(ApplyPatchStatus.ABORT);
          }
          else if (result == Messages.NO) {
            applyPatchStatusReference.set(ApplyPatchStatus.SKIP);
          }
          return result != Messages.CANCEL;
        }
      });
      DiffManager.getInstance().showMerge(project, request);
      return applyPatchStatusReference.get();
    }
    catch (InvalidDiffRequestException e) {
      LOG.warn(e);
      return ApplyPatchStatus.FAILURE;
    }
  }

  @Nonnull
  private static ApplyFilePatch.Result tryApplyPatch(@javax.annotation.Nullable final Project project,
                                                     @Nonnull final ApplyFilePatchBase patch,
                                                     @javax.annotation.Nullable final ApplyPatchContext context,
                                                     @Nonnull final VirtualFile file,
                                                     @javax.annotation.Nullable final CommitContext commitContext) {
    final FilePatch patchBase = patch.getPatch();
    return ApplicationManager.getApplication().runWriteAction(new Computable<ApplyFilePatch.Result>() {
              @Override
              public ApplyFilePatch.Result compute() {
                try {
                  return patch.apply(file, context, project, VcsUtil.getFilePath(file), new Getter<CharSequence>() {
                    @Override
                    public CharSequence get() {
                      final BaseRevisionTextPatchEP baseRevisionTextPatchEP = PatchEP.EP_NAME.findExtensionOrFail(project, BaseRevisionTextPatchEP.class);
                      final String path = ObjectUtil.chooseNotNull(patchBase.getBeforeName(), patchBase.getAfterName());
                      return baseRevisionTextPatchEP.provideContent(path, commitContext);
                    }
                  }, commitContext);
                }
                catch (IOException e) {
                  LOG.error(e);
                  return ApplyFilePatch.Result.createThrow(e);
                }
              }
            });
  }

  @javax.annotation.Nullable
  private static String toString(@javax.annotation.Nullable CharSequence charSequence) {
    return charSequence != null ? StringUtil.convertLineSeparators(charSequence.toString()) : null;
  }
}
