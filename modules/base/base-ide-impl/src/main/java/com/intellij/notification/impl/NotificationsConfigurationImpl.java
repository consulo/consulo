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

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.MessageBus;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author spleaner
 */
@State(name = "NotificationConfiguration", storages = @Storage("notifications.xml"))
@Singleton
public class NotificationsConfigurationImpl extends NotificationsConfiguration implements Disposable, PersistentStateComponent<Element> {

  private static final Logger LOG = Logger.getInstance(NotificationsConfiguration.class);
  private static final String SHOW_BALLOONS_ATTRIBUTE = "showBalloons";
  private static final String SYSTEM_NOTIFICATIONS_ATTRIBUTE = "systemNotifications";

  private static final Comparator<NotificationSettings> NOTIFICATION_SETTINGS_COMPARATOR = new Comparator<NotificationSettings>() {
    @Override
    public int compare(@Nonnull NotificationSettings o1, @Nonnull NotificationSettings o2) {
      return o1.getGroupId().compareToIgnoreCase(o2.getGroupId());
    }
  };

  private final Map<String, NotificationSettings> myIdToSettingsMap = new HashMap<String, NotificationSettings>();
  private final Map<String, String> myToolWindowCapable = new HashMap<String, String>();
  private final MessageBus myMessageBus;

  public boolean SHOW_BALLOONS = true;
  public boolean SYSTEM_NOTIFICATIONS = true;

  @Inject
  public NotificationsConfigurationImpl(@Nonnull Application application) {
    myMessageBus = application.getMessageBus();
  }

  public static NotificationsConfigurationImpl getInstanceImpl() {
    return (NotificationsConfigurationImpl)getNotificationsConfiguration();
  }

  public synchronized boolean hasToolWindowCapability(@Nonnull String groupId) {
    return getToolWindowId(groupId) != null || myToolWindowCapable.containsKey(groupId);
  }

  @Nullable
  public String getToolWindowId(@Nonnull String groupId) {
    NotificationGroup group = NotificationGroup.findRegisteredGroup(groupId);
    return group == null ? null : group.getToolWindowId();
  }

  public synchronized NotificationSettings[] getAllSettings() {
    Collection<NotificationSettings> settings = new HashSet<NotificationSettings>(myIdToSettingsMap.values());
    for (NotificationGroup group : NotificationGroup.getAllRegisteredGroups()) {
      settings.add(getSettings(group.getDisplayId()));
    }
    NotificationSettings[] result = settings.toArray(new NotificationSettings[settings.size()]);
    Arrays.sort(result, NOTIFICATION_SETTINGS_COMPARATOR);
    return result;
  }

  public static void remove(String... toRemove) {
    getInstanceImpl().doRemove(toRemove);
  }

  private synchronized void doRemove(String... toRemove) {
    for (String groupId : toRemove) {
      myIdToSettingsMap.remove(groupId);
      myToolWindowCapable.remove(groupId);
    }
  }

  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  @Nonnull
  public static NotificationSettings getSettings(@Nonnull String groupId) {
    NotificationSettings settings;
    NotificationsConfigurationImpl impl = getInstanceImpl();
    synchronized (impl) {
      settings = impl.myIdToSettingsMap.get(groupId);
    }
    return settings == null ? getDefaultSettings(groupId) : settings;
  }

  @Nonnull
  private static NotificationSettings getDefaultSettings(String groupId) {
    NotificationGroup group = NotificationGroup.findRegisteredGroup(groupId);
    if (group != null) {
      return new NotificationSettings(groupId, group.getDisplayType(), group.isLogByDefault(), false);
    }
    return new NotificationSettings(groupId, NotificationDisplayType.BALLOON, true, false);
  }

  @Override
  public void afterLoadState() {
    myMessageBus.connect().subscribe(TOPIC, this);
  }

  @Override
  public synchronized void dispose() {
    myIdToSettingsMap.clear();
  }

