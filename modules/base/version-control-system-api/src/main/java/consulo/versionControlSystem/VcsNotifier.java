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

import consulo.annotation.DeprecationInfo;
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
    public static final NotificationGroup NOTIFICATION_GROUP_ID =
        NotificationGroup.toolWindowGroup("Vcs Messages", VcsToolWindow.ID);
    public static final NotificationGroup IMPORTANT_ERROR_NOTIFICATION =
        new NotificationGroup("Vcs Important Messages", NotificationDisplayType.STICKY_BALLOON, true);
    public static final NotificationGroup STANDARD_NOTIFICATION =
        new NotificationGroup("Vcs Notifications", NotificationDisplayType.BALLOON, true);
    public static final NotificationGroup SILENT_NOTIFICATION =
        new NotificationGroup("Vcs Silent Notifications", NotificationDisplayType.NONE, true);

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final NotificationService myNotificationService;

    public static VcsNotifier getInstance(@Nonnull Project project) {
        return project.getInstance(VcsNotifier.class);
    }

    @Inject
    public VcsNotifier(@Nonnull Project project, @Nonnull NotificationService notificationService) {
        myProject = project;
        myNotificationService = notificationService;
    }

    @Nonnull
    public static Notification createNotification(
        @Nonnull NotificationGroup notificationGroup,
        @Nonnull String title,
        @Nonnull String message,
        @Nonnull NotificationType type,
        @Nullable NotificationListener listener
    ) {
        // title can be empty; message can't be neither null, nor empty
        if (StringUtil.isEmptyOrSpaces(message)) {
            message = title;
            title = "";
        }
        // if both title and message were empty, then it is a problem in the calling code => Notifications engine assertion will notify.

        return NotificationService.getInstance()
            .newOfType(notificationGroup, type)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notify(
        @Nonnull NotificationGroup notificationGroup,
        @Nonnull String title,
        @Nonnull String message,
        @Nonnull NotificationType type,
        @Nullable NotificationListener listener
    ) {
        return myNotificationService.newOfType(notificationGroup, type)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @Nonnull
    public Notification notify(@Nonnull Notification notification) {
        notification.notify(myProject);
        return notification;
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyError(
        @Nullable String displayId,
        @Nonnull String title,
        @Nonnull String message,
        @Nonnull NotificationAction... actions
    ) {
        return myNotificationService.newError(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyError(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newError(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyError(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newError(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyWeakError(@Nonnull String message) {
        return myNotificationService.newError(NOTIFICATION_GROUP_ID)
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifySuccess(@Nonnull String message) {
        return myNotificationService.newInfo(NOTIFICATION_GROUP_ID)
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifySuccess(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newInfo(NOTIFICATION_GROUP_ID)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifySuccess(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newInfo(NOTIFICATION_GROUP_ID)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyImportantInfo(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newInfo(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyImportantInfo(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newInfo(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyInfo(@Nonnull String message) {
        return myNotificationService.newInfo(NOTIFICATION_GROUP_ID)
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyInfo(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newInfo(NOTIFICATION_GROUP_ID)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyInfo(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newInfo(NOTIFICATION_GROUP_ID)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyMinorWarning(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newWarn(STANDARD_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyMinorWarning(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newWarn(STANDARD_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyWarning(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newWarn(NOTIFICATION_GROUP_ID)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyWarning(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newWarn(NOTIFICATION_GROUP_ID)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyImportantWarning(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newWarn(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyImportantWarning(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newWarn(IMPORTANT_ERROR_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyMinorInfo(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newInfo(STANDARD_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyMinorInfo(@Nonnull String title, @Nonnull String message, @Nullable NotificationListener listener) {
        return myNotificationService.newInfo(STANDARD_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .optionalHyperlinkListener(listener)
            .notifyAndGet(myProject);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    @Nonnull
    public Notification notifyMinorInfo(
        @Nullable String displayId,
        @Nonnull String title,
        @Nonnull String message,
        @Nonnull NotificationAction... actions
    ) {
        return myNotificationService.newInfo(STANDARD_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject)
            .addActions(actions);
    }

    @Deprecated
    @DeprecationInfo("Use NotificationGroup.newError/newWarning/newInfo()...notify()")
    public Notification logInfo(@Nonnull String title, @Nonnull String message) {
        return myNotificationService.newInfo(SILENT_NOTIFICATION)
            .title(LocalizeValue.localizeTODO(title))
            .content(LocalizeValue.localizeTODO(message))
            .notifyAndGet(myProject);
    }
}
