// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.UIBundle;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "notificationsWidget", order = "after readOnlyWidget")
public class NotificationWidgetFactory implements StatusBarWidgetFactory {
  public static boolean isAvailable() {
    return UISettings.getInstance().getHideToolStripes() || UISettings.getInstance().getPresentationMode();
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return UIBundle.message("status.bar.notifications.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return isAvailable();
  }

  @Override
  @Nonnull
  public StatusBarWidget createWidget(@Nonnull Project project) {
    return new IdeNotificationArea(this);
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return isAvailable();
  }
}
