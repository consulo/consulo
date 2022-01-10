// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.notification.impl.widget;

import com.intellij.ide.ui.UISettings;
import com.intellij.notification.impl.IdeNotificationArea;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.ui.UIBundle;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

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
