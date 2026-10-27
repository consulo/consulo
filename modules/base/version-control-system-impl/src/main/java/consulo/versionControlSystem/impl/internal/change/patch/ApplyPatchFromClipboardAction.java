// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.annotation.component.ActionImpl;
import consulo.document.FileDocumentManager;
import consulo.language.file.light.LightVirtualFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ActionImpl(id = "ChangesView.ApplyPatchFromClipboard")
public class ApplyPatchFromClipboardAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    public ApplyPatchFromClipboardAction() {
        super(
            ActionLocalize.actionChangesviewApplypatchfromclipboardText(),
            ActionLocalize.actionChangesviewApplypatchfromclipboardDescription()
        );
    }

    /**
     * The clipboard answers as a future on a frontend which keeps it in a browser, so the update awaits it
     * rather than reading it in place.
     */
    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(Project.KEY);

        if (project == null || ChangeListManager.getInstance(project).isFreezed() != null) {
            presentation.setEnabled(false);
            return Coroutine.first(CodeExecution.apply(input -> null));
        }

        return UIAction.apply((i) -> getTextInClipboard())
            .toCoroutine()
            .then(CompletableFutureStep.await(Function.identity()))
            // allow to apply from clipboard even if we do not detect it as a patch, because during applying we
            // parse content more precisely
            .then(CodeExecution.<String, Void>apply(text -> {
                presentation.setEnabled(text != null);
                return null;
            }));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(VcsLocalize.patchApplyCannotApplyNow().get())) {
            return;
        }
        FileDocumentManager.getInstance().saveAllDocuments();

        UIAccess uiAccess = UIAccess.current();
        getTextInClipboard().whenCompleteAsync((clipboardText, throwable) -> {
            if (throwable == null && clipboardText != null) {
                new MyApplyPatchFromClipboardDialog(project, clipboardText).showAsync();
            }
        }, uiAccess);
    }

    private static CompletableFuture<@Nullable String> getTextInClipboard() {
        return CopyPasteManager.getInstance().getContentsAsync(DataTransferType.TEXT);
    }

    public static class MyApplyPatchFromClipboardDialog extends ApplyPatchDifferentiatedDialog {
        @RequiredUIAccess
        public MyApplyPatchFromClipboardDialog(Project project, String clipboardText) {
            super(
                project,
                new ApplyPatchDefaultExecutor(project),
                List.of(),
                ApplyPatchMode.APPLY_PATCH_IN_MEMORY,
                new LightVirtualFile("clipboardPatchFile", clipboardText),
                null,
                null,
                null,
                null,
                null,
                false
            );
        }

        @Override
        @RequiredUIAccess
        protected @Nullable Component createDoNotAskCheckbox() {
            CheckBox removeOptionCheckBox = CheckBox.create(VcsLocalize.patchApplyAnalyzeFromClipboardOnTheFlyCheckbox());
            removeOptionCheckBox.setValue(VcsApplicationSettings.getInstance().getState().DETECT_PATCH_ON_THE_FLY);
            removeOptionCheckBox.addValueListener(
                event -> VcsApplicationSettings.getInstance().getState().DETECT_PATCH_ON_THE_FLY = removeOptionCheckBox.getValueOrError()
            );
            return removeOptionCheckBox;
        }
    }
}
