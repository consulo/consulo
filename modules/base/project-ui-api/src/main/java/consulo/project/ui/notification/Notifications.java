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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @author spleaner
 */
public interface Notifications {
    NotificationGroup SYSTEM_MESSAGES_GROUP = NotificationGroup.balloonGroup("System Messages");

    class Bus {
        public static void notify(@Nonnull Notification notification) {
            notify(notification, null);
        }

        public static void notify(@Nonnull Notification notification, @Nullable Project project) {
            NotificationService.getInstance().notify(notification, project);
        }

        public static void notifyAndHide(@Nonnull Notification notification) {
            notifyAndHide(notification, null);
        }

        public static void notifyAndHide(@Nonnull Notification notification, @Nullable Project project) {
            notify(notification);
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                () -> {
                    if (project == null || !project.isDisposed()) {
                        notification.expire();
                    }
                },
                5,
                TimeUnit.SECONDS
            );
        }
    }
}
