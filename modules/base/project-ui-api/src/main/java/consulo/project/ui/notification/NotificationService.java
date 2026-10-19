/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Notifies and keeps every notification until it expires - the store behind the toasts and the Notifications tool
 * window. Everything here can be called from any thread and never needs UI access; whoever shows notifications
 * listens to {@link consulo.project.ui.notification.event.NotificationServiceListener} and switches to its own UI.
 * <p/>
 * Notifications without a project belong to the application: every welcome screen shows them and every project
 * lists them next to its own.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface NotificationService {
    default Notification.Builder newError(NotificationGroup group) {
        return newOfType(group, NotificationType.ERROR);
    }

    default Notification.Builder newWarn(NotificationGroup group) {
        return newOfType(group, NotificationType.WARNING);
    }

    default Notification.Builder newInfo(NotificationGroup group) {
        return newOfType(group, NotificationType.INFORMATION);
    }

    default Notification.Builder newSuccess(NotificationGroup group) {
        return newOfType(group, NotificationType.SUCCESS);
    }

    default Notification.Builder newOfType(NotificationGroup group, NotificationType type) {
        return new Notification.Builder(this, group, type);
    }

    default void notify(Notification notification) {
        notify(notification, null);
    }

    void notify(Notification notification, @Nullable Project project);

    /**
     * The notifications of the project together with those of the application, newest last. {@code null} - those of
     * the application only.
     */
    List<Notification> getNotifications(@Nullable Project project);

    List<Notification> getUnreadNotifications(@Nullable Project project);

    <T extends Notification> List<T> getNotificationsOfType(Class<T> type, @Nullable Project project);

    void markAllRead(@Nullable Project project);

    /**
     * Expires and forgets every notification of the project and of the application.
     */
    void clearAll(@Nullable Project project);

    /**
     * Expires and forgets the notifications which are not suggestions.
     */
    void clearTimeline(@Nullable Project project);

    void remove(Notification notification);

    /**
     * Called by {@link Notification#expire()}.
     */
    void expire(Notification notification);

    /**
     * Runs an action of a notification; see {@link Notification#fire(Notification, AnAction, DataContext)}.
     */
    @RequiredUIAccess
    void fire(Notification notification, AnAction action, @Nullable DataContext context);

    @Deprecated
    @DeprecationInfo("Use injection instead")
    static NotificationService getInstance() {
        return Application.get().getInstance(NotificationService.class);
    }
}
