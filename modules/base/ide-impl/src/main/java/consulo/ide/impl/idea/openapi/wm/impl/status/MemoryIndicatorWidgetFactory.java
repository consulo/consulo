// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.UIBundle;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

@ExtensionImpl(order = "last")
public class MemoryIndicatorWidgetFactory implements StatusBarWidgetFactory {
  @Override
  @Nonnull
  public String getId() {
    return MemoryUsagePanel.WIDGET_ID;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.memory.usage.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return true;
  }

  @Override
  public boolean isEnabledByDefault() {
    return false;
  }

  @Override
  @Nonnull
  public StatusBarWidget createWidget(@Nonnull Project project) {
    return new MemoryUsagePanel(project);
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return true;
  }
}
