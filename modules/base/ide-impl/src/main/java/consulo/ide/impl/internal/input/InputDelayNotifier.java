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
package consulo.ide.impl.internal.input;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.internal.InputDelayListener;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells that typed keys wait for a background write action, until they ran.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@TopicImpl(ComponentScope.APPLICATION)
public class InputDelayNotifier implements InputDelayListener {
    public static final NotificationGroup GROUP =
        NotificationGroup.balloonGroup("input.delayed", IdeLocalize.inputDelayedNotificationGroup());

    private final Provider<NotificationService> myNotificationService;
    private final Provider<ProjectManager> myProjectManager;
    private final Map<UIAccess, Notification> myNotifications = new ConcurrentHashMap<>();

    @Inject
    public InputDelayNotifier(Provider<NotificationService> notificationService, Provider<ProjectManager> projectManager) {
        myNotificationService = notificationService;
        myProjectManager = projectManager;
    }

    @RequiredUIAccess
    @Override
    public void inputDelayed(UIAccess uiAccess) {
        Notification notification = myNotificationService.get()
            .newWarn(GROUP)
            .content(IdeLocalize.inputDelayedNotificationContent())
            .removeWhenExpired()
            .notifyAndGet(findProject(uiAccess));
        Notification previous = myNotifications.put(uiAccess, notification);
        if (previous != null) {
            previous.expire();
        }
    }

    @RequiredUIAccess
    @Override
    public void inputResumed(UIAccess uiAccess) {
        Notification notification = myNotifications.remove(uiAccess);
        if (notification != null) {
            notification.expire();
        }
    }

    private @Nullable Project findProject(UIAccess uiAccess) {
        for (Project project : myProjectManager.get().getOpenProjects()) {
            if (project.getUIAccess() == uiAccess) {
                return project;
            }
        }
        return null;
    }
}
