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
package consulo.project.ui.notification.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import org.jspecify.annotations.Nullable;

import java.util.EventListener;

/**
 * Published by {@link consulo.project.ui.notification.NotificationService} on the thread which caused the change -
 * a listener which touches UI switches to its own {@link consulo.ui.UIAccess}. A {@code null} project means the
 * notification belongs to the application.
 *
 * @author VISTALL
 * @since 21/08/2023
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface NotificationServiceListener extends EventListener {
    default void notificationAdded(Notification notification, @Nullable Project project) {
    }

    /**
     * The notification expired - its toast goes away; it stays listed, greyed out, unless it is removed too.
     */
    default void notificationExpired(Notification notification, @Nullable Project project) {
    }

    default void notificationRemoved(Notification notification, @Nullable Project project) {
    }

    /**
     * The unread notifications of the project, or of the application, changed.
     */
    default void stateChanged(@Nullable Project project) {
    }
}
