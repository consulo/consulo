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
package consulo.externalSystem.service.notification;

import consulo.navigation.Navigatable;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;

import org.jspecify.annotations.Nullable;
import javax.swing.event.HyperlinkEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 3/28/14
 */
public class NotificationData {

  
  private String myTitle;
  
  private String myMessage;
  
  private NotificationCategory myNotificationCategory;
  
  private final NotificationSource myNotificationSource;
  
  private NotificationListener myListener;
  private @Nullable String myFilePath;
  private @Nullable Navigatable navigatable;
  private int myLine;
  private int myColumn;
  private boolean myBalloonNotification;

  private final Map<String, NotificationListener> myListenerMap;

  public NotificationData(String title,
                          String message,
                          NotificationCategory notificationCategory,
                          NotificationSource notificationSource) {
    this(title, message, notificationCategory, notificationSource, null, -1, -1, false);
  }

  public NotificationData(String title,
                          String message,
                          NotificationCategory notificationCategory,
                          NotificationSource notificationSource,
                          @Nullable String filePath,
                          int line,
                          int column,
                          boolean balloonNotification) {
    myTitle = title;
    myMessage = message;
    myNotificationCategory = notificationCategory;
    myNotificationSource = notificationSource;
    myListenerMap = new HashMap<>();
    myListener = new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(Notification notification, HyperlinkEvent event) {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;

        NotificationListener notificationListener = myListenerMap.get(event.getDescription());
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

  
  public String getTitle() {
    return myTitle;
  }

  public void setTitle(String title) {
    myTitle = title;
  }

  
  public String getMessage() {
    return myMessage;
  }

  public void setMessage(String message) {
    myMessage = message;
  }

  
  public NotificationCategory getNotificationCategory() {
    return myNotificationCategory;
  }

  public void setNotificationCategory(NotificationCategory notificationCategory) {
    myNotificationCategory = notificationCategory;
  }

  
  public NotificationSource getNotificationSource() {
    return myNotificationSource;
  }

  
  public NotificationListener getListener() {
    return myListener;
  }

  public @Nullable String getFilePath() {
    return myFilePath;
  }

  public void setFilePath(@Nullable String filePath) {
    myFilePath = filePath;
  }

  
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

  public void setListener(String listenerId, NotificationListener listener) {
    myListenerMap.put(listenerId, listener);
  }

  public boolean hasLinks() {
    return !myListenerMap.isEmpty();
  }

  public List<String> getRegisteredListenerIds() {
    return new ArrayList<>(myListenerMap.keySet());
  }

  public @Nullable Navigatable getNavigatable() {
    return navigatable;
  }

  public void setNavigatable(@Nullable Navigatable navigatable) {
    this.navigatable = navigatable;
  }
}
