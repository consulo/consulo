// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.statusBar;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import org.jspecify.annotations.Nullable;

public abstract class StatusBarEditorBasedWidgetFactory implements StatusBarWidgetFactory {
  @Override
  public boolean isAvailable(Project project) {
    return true;
  }

  @Override
  public boolean canBeEnabledOn(StatusBar statusBar) {
    return getTextEditor(statusBar) != null;
  }

  protected @Nullable FileEditor getFileEditor(StatusBar statusBar) {
    return StatusBarUtil.getCurrentFileEditor(statusBar);
  }

  protected @Nullable Editor getTextEditor(StatusBar statusBar) {
    return StatusBarUtil.getCurrentTextEditor(statusBar);
  }
}
