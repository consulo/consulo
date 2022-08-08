/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.Notifications;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08-Aug-22
 */
@TopicImpl(ComponentScope.PROJECT)
public class NotificationProjectTrackerTopicListener implements Notifications {
  private final Provider<NotificationProjectTracker> myNotificationProjectTracker;

  @Inject
  public NotificationProjectTrackerTopicListener(Provider<NotificationProjectTracker> notificationProjectTracker) {
    myNotificationProjectTracker = notificationProjectTracker;
  }

  @Override
  public void notify(@Nonnull Notification notification) {
    myNotificationProjectTracker.get().printNotification(notification);
  }
}
