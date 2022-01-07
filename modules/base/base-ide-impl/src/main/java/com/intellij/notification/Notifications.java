/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.notification;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author spleaner
 */
public interface Notifications {
  Topic<Notifications> TOPIC = Topic.create("Notifications", Notifications.class, Topic.BroadcastDirection.NONE);

  String SYSTEM_MESSAGES_GROUP_ID = "System Messages";

  void notify(@Nonnull Notification notification);
  void register(@Nonnull final String groupDisplayName, @Nonnull final NotificationDisplayType defaultDisplayType);
  void register(@Nonnull final String groupDisplayName, @Nonnull final NotificationDisplayType defaultDisplayType, boolean shouldLog);
  void register(@Nonnull final String groupDisplayName, @Nonnull final NotificationDisplayType defaultDisplayType, boolean shouldLog, boolean shouldReadAloud);

  @SuppressWarnings({"UtilityClassWithoutPrivateConstructor"})
  class Bus {
    /**
     * Registration is OPTIONAL: STICKY_BALLOON display type will be used by default.
     */
    @SuppressWarnings("JavaDoc")
    public static void register(@Nonnull final String group_id, @Nonnull final NotificationDisplayType defaultDisplayType) {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        Application app = ApplicationManager.getApplication();
        if (!app.isDisposed()) {
          app.getMessageBus().syncPublisher(TOPIC).register(group_id, defaultDisplayType);
        }
      });
    }

    public static void notify(@Nonnull final Notification notification) {
      notify(notification, null);
    }

    public static void notify(@Nonnull final Notification notification, @Nullable final Project project) {
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        doNotify(notification, project);
      }
      else {
        UIUtil.invokeLaterIfNeeded(() -> doNotify(notification, project));
      }
    }

    private static void doNotify(Notification notification, @Nullable Project project) {
      if (project != null && !project.isDisposed()) {
        project.getMessageBus().syncPublisher(TOPIC).notify(notification);
      } else {
        Application app = Application.get();
        if (!app.isDisposed()) {
          app.getMessageBus().syncPublisher(TOPIC).notify(notification);
        }
      }
    }

    public static void notifyAndHide(@Nonnull final Notification notification) {
      notifyAndHide(notification, null);
    }

    public static void notifyAndHide(@Nonnull final Notification notification, @Nullable Project project) {
      notify(notification);
      Alarm alarm = new Alarm(project == null ? Application.get() : project);
      alarm.addRequest(() -> {
        notification.expire();
        Disposer.dispose(alarm);
      }, 5000);
    }
  }
}
