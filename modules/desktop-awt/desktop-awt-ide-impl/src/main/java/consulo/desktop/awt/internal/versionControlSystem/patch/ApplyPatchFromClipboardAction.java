// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.annotation.component.ActionImpl;
import consulo.desktop.awt.internal.versionControlSystem.change.shelf.ApplyPatchDifferentiatedDialog;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.application.ex.ClipboardUtil;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchDefaultExecutor;
import consulo.versionControlSystem.impl.internal.change.patch.ApplyPatchMode;
import consulo.language.file.light.LightVirtualFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.ChangeListManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Collections;

@ActionImpl(id = "ChangesView.ApplyPatchFromClipboard")
public class ApplyPatchFromClipboardAction extends DumbAwareAction {
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        String text = ClipboardUtil.getTextInClipboard();
        // allow to apply from clipboard even if we do not detect it as a patch, because during applying we parse content more precisely
        e.getPresentation().setEnabled(project != null && text != null && ChangeListManager.getInstance(project).isFreezed() == null);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        if (ChangeListManager.getInstance(project).isFreezedWithNotification(VcsBundle.message("patch.apply.cannot.apply.now"))) {
            return;
        }
        FileDocumentManager.getInstance().saveAllDocuments();

        String clipboardText = ClipboardUtil.getTextInClipboard();
        assert clipboardText != null;
        new MyApplyPatchFromClipboardDialog(project, clipboardText).show();
    }

    public static class MyApplyPatchFromClipboardDialog extends ApplyPatchDifferentiatedDialog {
        public MyApplyPatchFromClipboardDialog(@Nonnull Project project, @Nonnull String clipboardText) {
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

        @Nullable
        @Override
        protected JComponent createDoNotAskCheckbox() {
            return createAnalyzeOnTheFlyOptionPanel();
        }

        @Nonnull
        private static JCheckBox createAnalyzeOnTheFlyOptionPanel() {
            JCheckBox removeOptionCheckBox =
                new JCheckBox(VcsBundle.message("patch.apply.analyze.from.clipboard.on.the.fly.checkbox"));
            removeOptionCheckBox.setMnemonic(KeyEvent.VK_L);
            removeOptionCheckBox.setSelected(VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY);
            removeOptionCheckBox.addActionListener(
                e -> VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY = removeOptionCheckBox.isSelected()
            );
            return removeOptionCheckBox;
        }
    }
}
