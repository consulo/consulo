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
package com.intellij.ui;

import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.AtomicNullableLazyValue;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.SystemProperties;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author mike
 */
@Singleton
public class SystemNotificationsImpl extends SystemNotifications {
  interface Notifier {
    void notify(@Nonnull String name, @Nonnull String title, @Nonnull String description);
  }

  private final NullableLazyValue<Notifier> myNotifier = AtomicNullableLazyValue.createValue(SystemNotificationsImpl::getPlatformNotifier);

  private final Application myApplication;

  @Inject
  public SystemNotificationsImpl(Application application) {
    myApplication = application;
  }

  @Override
  public boolean isAvailable() {
    return myNotifier.getValue() != null;
  }

  @Override
  public void notify(@Nonnull String notificationName, @Nonnull String title, @Nonnull String text) {
    if (NotificationsConfigurationImpl.getInstanceImpl().SYSTEM_NOTIFICATIONS && !myApplication.isActive()) {
      Notifier notifier = myNotifier.getValue();
      if (notifier != null) {
        notifier.notify(notificationName, title, text);
      }
    }
  }

  private static Notifier getPlatformNotifier() {
    try {
      if (SystemInfo.isMac) {
        if (SystemInfo.isMacOSMountainLion && SystemProperties.getBooleanProperty("ide.mac.mountain.lion.notifications.enabled", true)) {
          return MountainLionNotifications.getInstance();
        }
        else {
          return GrowlNotifications.getInstance();
        }
      }
      else if (SystemInfo.isXWindow) {
        return LibNotifyWrapper.getInstance();
      }
      else if (SystemInfo.isWin10OrNewer) {
        return JTrayNotificationImpl.getWin10Instance();
      }
    }
    catch (Throwable t) {
      Logger logger = Logger.getInstance(SystemNotifications.class);
      if (logger.isDebugEnabled()) {
        logger.debug(t);
      }
      else {
        logger.info(t.getMessage(), t);
      }
    }

    return null;
  }
}
