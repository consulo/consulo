/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ui.internal.NotificationIconBuilder;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ToolWindow;

import java.util.List;

/**
 * @author VISTALL
 * @since 16-Jun-24
 */
@TopicImpl(ComponentScope.APPLICATION)
public class NotificationLogModelListener implements LogModelListener {
    @Override
    public void modelChanged(Project project) {
        if (project == null) {
            return;
        }

        project.getUIAccess().giveIfNeed(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(EventLog.NOTIFICATIONS_TOOLWINDOW_ID);
            if (toolWindow != null) {
                List<Notification> notifications = EventLog.getLogModel(project).getNotifications();

                toolWindow.setIcon(NotificationIconBuilder.getIcon(notifications.stream().map(Notification::getType).toList()));
            }
        });
    }
}
