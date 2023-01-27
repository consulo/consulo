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
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.versionControlSystem.VcsToolWindow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;

/**
 * Shows a notification balloon over one of version control related tool windows: Changes View or Version Control View.
 * By default the notification is shown over the Changes View.
 * Use the special method or supply additional parameter to the constructor to show the balloon over the Version Control View.
 */
public class VcsBalloonProblemNotifier implements Runnable {
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Common Version Control Messages",
                                                                                               VcsToolWindow.ID, true);
  private final Project myProject;
  private final String myMessage;
  private final NotificationType myMessageType;
  private final boolean myShowOverChangesView;
  @Nullable
  private final NamedRunnable[] myNotificationListener;

  public VcsBalloonProblemNotifier(@Nonnull final Project project, @Nonnull final String message, final NotificationType messageType) {
    this(project, message, messageType, true, null);
  }

  public VcsBalloonProblemNotifier(@Nonnull final Project project,
                                   @Nonnull final String message,
                                   final NotificationType messageType,
                                   boolean showOverChangesView,
                                   @Nullable final NamedRunnable[] notificationListener) {
    myProject = project;
    myMessage = message;
    myMessageType = messageType;
    myShowOverChangesView = showOverChangesView;
    myNotificationListener = notificationListener;
  }

  public static void showOverChangesView(@Nonnull final Project project,
                                         @Nonnull final String message,
                                         final NotificationType type,
                                         final NamedRunnable... notificationListener) {
    show(project, message, type, true, notificationListener);
  }

  public static void showOverVersionControlView(@Nonnull final Project project,
                                                @Nonnull final String message,
                                                final NotificationType type) {
    show(project, message, type, false, null);
  }

  private static void show(final Project project,
                           final String message,
                           final NotificationType type,
                           final boolean showOverChangesView,
                           @Nullable final NamedRunnable[] notificationListener) {
    final Application application = ApplicationManager.getApplication();
    if (application.isHeadlessEnvironment()) return;
    final Runnable showErrorAction = new Runnable() {
      public void run() {
        new VcsBalloonProblemNotifier(project, message, type, showOverChangesView, notificationListener).run();
      }
    };
    if (application.isDispatchThread()) {
      showErrorAction.run();
    }
    else {
      application.invokeLater(showErrorAction);
    }
  }

  public void run() {
    final Notification notification;
    if (myNotificationListener != null && myNotificationListener.length > 0) {
      final StringBuilder sb = new StringBuilder(myMessage);
      for (NamedRunnable runnable : myNotificationListener) {
        final String name = runnable.toString();
        sb.append("<br/><a href=\"").append(name).append("\">").append(name).append("</a>");
      }
      notification = NOTIFICATION_GROUP.createNotification(myMessageType.name(), sb.toString(), myMessageType, new NotificationListener() {
        @Override
        public void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
          if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) {
            if (myNotificationListener.length == 1) {
              myNotificationListener[0].run();
            }
            else {
              final String description = event.getDescription();
              if (description != null) {
                for (NamedRunnable runnable : myNotificationListener) {
                  if (description.equals(runnable.toString())) {
                    runnable.run();
                    break;
                  }
                }
              }
            }
            notification.expire();
          }
        }
      });
    }
    else {
      notification = NOTIFICATION_GROUP.createNotification(myMessage, myMessageType);
    }
    notification.notify(myProject.isDefault() ? null : myProject);
  }
}
