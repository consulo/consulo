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
package com.intellij.openapi.ui;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.util.ui.JBUI;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.*;

public class MessageType {

  public static final MessageType ERROR =
          new MessageType(AllIcons.General.NotificationError, JBUI.CurrentTheme.NotificationError.backgroundColor(), JBUI.CurrentTheme.NotificationError.foregroundColor(),
                          JBUI.CurrentTheme.NotificationError.borderColor());

  public static final MessageType INFO = new MessageType(AllIcons.General.NotificationInfo, JBUI.CurrentTheme.NotificationInfo.backgroundColor(), JBUI.CurrentTheme.NotificationInfo.foregroundColor(),
                                                         JBUI.CurrentTheme.NotificationInfo.borderColor());

  public static final MessageType WARNING =
          new MessageType(AllIcons.General.NotificationWarning, JBUI.CurrentTheme.NotificationWarning.backgroundColor(), JBUI.CurrentTheme.NotificationWarning.foregroundColor(),
                          JBUI.CurrentTheme.NotificationWarning.borderColor());

  private final Image myDefaultIcon;
  private final Color myPopupBackground;
  private final Color myForeground;
  private final Color myBorderColor;

  private MessageType(@Nonnull Image defaultIcon, @Nonnull Color popupBackground, @Nonnull Color foreground, @Nonnull Color borderColor) {
    myDefaultIcon = defaultIcon;
    myPopupBackground = popupBackground;
    myForeground = foreground;
    myBorderColor = borderColor;
  }

  @Nonnull
  public Image getDefaultIcon() {
    return myDefaultIcon;
  }

  @Nonnull
  public Color getPopupBackground() {
    return myPopupBackground;
  }

  @Nonnull
  public Color getTitleForeground() {
    return myForeground;
  }

  @Nonnull
  public Color getBorderColor() {
    return myBorderColor;
  }

  @Nonnull
  public NotificationType toNotificationType() {
    return this == ERROR ? NotificationType.ERROR : this == WARNING ? NotificationType.WARNING : NotificationType.INFORMATION;
  }
}
