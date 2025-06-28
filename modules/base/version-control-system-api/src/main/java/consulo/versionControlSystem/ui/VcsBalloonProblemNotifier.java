/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.ui;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.util.NamedRunnable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.versionControlSystem.VcsToolWindow;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * Shows a notification balloon over one of version control related tool windows: Changes View or Version Control View.
 * By default the notification is shown over the Changes View.
 * Use the special method or supply additional parameter to the constructor to show the balloon over the Version Control View.
 */
public class VcsBalloonProblemNotifier implements Runnable {
    public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup(
        "commonVersionControlMessages",
        LocalizeValue.localizeTODO("Common Version Control Messages"),
        VcsToolWindow.ID,
        true
    );
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final String myMessage;
    private final NotificationType myMessageType;
    private final boolean myShowOverChangesView;
    @Nullable
    private final NamedRunnable[] myNotificationListener;

    public VcsBalloonProblemNotifier(@Nonnull Project project, @Nonnull String message, NotificationType messageType) {
        this(project, message, messageType, true, null);
    }

    public VcsBalloonProblemNotifier(
        @Nonnull Project project,
        @Nonnull String message,
        NotificationType messageType,
        boolean showOverChangesView,
        @Nullable NamedRunnable[] notificationListener
    ) {
        myProject = project;
        myMessage = message;
        myMessageType = messageType;
        myShowOverChangesView = showOverChangesView;
        myNotificationListener = notificationListener;
    }

    public static void showOverChangesView(
        @Nonnull Project project,
        @Nonnull String message,
        NotificationType type,
        NamedRunnable... notificationListener
    ) {
        show(project, message, type, true, notificationListener);
    }

    public static void showOverVersionControlView(
        @Nonnull Project project,
        @Nonnull String message,
        NotificationType type
    ) {
        show(project, message, type, false, null);
    }

    private static void show(
        final Project project,
        final String message,
        final NotificationType type,
        final boolean showOverChangesView,
        @Nullable final NamedRunnable[] notificationListener
    ) {
        Application application = Application.get();
        if (application.isHeadlessEnvironment()) {
            return;
        }
        Runnable showErrorAction =
            () -> new VcsBalloonProblemNotifier(project, message, type, showOverChangesView, notificationListener).run();
        if (application.isDispatchThread()) {
            showErrorAction.run();
        }
        else {
            application.invokeLater(showErrorAction);
        }
    }

    @Override
    public void run() {
        NotificationService notificationService = NotificationService.getInstance();
        Notification notification;
        if (myNotificationListener != null && myNotificationListener.length > 0) {
            StringBuilder sb = new StringBuilder(myMessage);
            for (NamedRunnable runnable : myNotificationListener) {
                String name = runnable.toString();
                sb.append("<br/><a href=\"").append(name).append("\">").append(name).append("</a>");
            }

            notification = notificationService.newOfType(NOTIFICATION_GROUP, myMessageType)
                .title(LocalizeValue.localizeTODO(myMessageType.name()))
                .content(LocalizeValue.localizeTODO(sb.toString()))
                .hyperlinkListener((thisNotification, event) -> {
                    if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) {
                        if (myNotificationListener.length == 1) {
                            myNotificationListener[0].run();
                        }
                        else {
                            String description = event.getDescription();
                            if (description != null) {
                                for (NamedRunnable runnable : myNotificationListener) {
                                    if (description.equals(runnable.toString())) {
                                        runnable.run();
                                        break;
                                    }
                                }
                            }
                        }
                        thisNotification.expire();
                    }
                })
                .create();
        }
        else {
            notification = notificationService.newOfType(NOTIFICATION_GROUP, myMessageType)
                .content(LocalizeValue.localizeTODO(myMessage))
                .create();
        }
        notification.notify(myProject.isDefault() ? null : myProject);
    }
}
