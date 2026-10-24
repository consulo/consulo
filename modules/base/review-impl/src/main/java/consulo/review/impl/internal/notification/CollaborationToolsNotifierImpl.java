// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.notification;

import consulo.annotation.component.ServiceImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.review.notification.CollaborationToolsNotificationGroups;
import consulo.review.notification.CollaborationToolsNotifier;
import consulo.ui.ex.action.AnAction;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class CollaborationToolsNotifierImpl implements CollaborationToolsNotifier {
    private final Project myProject;
    private final NotificationService myNotificationService;

    @Inject
    public CollaborationToolsNotifierImpl(Project project, NotificationService notificationService) {
        myProject = project;
        myNotificationService = notificationService;
    }

    @Override
    public Notification notifyBalloon(LocalizeValue title, LocalizeValue message, AnAction... actions) {
        Notification.Builder builder = myNotificationService.newInfo(CollaborationToolsNotificationGroups.VCS_HOSTING_INTEGRATIONS)
            .title(title)
            .content(message);
        for (AnAction action : actions) {
            builder.addAction(action);
        }
        Notification notification = builder.create();
        myNotificationService.notify(notification, myProject);
        return notification;
    }
}
