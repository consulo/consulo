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
package consulo.ide.impl.idea.notification.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.Notifications;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06-Jul-22
 */
@TopicImpl(ComponentScope.PROJECT)
public class ProjectNotificationsListenerImpl implements Notifications {
  private final Project myProject;

  @Inject
  public ProjectNotificationsListenerImpl(Project project) {
    myProject = project;
  }

  @Override
  public void notify(@Nonnull Notification notification) {
    if (!myProject.getApplication().isSwingApplication()) {
      return;
    }

    NotificationsManagerImpl.doNotify(notification, myProject);
  }
}
