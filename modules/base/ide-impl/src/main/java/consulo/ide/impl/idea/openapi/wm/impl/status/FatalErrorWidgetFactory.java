// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.diagnostic.IdeMessagePanel;
import consulo.ide.impl.idea.diagnostic.MessagePool;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.UIBundle;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

@ExtensionImpl(id = "fatalErrorWidget", order = "after notificationsWidget")
public class FatalErrorWidgetFactory implements StatusBarWidgetFactory {
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
  @Nonnull
  public StatusBarWidget createWidget(@Nonnull Project project) {
    return new IdeMessagePanel(project, this, WindowManager.getInstance().getIdeFrame(project), MessagePool.getInstance());
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
