// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status.widget;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.fileEditor.util.StatusBarUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class StatusBarEditorBasedWidgetFactory implements StatusBarWidgetFactory {
  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return true;
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return getTextEditor(statusBar) != null;
  }

  @Nullable
  protected FileEditor getFileEditor(@Nonnull StatusBar statusBar) {
    return StatusBarUtil.getCurrentFileEditor(statusBar);
  }

  @Nullable
  protected Editor getTextEditor(@Nonnull StatusBar statusBar) {
    return StatusBarUtil.getCurrentTextEditor(statusBar);
  }
}
