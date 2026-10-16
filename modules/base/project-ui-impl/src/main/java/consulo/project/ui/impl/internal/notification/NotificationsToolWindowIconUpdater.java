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
package consulo.project.ui.impl.internal.notification;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationServiceListener;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Marks the stripe button of the Action Center while it holds unread notifications. A notification of the application
 * is unread in every project.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
@TopicImpl(ComponentScope.APPLICATION)
public class NotificationsToolWindowIconUpdater implements NotificationServiceListener {
    private final Provider<NotificationService> myNotificationService;
    private final Provider<ProjectManager> myProjectManager;

    @Inject
    public NotificationsToolWindowIconUpdater(Provider<NotificationService> notificationService, Provider<ProjectManager> projectManager) {
        myNotificationService = notificationService;
        myProjectManager = projectManager;
    }

    @Override
    public void stateChanged(@Nullable Project project) {
        List<Project> projects = project != null ? List.of(project) : List.of(myProjectManager.get().getOpenProjects());
        for (Project each : projects) {
            if (each.isDisposed()) {
                continue;
            }
            each.getUIAccess().give(() -> {
                if (each.isDisposed()) {
                    return;
                }
                ToolWindow toolWindow = ToolWindowManager.getInstance(each).getToolWindow(NotificationsToolWindowFactory.ID);
                if (toolWindow != null) {
                    toolWindow.setIcon(NotificationsToolWindowFactory.iconOf(myNotificationService.get(), each));
                }
            });
        }
    }
}
