// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.UIBundle;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "positionWidget")
public class PositionPanelWidgetFactory extends StatusBarEditorBasedWidgetFactory {
  @Override
  @Nonnull
  public String getDisplayName() {
    return UIBundle.message("status.bar.position.widget.name");
  }

  @Override
  @Nonnull
  public StatusBarWidget createWidget(@Nonnull Project project) {
    return new PositionPanel(project, this);
  }
}
