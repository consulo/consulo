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
package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.pom.Navigatable;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 3/28/14
 */
public class NotificationData {

  @Nonnull
  private String myTitle;
  @Nonnull
  private String myMessage;
  @Nonnull
  private NotificationCategory myNotificationCategory;
  @Nonnull
  private final NotificationSource myNotificationSource;
  @Nonnull
  private NotificationListener myListener;
  @Nullable private String myFilePath;
  @Nullable private Navigatable navigatable;
  private int myLine;
  private int myColumn;
  private boolean myBalloonNotification;

  private final Map<String, NotificationListener> myListenerMap;

  public NotificationData(@Nonnull String title,
                          @Nonnull String message,
                          @Nonnull NotificationCategory notificationCategory,
                          @Nonnull NotificationSource notificationSource) {
    this(title, message, notificationCategory, notificationSource, null, -1, -1, false);
  }

  public NotificationData(@Nonnull String title,
                          @Nonnull String message,
                          @Nonnull NotificationCategory notificationCategory,
                          @Nonnull NotificationSource notificationSource,
                          @Nullable String filePath,
                          int line,
                          int column,
                          boolean balloonNotification) {
    myTitle = title;
    myMessage = message;
    myNotificationCategory = notificationCategory;
    myNotificationSource = notificationSource;
    myListenerMap = ContainerUtil.newHashMap();
    myListener = new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;

        final NotificationListener notificationListener = myListenerMap.get(event.getDescription());
        if (notificationListener != null) {
          notificationListener.hyperlinkUpdate(notification, event);
        }
      }
    };
    myFilePath = filePath;
    myLine = line;
    myColumn = column;
    myBalloonNotification = balloonNotification;
  }

  @Nonnull
  public String getTitle() {
    return myTitle;
  }

  public void setTitle(@Nonnull String title) {
    myTitle = title;
  }

  @Nonnull
  public String getMessage() {
    return myMessage;
  }

  public void setMessage(@Nonnull String message) {
    myMessage = message;
  }

  @Nonnull
  public NotificationCategory getNotificationCategory() {
    return myNotificationCategory;
  }

  public void setNotificationCategory(@Nonnull NotificationCategory notificationCategory) {
    myNotificationCategory = notificationCategory;
  }

  @Nonnull
  public NotificationSource getNotificationSource() {
    return myNotificationSource;
  }

  @Nonnull
  public NotificationListener getListener() {
    return myListener;
  }

  @Nullable
  public String getFilePath() {
    return myFilePath;
  }

  public void setFilePath(@Nullable String filePath) {
    myFilePath = filePath;
  }

  @Nonnull
  public Integer getLine() {
    return myLine;
  }

  public void setLine(int line) {
    myLine = line;
  }

  public int getColumn() {
    return myColumn;
  }

  public void setColumn(int column) {
    myColumn = column;
  }

  public boolean isBalloonNotification() {
    return myBalloonNotification;
  }

  public void setBalloonNotification(boolean balloonNotification) {
    myBalloonNotification = balloonNotification;
  }

  public void setListener(@Nonnull String listenerId, @Nonnull NotificationListener listener) {
    myListenerMap.put(listenerId, listener);
  }

  boolean hasLinks() {
    return !myListenerMap.isEmpty();
  }

  public List<String> getRegisteredListenerIds() {
    return ContainerUtil.newArrayList(myListenerMap.keySet());
  }

  @Nullable
  public Navigatable getNavigatable() {
    return navigatable;
  }

  public void setNavigatable(@Nullable Navigatable navigatable) {
    this.navigatable = navigatable;
  }
}
