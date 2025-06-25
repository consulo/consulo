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
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2023-08-21
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface NotificationService {
    default Notification.Builder newError(@Nonnull NotificationGroup group) {
        return newOfType(group, NotificationType.ERROR);
    }

    default Notification.Builder newWarn(@Nonnull NotificationGroup group) {
        return newOfType(group, NotificationType.WARNING);
    }

    default Notification.Builder newInfo(@Nonnull NotificationGroup group) {
        return newOfType(group, NotificationType.INFORMATION);
    }

    default Notification.Builder newOfType(@Nonnull NotificationGroup group, @Nonnull NotificationType type) {
        return new Notification.Builder(this, group, type);
    }

    default void notify(@Nonnull Notification notification) {
        notify(notification, null);
    }

    void notify(@Nonnull Notification notification, @Nullable Project project);

    @Deprecated
    @DeprecationInfo("Use injection instead")
    static NotificationService getInstance() {
        return Application.get().getInstance(NotificationService.class);
    }
}
