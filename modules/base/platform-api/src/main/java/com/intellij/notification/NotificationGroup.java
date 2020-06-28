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

import com.intellij.openapi.ui.MessageType;
import consulo.logging.Logger;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author peter
 */
public final class NotificationGroup {
  private static final Logger LOG = Logger.getInstance(NotificationGroup.class);
  private static final Map<String, NotificationGroup> ourRegisteredGroups = new ConcurrentHashMap<>();

  @Nonnull
  private final String myDisplayId;
  @Nonnull
  private final NotificationDisplayType myDisplayType;
  private final boolean myLogByDefault;
  @Nullable
  private final String myToolWindowId;
  private final Image myIcon;

  private String myParentId;

  public NotificationGroup(@Nonnull String displayId, @Nonnull NotificationDisplayType defaultDisplayType, boolean logByDefault) {
    this(displayId, defaultDisplayType, logByDefault, null);
  }

  public NotificationGroup(@Nonnull String displayId, @Nonnull NotificationDisplayType defaultDisplayType, boolean logByDefault, @Nullable String toolWindowId) {
    this(displayId, defaultDisplayType, logByDefault, toolWindowId, null);
  }

  public NotificationGroup(@Nonnull String displayId, @Nonnull NotificationDisplayType defaultDisplayType, boolean logByDefault, @Nullable String toolWindowId, @Nullable Image icon) {
    myDisplayId = displayId;
    myDisplayType = defaultDisplayType;
    myLogByDefault = logByDefault;
    myToolWindowId = toolWindowId;
    myIcon = icon;

    if (ourRegisteredGroups.containsKey(displayId)) {
      LOG.info("Notification group " + displayId + " is already registered", new Throwable());
    }
    ourRegisteredGroups.put(displayId, this);
  }

  @Nonnull
  public static NotificationGroup balloonGroup(@Nonnull String displayId) {
    return new NotificationGroup(displayId, NotificationDisplayType.BALLOON, true);
  }

  @Nonnull
  public static NotificationGroup logOnlyGroup(@Nonnull String displayId) {
    return new NotificationGroup(displayId, NotificationDisplayType.NONE, true);
  }

  @Nonnull
  public static NotificationGroup toolWindowGroup(@Nonnull String displayId, @Nonnull String toolWindowId, final boolean logByDefault) {
    return new NotificationGroup(displayId, NotificationDisplayType.TOOL_WINDOW, logByDefault, toolWindowId);
  }

  @Nonnull
  public static NotificationGroup toolWindowGroup(@Nonnull String displayId, @Nonnull String toolWindowId) {
    return toolWindowGroup(displayId, toolWindowId, true);
  }

  @Nonnull
  public String getDisplayId() {
    return myDisplayId;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  public Notification createNotification(@Nonnull final String content, @Nonnull final MessageType type) {
    return createNotification(content, type.toNotificationType());
  }

  @Nonnull
  public Notification createNotification(@Nonnull final String content, @Nonnull final NotificationType type) {
    return createNotification("", content, type, null);
  }

  @Nonnull
  public Notification createNotification(@Nonnull final String title, @Nonnull final String content, @Nonnull final NotificationType type, @Nullable NotificationListener listener) {
    return new Notification(myDisplayId, title, content, type, listener);
  }

  @Nonnull
  public Notification createNotification() {
    return createNotification(NotificationType.INFORMATION);
  }

  @Nonnull
  public Notification createNotification(@Nonnull NotificationType type) {
    return createNotification(null, null, null, type, null);
  }

  @Nonnull
  public Notification createNotification(@Nullable String title, @Nullable String subtitle, @Nullable String content, @Nonnull NotificationType type) {
    return createNotification(title, subtitle, content, type, null);
  }

  @Nonnull
  public Notification createNotification(@Nullable String title, @Nullable String subtitle, @Nullable String content, @Nonnull NotificationType type, @Nullable NotificationListener listener) {
    LOG.assertTrue(myIcon != null);
    return new Notification(myDisplayId, myIcon, title, subtitle, content, type, listener);
  }

  @Nullable
  public String getParentId() {
    return myParentId;
  }

  @Nonnull
  public NotificationGroup setParentId(@Nonnull String parentId) {
    myParentId = parentId;
    return this;
  }

  @Nonnull
  public NotificationDisplayType getDisplayType() {
    return myDisplayType;
  }

  public boolean isLogByDefault() {
    return myLogByDefault;
  }

  @Nullable
  public String getToolWindowId() {
    return myToolWindowId;
  }

  @Nullable
  public static NotificationGroup findRegisteredGroup(String displayId) {
    return ourRegisteredGroups.get(displayId);
  }

  @Nonnull
  public static Iterable<NotificationGroup> getAllRegisteredGroups() {
    return ourRegisteredGroups.values();
  }

}
