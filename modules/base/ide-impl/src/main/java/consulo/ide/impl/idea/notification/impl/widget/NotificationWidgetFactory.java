// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.notification.impl.widget;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.ide.impl.idea.notification.impl.IdeNotificationArea;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.UIBundle;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "notificationsWidget", order = "after inspectionProfileWidget")
public class NotificationWidgetFactory implements StatusBarWidgetFactory {
  public static boolean isAvailable() {
    return UISettings.getInstance().getHideToolStripes() || UISettings.getInstance().getPresentationMode();
  }

  @Override
  public
  @Nonnull
  String getId() {
    return IdeNotificationArea.WIDGET_ID;
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.notifications.widget.name");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return isAvailable();
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new IdeNotificationArea();
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return isAvailable();
  }
}
