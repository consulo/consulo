// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.ui.ex.UIBundle;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "encodingWidget", order = "after lineSeparatorWidget")
public class EncodingPanelWidgetFactory extends StatusBarEditorBasedWidgetFactory {
  @Override
  public
  @Nonnull
  String getId() {
    return StatusBar.StandardWidgets.ENCODING_PANEL;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.encoding.widget.name");
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new EncodingPanel(project);
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }
}
