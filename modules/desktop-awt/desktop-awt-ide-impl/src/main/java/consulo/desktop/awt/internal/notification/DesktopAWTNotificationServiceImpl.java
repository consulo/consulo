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
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.notification.impl.NotificationsManagerImpl;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationServiceListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 21/08/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTNotificationServiceImpl implements NotificationService, Disposable {
  private final NotificationServiceListener myNotificationServiceListener;

  @Inject
  public DesktopAWTNotificationServiceImpl(Application application) {
    myNotificationServiceListener = application.getMessageBus().syncPublisher(NotificationServiceListener.class);
  }

  @Override
  public void notify(@Nonnull Notification notification, @Nullable Project project) {
    NotificationsManagerImpl.doNotify(notification, project);

    myNotificationServiceListener.notify(notification, project);
  }

  @Override
  public void dispose() {
    
  }
}
