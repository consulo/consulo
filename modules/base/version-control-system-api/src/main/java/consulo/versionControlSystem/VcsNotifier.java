/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.*;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsNotifier {

  public static final NotificationGroup NOTIFICATION_GROUP_ID = NotificationGroup.toolWindowGroup("Vcs Messages", VcsToolWindow.ID);
  public static final NotificationGroup IMPORTANT_ERROR_NOTIFICATION = new NotificationGroup("Vcs Important Messages", NotificationDisplayType.STICKY_BALLOON, true);
  public static final NotificationGroup STANDARD_NOTIFICATION = new NotificationGroup("Vcs Notifications", NotificationDisplayType.BALLOON, true);
  public static final NotificationGroup SILENT_NOTIFICATION = new NotificationGroup("Vcs Silent Notifications", NotificationDisplayType.NONE, true);

  @Nonnull
  private final Project myProject;

  public static VcsNotifier getInstance(@Nonnull Project project) {
    return project.getInstance(VcsNotifier.class);
  }

  @Inject
  public VcsNotifier(@Nonnull Project project) {
    myProject = project;
  }

  @Nonnull
  public static Notification createNotification(@Nonnull NotificationGroup notificationGroup,
                                                @Nonnull String title,
                                                @Nonnull String message,
                                                @Nonnull NotificationType type,
                                                @Nullable NotificationListener listener) {
    // title can be empty; message can't be neither null, nor empty
    if (StringUtil.isEmptyOrSpaces(message)) {
      message = title;
      title = "";
    }
    // if both title and message were empty, then it is a problem in the calling code => Notifications engine assertion will notify.
    return notificationGroup.newOfType(type)
        .title(LocalizeValue.localizeTODO(title))
        .content(LocalizeValue.localizeTODO(message))
        .optionalHyperlinkListener(listener)
        .create();
  }

  @Nonnull
  public Notification notify(@Nonnull NotificationGroup notificationGroup, @Nonnull String title, @Nonnull String message, @Nonnull NotificationType type, @Nullable NotificationListener listener) {
    Notification notification = createNotification(notificationGroup, title, message, type, listener);
    notification.notify(myProject);
    return notification;
  }

  @Nonnull
  public Notification notify(@Nonnull Notification notification) {
    notification.notify(myProject);
    return notification;
  }

  @Nonnull
  public Notification notifyError(@Nullable String displayId, @Nonnull String title, @Nonnull String message, @Nonnull NotificationAction... actions) {
    return notifyError(title, message, null).addActions(actions);
  }

  @Nonnull
  public Notification notifyError(@Nonnull String title, @Nonnull String message) {
    return notifyError(title, message, null);
  }

  @Nonnull
  public Notification notifyError(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.ERROR, listener);
  }

  @Nonnull
  public Notification notifyWeakError(@Nonnull String message) {
    return notify(NOTIFICATION_GROUP_ID, "", message, NotificationType.ERROR, null);
  }

  @Nonnull
  public Notification notifySuccess(@Nonnull String message) {
    return notifySuccess("", message);
  }

  @Nonnull
  public Notification notifySuccess(@Nonnull String title, @Nonnull String message) {
    return notifySuccess(title, message, null);
  }

  @Nonnull
  public Notification notifySuccess(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION, listener);
  }

  @Nonnull
  public Notification notifyImportantInfo(@Nonnull String title, @Nonnull String message) {
    return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.INFORMATION, null);
  }

  @Nonnull
  public Notification notifyImportantInfo(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.INFORMATION, listener);
  }

  @Nonnull
  public Notification notifyInfo(@Nonnull String message) {
    return notifyInfo("", message);
  }

  @Nonnull
  public Notification notifyInfo(@Nonnull String title, @Nonnull String message) {
    return notifyInfo(title, message, null);
  }

  @Nonnull
  public Notification notifyInfo(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION, listener);
  }

  @Nonnull
  public Notification notifyMinorWarning(@Nonnull String title, @Nonnull String message) {
    return notifyMinorWarning(title, message, null);
  }

  @Nonnull
  public Notification notifyMinorWarning(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(STANDARD_NOTIFICATION, title, message, NotificationType.WARNING, listener);
  }

  @Nonnull
  public Notification notifyWarning(@Nonnull String title, @Nonnull String message) {
    return notifyWarning(title, message, null);
  }

  @Nonnull
  public Notification notifyWarning(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.WARNING, listener);
  }

  @Nonnull
  public Notification notifyImportantWarning(@Nonnull String title, @Nonnull String message) {
    return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.WARNING, null);
  }

  @Nonnull
  public Notification notifyImportantWarning(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.WARNING, listener);
  }

  @Nonnull
  public Notification notifyMinorInfo(@Nonnull String title, @Nonnull String message) {
    return notifyMinorInfo(title, message, null);
  }

  @Nonnull
  public Notification notifyMinorInfo(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
    return notify(STANDARD_NOTIFICATION, title, message, NotificationType.INFORMATION, listener);
  }

  @Nonnull
  public Notification notifyMinorInfo(@Nullable String displayId, @Nonnull String title, @Nonnull String message, @Nonnull NotificationAction... actions) {
    return notify(STANDARD_NOTIFICATION, title, message, NotificationType.INFORMATION, null).addActions(actions);
  }

  public Notification logInfo(@Nonnull String title, @Nonnull String message) {
    return notify(SILENT_NOTIFICATION, title, message, NotificationType.INFORMATION, null);
  }
}
