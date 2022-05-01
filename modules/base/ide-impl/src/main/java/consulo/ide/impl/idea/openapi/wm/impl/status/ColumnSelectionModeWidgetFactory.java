// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import consulo.ui.ex.UIBundle;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

public class ColumnSelectionModeWidgetFactory extends StatusBarEditorBasedWidgetFactory {
  @Override
  public
  @Nonnull
  String getId() {
    return StatusBar.StandardWidgets.COLUMN_SELECTION_MODE_PANEL;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.selection.mode.widget.name");
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    Editor editor = getTextEditor(statusBar);
    return editor != null && editor.isColumnMode();
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new ColumnSelectionModePanel(project);
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }
}
