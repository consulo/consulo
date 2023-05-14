// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.impl.idea.openapi.application.ex.ClipboardUtil;
import consulo.document.FileDocumentManager;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.language.file.light.LightVirtualFile;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Collections;

public class ApplyPatchFromClipboardAction extends DumbAwareAction {

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    String text = ClipboardUtil.getTextInClipboard();
    // allow to apply from clipboard even if we do not detect it as a patch, because during applying we parse content more precisely
    e.getPresentation().setEnabled(project != null && text != null && ChangeListManager.getInstance(project).isFreezed() == null);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    if (ChangeListManager.getInstance(project).isFreezedWithNotification(VcsBundle.message("patch.apply.cannot.apply.now"))) return;
    FileDocumentManager.getInstance().saveAllDocuments();

    String clipboardText = ClipboardUtil.getTextInClipboard();
    assert clipboardText != null;
    new MyApplyPatchFromClipboardDialog(project, clipboardText).show();
  }

  public static class MyApplyPatchFromClipboardDialog extends ApplyPatchDifferentiatedDialog {

    public MyApplyPatchFromClipboardDialog(@Nonnull Project project, @Nonnull String clipboardText) {
      super(project, new ApplyPatchDefaultExecutor(project), Collections.emptyList(), ApplyPatchMode.APPLY_PATCH_IN_MEMORY, new LightVirtualFile("clipboardPatchFile", clipboardText), null, null,
            //NON-NLS
            null, null, null, false);
    }

    @Nullable
    @Override
    protected JComponent createDoNotAskCheckbox() {
      return createAnalyzeOnTheFlyOptionPanel();
    }

    @Nonnull
    private static JCheckBox createAnalyzeOnTheFlyOptionPanel() {
      final JCheckBox removeOptionCheckBox = new JCheckBox(VcsBundle.message("patch.apply.analyze.from.clipboard.on.the.fly.checkbox"));
      removeOptionCheckBox.setMnemonic(KeyEvent.VK_L);
      removeOptionCheckBox.setSelected(VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY);
      removeOptionCheckBox.addActionListener(e -> VcsApplicationSettings.getInstance().DETECT_PATCH_ON_THE_FLY = removeOptionCheckBox.isSelected());
      return removeOptionCheckBox;
    }
  }
}
