// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.notification;

import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.security.Provider;

@Service(Provider.Service.Level.PROJECT)
public final class CollaborationToolsNotifier {
    private final Project myProject;
    private final NotificationGroup NOTIFICATION_GROUP_ID;

    public CollaborationToolsNotifier(@Nonnull Project project) {
        myProject = project;
        NOTIFICATION_GROUP_ID = NotificationGroupManager.getInstance().getNotificationGroup("VCS Hosting Integrations");
    }

    public @Nonnull Notification notifyBalloon(
        @Nullable @NonNls String displayId,
        @NlsContexts.NotificationTitle @Nonnull String title,
        @NlsContexts.NotificationContent @Nonnull String message,
        @Nonnull NotificationAction @Nonnull ... actions
    ) {
        Notification notification = createNotification(NOTIFICATION_GROUP_ID, displayId, title, message, NotificationType.INFORMATION);
        for (NotificationAction action : actions) {
            notification.addAction(action);
        }
        notification.notify(myProject);
        return notification;
    }

    private @Nonnull Notification createNotification(
        @Nonnull NotificationGroup notificationGroup,
        @Nullable @NonNls String displayId,
        @NlsContexts.NotificationTitle @Nonnull String title,
        @NlsContexts.NotificationContent @Nonnull String message,
        @Nonnull NotificationType type
    ) {
        Notification notification = notificationGroup.createNotification(title, message, type);
        if (displayId != null && !displayId.isEmpty()) {
            notification.setDisplayId(displayId);
        }
        return notification;
    }

    public static @Nonnull CollaborationToolsNotifier getInstance(@Nonnull Project project) {
        return project.getService(CollaborationToolsNotifier.class);
    }
}
