/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.application.Application;
import consulo.ide.impl.idea.openapi.keymap.KeyboardSettingsExternalizable;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapPanel;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.project.ui.internal.NotificationsConfiguration;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.notification.*;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * @author Denis Fokin
 */
public class KeyboardInternationalizationNotificationManager {
  public static final NotificationGroup LOCALIZATION_GROUP = NotificationGroup.balloonGroup("Localization and Internationalization");

  public static boolean notificationHasBeenShown;

  private KeyboardInternationalizationNotificationManager() {
  }

  public static void showNotification() {

    if (notificationHasBeenShown) {
      return;
    }

    consulo.ui.Window window = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();
    if(!KeyboardSettingsExternalizable.isSupportedKeyboardLayout(TargetAWT.to(window))) {
      return;
    }

    MyNotificationListener listener = new MyNotificationListener();

    Notifications.Bus.notify(createNotification(LOCALIZATION_GROUP, listener));
    notificationHasBeenShown = true;
  }

  public static Notification createNotification(@Nonnull final NotificationGroup group, @Nullable NotificationListener listener) {
    LocalizeValue productName = Application.get().getName();

    Window recentFocusedWindow = TargetAWT.to(WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow());

    return group.newInfo()
        .title(LocalizeValue.localizeTODO("Enable smart keyboard internalization for " + productName + "."))
        .content(LocalizeValue.localizeTODO(
            "<html>We have found out that you are using a non-english keyboard layout." +
                " You can <a href='enable'>enable</a> smart layout support for " +
                KeyboardSettingsExternalizable.getDisplayLanguageNameForComponent(recentFocusedWindow) + " language." +
                " You can change this option in the settings of " + productName + " <a href='settings'>more...</a></html>"
        ))
        .optionalHyperlinkListener(listener)
        .create();
  }

  private static class MyNotificationListener implements NotificationListener {

    public MyNotificationListener() {
    }

    @Override
    public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        final String description = event.getDescription();
        if ("enable".equals(description)) {
          KeyboardSettingsExternalizable.getInstance().setNonEnglishKeyboardSupportEnabled(true);
        }
        else if ("settings".equals(description)) {
          final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
          IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
          //util.editConfigurable((JFrame)ideFrame, new StatisticsConfigurable(true));
          util.showSettingsDialog(ideFrame.getProject(), KeymapPanel.class);
        }

        NotificationsConfiguration.getNotificationsConfiguration().changeSettings(LOCALIZATION_GROUP, NotificationDisplayType.NONE, false, false);
        notification.expire();
      }
    }
  }
}
