// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.file.exclude;

import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * removes destruction caused by {@link OverrideFileTypeAction} and restores the original file type
 */
public class ReverteOverrideFileTypeAction extends AnAction {
  @Override
  @RequiredUIAccess
  public void update(@Nonnull AnActionEvent e) {
    VirtualFile[] files =
      OverrideFileTypeAction.getContextFiles(e, file -> OverrideFileTypeManager.getInstance().getFileValue(file) != null);
    Presentation presentation = e.getPresentation();
    boolean enabled = files.length != 0;
    presentation.setDescriptionValue(
      enabled
        ? ActionLocalize.actionReverteoverridefiletypeactionVerboseDescription(files[0].getName(), files.length - 1)
        : ActionLocalize.actionReverteoverridefiletypeactionDescription()
    );
    presentation.setEnabledAndVisible(enabled);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    VirtualFile[] files = OverrideFileTypeAction.getContextFiles(e, file -> OverrideFileTypeManager.getInstance().getFileValue(file) != null);
    for (VirtualFile file : files) {
      OverrideFileTypeManager.getInstance().removeFile(file);
    }
  }
}
