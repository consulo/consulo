// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.diagnostic.IdeMessagePanel;
import consulo.ide.impl.idea.diagnostic.MessagePool;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.UIBundle;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "fatalErrorWidget", order = "after notificationsWidget")
public class FatalErrorWidgetFactory implements StatusBarWidgetFactory {
  @Override
  public
  @Nonnull
  String getId() {
    return IdeMessagePanel.FATAL_ERROR;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.fatal.error.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return true;
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new IdeMessagePanel(WindowManager.getInstance().getIdeFrame(project), MessagePool.getInstance());
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Override
  public boolean isConfigurable() {
    return false;
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return false;
  }
}
