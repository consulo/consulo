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
package consulo.project.ui.notification;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.event.NotificationListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public final class NotificationGroup {
    @Nonnull
    private final String myId;
    @Nonnull
    private final LocalizeValue myDisplayName;
    @Nonnull
    private final NotificationDisplayType myDisplayType;
    private final boolean myLogByDefault;
    @Nullable
    private final String myToolWindowId;

    @Deprecated
    @DeprecationInfo("Use constructor with LocalizeValue parameter")
    public NotificationGroup(@Nonnull String id, @Nonnull NotificationDisplayType defaultDisplayType, boolean logByDefault) {
        this(id, LocalizeValue.of(id), defaultDisplayType, logByDefault, null);
    }

    public NotificationGroup(
        @Nonnull String id,
        @Nonnull LocalizeValue displayName,
        @Nonnull NotificationDisplayType defaultDisplayType,
        boolean logByDefault
    ) {
        this(id, displayName, defaultDisplayType, logByDefault, null);
    }

    @Deprecated
    @DeprecationInfo("Use constructor with LocalizeValue parameter")
    public NotificationGroup(
        @Nonnull String id,
        @Nonnull NotificationDisplayType defaultDisplayType,
        boolean logByDefault,
        @Nullable String toolWindowId
    ) {
        this(id, LocalizeValue.of(id), defaultDisplayType, logByDefault, toolWindowId);
    }

    public NotificationGroup(
        @Nonnull String id,
        @Nonnull LocalizeValue displayName,
        @Nonnull NotificationDisplayType defaultDisplayType,
        boolean logByDefault,
        @Nullable String toolWindowId
    ) {
        myId = id;
        myDisplayType = defaultDisplayType;
        myLogByDefault = logByDefault;
        myToolWindowId = toolWindowId;
        myDisplayName = displayName;
    }

    @Nonnull
    @Deprecated
    public static NotificationGroup balloonGroup(@Nonnull String id) {
        return new NotificationGroup(id, NotificationDisplayType.BALLOON, true);
    }

    @Nonnull
    public static NotificationGroup balloonGroup(@Nonnull String id, @Nonnull LocalizeValue displayName) {
        return new NotificationGroup(id, displayName, NotificationDisplayType.BALLOON, true);
    }

    @Nonnull
    @Deprecated
    public static NotificationGroup logOnlyGroup(@Nonnull String displayId) {
        return new NotificationGroup(displayId, NotificationDisplayType.NONE, true);
    }

    @Nonnull
    public static NotificationGroup logOnlyGroup(@Nonnull String id, @Nonnull LocalizeValue displayName) {
        return new NotificationGroup(id, displayName, NotificationDisplayType.NONE, true);
    }

    @Nonnull
    @Deprecated
    public static NotificationGroup toolWindowGroup(@Nonnull String id, @Nonnull String toolWindowId, boolean logByDefault) {
        return new NotificationGroup(id, NotificationDisplayType.TOOL_WINDOW, logByDefault, toolWindowId);
    }

    @Nonnull
    public static NotificationGroup toolWindowGroup(
        @Nonnull String id,
        @Nonnull LocalizeValue displayName,
        @Nonnull String toolWindowId,
        boolean logByDefault
    ) {
        return new NotificationGroup(id, displayName, NotificationDisplayType.TOOL_WINDOW, logByDefault, toolWindowId);
    }

    @Nonnull
    @Deprecated
    public static NotificationGroup toolWindowGroup(@Nonnull String id, @Nonnull String toolWindowId) {
        return toolWindowGroup(id, toolWindowId, true);
    }

    @Nonnull
    public static NotificationGroup toolWindowGroup(@Nonnull String id, @Nonnull LocalizeValue displayName, @Nonnull String toolWindowId) {
        return toolWindowGroup(id, displayName, toolWindowId, true);
    }

    @Nonnull
    public String getId() {
        return myId;
    }

    @Nonnull
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    public Notification.Builder newNotification(@Nonnull NotificationType type) {
        return new Notification.Builder(this, type);
    }

    public Notification.Builder newError() {
        return new Notification.Builder(this, NotificationType.ERROR);
    }

    public Notification.Builder newWarning() {
        return new Notification.Builder(this, NotificationType.WARNING);
    }

    public Notification.Builder newInfo() {
        return new Notification.Builder(this, NotificationType.INFORMATION);
    }

    @Deprecated
    @DeprecationInfo("Use buildError/buildWarning/buildInfo/buildNotification()...create()")
    @Nonnull
    public Notification createNotification(@Nonnull String content, @Nonnull NotificationType type) {
        return newNotification(type)
            .content(LocalizeValue.of(content))
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use buildError/buildWarning/buildInfo/buildNotification()...create()")
    @Nonnull
    public Notification createNotification(
        @Nonnull String title,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable NotificationListener listener
    ) {
        return newNotification(type)
            .title(LocalizeValue.of(title))
            .content(LocalizeValue.of(content))
            .optionalListener(listener)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use buildError/buildWarning/buildInfo/buildNotification()...create()")
    @Nonnull
    public Notification createNotification() {
        return newInfo().create();
    }

    @Deprecated
    @DeprecationInfo("Use buildError/buildWarning/buildInfo/buildNotification()...create()")
    @Nonnull
    public Notification createNotification(@Nonnull NotificationType type) {
        return newNotification(type).create();
    }

    @Deprecated
    @DeprecationInfo("Use buildError/buildWarning/buildInfo/buildNotification()...create()")
    @Nonnull
    public Notification createNotification(
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable String content,
        @Nonnull NotificationType type
    ) {
        return newNotification(type)
            .title(LocalizeValue.ofNullable(title))
            .subtitle(LocalizeValue.ofNullable(subtitle))
            .subtitle(LocalizeValue.ofNullable(content))
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use buildError/buildWarning/buildInfo/buildNotification()...create()")
    @Nonnull
    public Notification createNotification(
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable String content,
        @Nonnull NotificationType type,
        @Nullable NotificationListener listener
    ) {
        return newNotification(type)
            .title(LocalizeValue.ofNullable(title))
            .subtitle(LocalizeValue.ofNullable(subtitle))
            .subtitle(LocalizeValue.ofNullable(content))
            .optionalListener(listener)
            .create();
    }

    @Nonnull
    public NotificationDisplayType getDisplayType() {
        return myDisplayType;
    }

    public boolean isLogByDefault() {
        return myLogByDefault;
    }

    @Nullable
    public String getToolWindowId() {
        return myToolWindowId;
    }
}
