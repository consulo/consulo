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
package consulo.project.ui.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * @author spleaner
 */
// FIXME [VISTALL] this topic is App&Project level
@TopicAPI(value = ComponentScope.APPLICATION, direction = TopicBroadcastDirection.NONE)
public interface Notifications {
  NotificationGroup SYSTEM_MESSAGES_GROUP = NotificationGroup.balloonGroup("System Messages");

  void notify(@Nonnull Notification notification);

  class Bus {
    public static void notify(@Nonnull final Notification notification) {
      notify(notification, null);
    }

    public static void notify(@Nonnull final Notification notification, @Nullable final Project project) {
      Application.get().getLastUIAccess().giveIfNeed(() -> doNotify(notification, project));
    }

    private static void doNotify(Notification notification, @Nullable Project project) {
      if (project != null && !project.isDisposed()) {
        project.getMessageBus().syncPublisher(Notifications.class).notify(notification);
      }
      else {
        Application app = Application.get();
        if (!app.isDisposed()) {
          app.getMessageBus().syncPublisher(Notifications.class).notify(notification);
        }
      }
    }

    public static void notifyAndHide(@Nonnull final Notification notification) {
      notifyAndHide(notification, null);
    }

    public static void notifyAndHide(@Nonnull final Notification notification, @Nullable Project project) {
      notify(notification);
      AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
        if (project == null || !project.isDisposed()) {
          notification.expire();
        }
      }, 5, TimeUnit.SECONDS);
    }
  }
}
