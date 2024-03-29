// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.ui.ex.UIBundle;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "inspectionProfileWidget", order = "after readOnlyWidget")
public class InspectionProfileWidgetFactory extends StatusBarEditorBasedWidgetFactory {
  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return false; // Possibly add a Registry key
  }

  @Override
  public
  @Nonnull
  String getId() {
    return TogglePopupHintsPanel.ID;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.inspection.profile.widget.name");
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new TogglePopupHintsPanel(project);
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }
}
