/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.notification.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.notification.EventLog;
import com.intellij.notification.LogModel;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.ui.NotificationsUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.messages.MessageBusConnection;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.ComponentColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author spleaner
 */
public class IdeNotificationArea implements CustomStatusBarWidget, IconLikeCustomStatusBarWidget {
  public static final String WIDGET_ID = "Notifications";
  private StatusBar myStatusBar;

  private Label myLabel;

  public IdeNotificationArea() {
    myLabel = Label.create();
    myLabel.addClickListener(event -> EventLog.toggleLog(getProject(), null));

    MessageBusConnection connection = Application.get().getMessageBus().connect(this);
    connection.subscribe(UISettingsListener.TOPIC, source -> updateStatus());
    connection.subscribe(LogModel.LOG_MODEL_CHANGED, () -> Application.get().invokeLater(IdeNotificationArea.this::updateStatus));
  }

  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Override
  public void dispose() {
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    myStatusBar = statusBar;
    updateStatus();
  }

  @Nullable
  private Project getProject() {
    return myStatusBar == null ? null : myStatusBar.getProject();
  }

  @Override
  @Nonnull
  public String ID() {
    return WIDGET_ID;
  }

  @RequiredUIAccess
  private void updateStatus() {
    final Project project = getProject();
    ArrayList<Notification> notifications = EventLog.getLogModel(project).getNotifications();
    applyIconToStatusAndToolWindow(project, createIconWithNotificationCount(notifications));

    int count = notifications.size();
    myLabel.setToolTipText(count > 0 ? String.format("%s notification%s pending", count, count == 1 ? "" : "s") : "No new notifications");

    myStatusBar.updateWidget(ID());
  }

  @RequiredUIAccess
  private void applyIconToStatusAndToolWindow(Project project, Image icon) {
    if (UISettings.getInstance().HIDE_TOOL_STRIPES || UISettings.getInstance().PRESENTATION_MODE) {
      myLabel.setVisible(true);
      myLabel.setImage(icon);
    }
    else {
      ToolWindow eventLog = EventLog.getEventLog(project);
      if (eventLog != null) {
        eventLog.setIcon(icon);
      }
      myLabel.setVisible(false);
    }
  }

  @Nonnull
  private Image createIconWithNotificationCount(ArrayList<Notification> notifications) {
    return createIconWithNotificationCount(getMaximumType(notifications), notifications.size());
  }

  @Nonnull
  public static Image createIconWithNotificationCount(NotificationType type, int size) {
    Image mainIcon = getPendingNotificationsIcon(AllIcons.Ide.Notification.NoEvents, type);
    if (size > 0) {
      int width = AllIcons.Ide.Notification.NoEvents.getWidth();
      int height = AllIcons.Ide.Notification.NoEvents.getHeight();

      mainIcon = ImageEffects.layered(mainIcon, ImageEffects.canvas(width, height, ctx -> {
        ctx.setFont(FontManager.get().createFont(NotificationsUtil.getFontName(), 9, Font.STYLE_BOLD));

        ctx.setFillStyle(ComponentColors.LAYOUT);
        ctx.setTextAlign(Canvas2D.TextAlign.center);
        ctx.setTextBaseline(Canvas2D.TextBaseline.middle);

        int diff = size >= 10 ? 1 : 0;
        ctx.fillText(size < 10 ? String.valueOf(size) : "9", width / 2 - diff, height / 2 - 1);
        if(diff != 0) {
          ctx.fillText("+", width / 2 + 3, height / 2 - 1);
        }
      }));
    }

    return mainIcon;
  }

  @Nullable
  @Override
  public Component getUIComponent() {
    return myLabel;
  }

  @Override
  public boolean isUnified() {
    return true;
  }

  @Nonnull
  private static Image getPendingNotificationsIcon(Image defIcon, final NotificationType maximumType) {
    if (maximumType != null) {
      switch (maximumType) {
        case WARNING:
          return AllIcons.Ide.Notification.WarningEvents;
        case ERROR:
          return AllIcons.Ide.Notification.ErrorEvents;
        case INFORMATION:
          return AllIcons.Ide.Notification.InfoEvents;
      }
    }
    return defIcon;
  }

  @Nullable
  private static NotificationType getMaximumType(List<Notification> notifications) {
    NotificationType result = null;
    for (Notification notification : notifications) {
      if (NotificationType.ERROR == notification.getType()) {
        return NotificationType.ERROR;
      }

      if (NotificationType.WARNING == notification.getType()) {
        result = NotificationType.WARNING;
      }
      else if (result == null && NotificationType.INFORMATION == notification.getType()) {
        result = NotificationType.INFORMATION;
      }
    }

    return result;
  }
}
