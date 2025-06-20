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
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.ui.UIAccess;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;

/**
 * @author VISTALL
 * @since 29-Aug-22
 */
@ExtensionImpl
public class NotificationEventLogMover implements BackgroundStartupActivity {
    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        EventLog applicationComponent = EventLog.getApplicationComponent();

        ArrayList<Notification> notifications = applicationComponent.myModel.getNotifications();
        if (!notifications.isEmpty()) {
            NotificationProjectTracker.getInstance(project).printToProjectEventLog();
        }
    }
}
