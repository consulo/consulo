/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationServiceListener;
import org.jspecify.annotations.Nullable;

/**
 * Integration tests have nothing to show a notification in, so it goes to the log.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@TopicImpl(ComponentScope.APPLICATION)
public class HeadlessNotificationLogger implements NotificationServiceListener {
    private static final Logger LOG = Logger.getInstance(HeadlessNotificationLogger.class);

    @Override
    public void notificationAdded(Notification notification, @Nullable Project project) {
        LOG.info("notification [" + notification.getType() + "] " + notification.getTitle() + ": " + notification.getContent());
    }
}
