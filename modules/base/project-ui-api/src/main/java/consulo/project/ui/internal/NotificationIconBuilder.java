/*
 * Copyright 2013-2024 consulo.io
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
package consulo.project.ui.internal;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 15-Jun-24
 */
public class NotificationIconBuilder {
  @Nonnull
  public static Image getIcon(List<NotificationType> notifications) {
    return createIconWithNotificationCount(getMaximumType(notifications), notifications.size());
  }

  @Nonnull
  private static Image createIconWithNotificationCount(NotificationType type, int size) {
    if (size == 0) {
      return PlatformIconGroup.toolwindowsNotifications();
    }

    if (type == NotificationType.ERROR) {
      return PlatformIconGroup.toolwindowsNotificationsnewimportant();
    }

    return PlatformIconGroup.toolwindowsNotificationsnew();
  }

  @Nullable
  private static NotificationType getMaximumType(List<NotificationType> notifications) {
    NotificationType result = null;
    for (NotificationType notificationType : notifications) {
      if (NotificationType.ERROR == notificationType) {
        return NotificationType.ERROR;
      }

      if (NotificationType.WARNING == notificationType) {
        result = NotificationType.WARNING;
      }
      else if (result == null && NotificationType.INFORMATION == notificationType) {
        result = NotificationType.INFORMATION;
      }
    }

    return result;
  }
}
