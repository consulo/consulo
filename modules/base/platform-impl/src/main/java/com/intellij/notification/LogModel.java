/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author peter
 */
public class LogModel implements Disposable {
  public static final Topic<Runnable> LOG_MODEL_CHANGED = Topic.create("LOG_MODEL_CHANGED", Runnable.class, Topic.BroadcastDirection.NONE);

  private final List<Notification> myNotifications = new ArrayList<Notification>();
  private final Map<Notification, Long> myStamps = Collections.synchronizedMap(new WeakHashMap<Notification, Long>());
  private Trinity<Notification, String, Long> myStatusMessage;
  private final Project myProject;
  final Map<Notification, Runnable> removeHandlers = new HashMap<Notification, Runnable>();

  LogModel(@Nullable Project project, @Nonnull Disposable parentDisposable) {
    myProject = project;
    Disposer.register(parentDisposable, this);
  }

  void addNotification(Notification notification) {
    long stamp = System.currentTimeMillis();
    NotificationDisplayType type = NotificationsConfigurationImpl.getSettings(notification.getGroupId()).getDisplayType();
    if (notification.isImportant() || (type != NotificationDisplayType.NONE && type != NotificationDisplayType.TOOL_WINDOW)) {
      synchronized (myNotifications) {
        myNotifications.add(notification);
      }
    }
    myStamps.put(notification, stamp);
    setStatusMessage(notification, stamp);
    fireModelChanged();
  }

  private static void fireModelChanged() {
    ApplicationManager.getApplication().getMessageBus().syncPublisher(LOG_MODEL_CHANGED).run();
  }

  List<Notification> takeNotifications() {
    final ArrayList<Notification> result;
    synchronized (myNotifications) {
      result = getNotifications();
      myNotifications.clear();
    }
    fireModelChanged();
    return result;
  }

  void setStatusMessage(@Nullable Notification statusMessage, long stamp) {
    synchronized (myNotifications) {
      if (myStatusMessage != null && myStatusMessage.first == statusMessage) return;
      if (myStatusMessage == null && statusMessage == null) return;

      myStatusMessage = statusMessage == null ? null : Trinity.create(statusMessage, EventLog.formatForLog(statusMessage, "").status, stamp);
    }
    StatusBar.Info.set("", myProject, EventLog.LOG_REQUESTOR);
  }

  @Nullable
  Trinity<Notification, String, Long> getStatusMessage() {
    synchronized (myNotifications) {
      return myStatusMessage;
    }
  }

  void logShown() {
    for (Notification notification : getNotifications()) {
      if (!notification.isImportant()) {
        removeNotification(notification);
      }
    }
    setStatusToImportant();
  }

  public ArrayList<Notification> getNotifications() {
    synchronized (myNotifications) {
      return new ArrayList<Notification>(myNotifications);
    }
  }

  @Nullable
  public Long getNotificationTime(Notification notification) {
    return myStamps.get(notification);
  }

  void removeNotification(Notification notification) {
    synchronized (myNotifications) {
      myNotifications.remove(notification);
    }

    Runnable handler = removeHandlers.remove(notification);
    if (handler != null) {
      UIUtil.invokeLaterIfNeeded(handler);
    }

    Trinity<Notification, String, Long> oldStatus = getStatusMessage();
    if (oldStatus != null && notification == oldStatus.first) {
      setStatusToImportant();
    }
    fireModelChanged();
  }

  private void setStatusToImportant() {
    ArrayList<Notification> notifications = getNotifications();
    Collections.reverse(notifications);
    Notification message = ContainerUtil.find(notifications, new Condition<Notification>() {
      @Override
      public boolean value(Notification notification) {
        return notification.isImportant();
      }
    });
    if (message == null) {
      setStatusMessage(message, 0);
    } else {
      Long notificationTime = getNotificationTime(message);
      assert notificationTime != null;
      setStatusMessage(message, notificationTime);
    }
  }

  public Project getProject() {
    //noinspection ConstantConditions
    return myProject;
  }

  @Override
  public void dispose() {
  }
}
