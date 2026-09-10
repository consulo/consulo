// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.annotation.component.ActionImpl;
import consulo.desktop.awt.internal.versionControlSystem.change.shelf.ApplyPatchDifferentiatedDialog;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.application.ex.ClipboardUtil;
import consulo.language.file.light.LightVirtualFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.ex.action.*;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchDefaultExecutor;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchMode;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Collections;
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

        return UIAction.apply((i) -> ClipboardUtil.getTextInClipboard())
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
        ClipboardUtil.getTextInClipboard().whenCompleteAsync((clipboardText, throwable) -> {
            if (throwable == null && clipboardText != null) {
                new MyApplyPatchFromClipboardDialog(project, clipboardText).show();
            }
        }, uiAccess);
    }

    public static class MyApplyPatchFromClipboardDialog extends ApplyPatchDifferentiatedDialog {
        public MyApplyPatchFromClipboardDialog(Project project, String clipboardText) {
            super(
                project,
                new ApplyPatchDefaultExecutor(project),
                Collections.emptyList(),
                ApplyPatchMode.APPLY_PATCH_IN_MEMORY,
                new LightVirtualFile("clipboardPatchFile", clipboardText),
                null,
                null,
                //NON-NLS
                null,
                null,
                null,
                false
            );
        }

        @Override
        protected @Nullable JComponent createDoNotAskCheckbox() {
            return createAnalyzeOnTheFlyOptionPanel();
        }

        
        private static JCheckBox createAnalyzeOnTheFlyOptionPanel() {
            JCheckBox removeOptionCheckBox =
                new JCheckBox(VcsLocalize.patchApplyAnalyzeFromClipboardOnTheFlyCheckbox().get());
            removeOptionCheckBox.setMnemonic(KeyEvent.VK_L);
            removeOptionCheckBox.setSelected(VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY);
            removeOptionCheckBox.addActionListener(
                e -> VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY = removeOptionCheckBox.isSelected()
            );
            return removeOptionCheckBox;
        }
    }
}