  @Override
  public void register(@Nonnull final String groupDisplayName, @Nonnull final NotificationDisplayType displayType) {
    register(groupDisplayName, displayType, true);
  }

  @Override
  public void register(@Nonnull String groupDisplayName, @Nonnull NotificationDisplayType displayType, boolean shouldLog) {
    register(groupDisplayName, displayType, shouldLog, false);
  }

  @Override
  public void register(@Nonnull String groupDisplayName, @Nonnull NotificationDisplayType displayType, boolean shouldLog, boolean shouldReadAloud) {
    if (!isRegistered(groupDisplayName)) {
      // register a new group and remember these settings as default
      new NotificationGroup(groupDisplayName, displayType, shouldLog);
      // and decide whether to save them explicitly (in case of non-default shouldReadAloud)
      changeSettings(groupDisplayName, displayType, shouldLog, shouldReadAloud);
    }
    else if (displayType == NotificationDisplayType.TOOL_WINDOW && !hasToolWindowCapability(groupDisplayName)) {
      // the first time with tool window capability
      changeSettings(getSettings(groupDisplayName).withDisplayType(NotificationDisplayType.TOOL_WINDOW));
      myToolWindowCapable.put(groupDisplayName, null);
    }
  }

  @Override
  public void changeSettings(String groupDisplayName, NotificationDisplayType displayType, boolean shouldLog, boolean shouldReadAloud) {
    changeSettings(new NotificationSettings(groupDisplayName, displayType, shouldLog, shouldReadAloud));
  }

  public synchronized void changeSettings(NotificationSettings settings) {
    String groupDisplayName = settings.getGroupId();
    if (settings.equals(getDefaultSettings(groupDisplayName))) {
      myIdToSettingsMap.remove(groupDisplayName);
    }
    else {
      myIdToSettingsMap.put(groupDisplayName, settings);
    }
  }

  public synchronized boolean isRegistered(@Nonnull final String id) {
    return myIdToSettingsMap.containsKey(id) || NotificationGroup.findRegisteredGroup(id) != null;
  }

  @Override
  public synchronized Element getState() {
    Element element = new Element("NotificationsConfiguration");

    NotificationSettings[] sortedNotifications = myIdToSettingsMap.values().toArray(new NotificationSettings[myIdToSettingsMap.size()]);
    Arrays.sort(sortedNotifications, NOTIFICATION_SETTINGS_COMPARATOR);
    for (NotificationSettings settings : sortedNotifications) {
      element.addContent(settings.save());
    }

    //noinspection NonPrivateFieldAccessedInSynchronizedContext
    if (!SHOW_BALLOONS) {
      element.setAttribute(SHOW_BALLOONS_ATTRIBUTE, "false");
    }

    //noinspection NonPrivateFieldAccessedInSynchronizedContext
    if (!SYSTEM_NOTIFICATIONS) {
      element.setAttribute(SYSTEM_NOTIFICATIONS_ATTRIBUTE, "false");
    }

    return element;
  }

  @Override
  public synchronized void loadState(final Element state) {
    myIdToSettingsMap.clear();
    for (Element child : state.getChildren("notification")) {
      final NotificationSettings settings = NotificationSettings.load(child);
      if (settings != null) {
        final String id = settings.getGroupId();
        LOG.assertTrue(!myIdToSettingsMap.containsKey(id), String.format("Settings for '%s' already loaded!", id));
        myIdToSettingsMap.put(id, settings);
      }
    }
    doRemove("Log Only");

    if ("false".equals(state.getAttributeValue(SHOW_BALLOONS_ATTRIBUTE))) {
      //noinspection NonPrivateFieldAccessedInSynchronizedContext
      SHOW_BALLOONS = false;
    }

    if ("false".equals(state.getAttributeValue(SYSTEM_NOTIFICATIONS_ATTRIBUTE))) {
      //noinspection NonPrivateFieldAccessedInSynchronizedContext
      SYSTEM_NOTIFICATIONS = false;
    }
  }
}
