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
import org.jspecify.annotations.Nullable;

/**
 * @author peter
 */
public final class NotificationGroup {
    
    private final String myId;
    
    private final LocalizeValue myDisplayName;
    
    private final NotificationDisplayType myDisplayType;
    private final boolean myLogByDefault;
    private final @Nullable String myToolWindowId;

    @Deprecated
    @DeprecationInfo("Use constructor with LocalizeValue parameter")
    public NotificationGroup(String id, NotificationDisplayType defaultDisplayType, boolean logByDefault) {
        this(id, LocalizeValue.of(id), defaultDisplayType, logByDefault, null);
    }

    public NotificationGroup(
        String id,
        LocalizeValue displayName,
        NotificationDisplayType defaultDisplayType,
        boolean logByDefault
    ) {
        this(id, displayName, defaultDisplayType, logByDefault, null);
    }

    @Deprecated
    @DeprecationInfo("Use constructor with LocalizeValue parameter")
    public NotificationGroup(
        String id,
        NotificationDisplayType defaultDisplayType,
        boolean logByDefault,
        @Nullable String toolWindowId
    ) {
        this(id, LocalizeValue.of(id), defaultDisplayType, logByDefault, toolWindowId);
    }

    public NotificationGroup(
        String id,
        LocalizeValue displayName,
        NotificationDisplayType defaultDisplayType,
        boolean logByDefault,
        @Nullable String toolWindowId
    ) {
        myId = id;
        myDisplayType = defaultDisplayType;
        myLogByDefault = logByDefault;
        myToolWindowId = toolWindowId;
        myDisplayName = displayName;
    }

    
    @Deprecated
    public static NotificationGroup balloonGroup(String id) {
        return new NotificationGroup(id, NotificationDisplayType.BALLOON, true);
    }

    
    public static NotificationGroup balloonGroup(String id, LocalizeValue displayName) {
        return new NotificationGroup(id, displayName, NotificationDisplayType.BALLOON, true);
    }

    
    @Deprecated
    public static NotificationGroup logOnlyGroup(String displayId) {
        return new NotificationGroup(displayId, NotificationDisplayType.NONE, true);
    }

    
    public static NotificationGroup logOnlyGroup(String id, LocalizeValue displayName) {
        return new NotificationGroup(id, displayName, NotificationDisplayType.NONE, true);
    }

    
    @Deprecated
    public static NotificationGroup toolWindowGroup(String id, String toolWindowId, boolean logByDefault) {
        return new NotificationGroup(id, NotificationDisplayType.TOOL_WINDOW, logByDefault, toolWindowId);
    }

    
    public static NotificationGroup toolWindowGroup(
        String id,
        LocalizeValue displayName,
        String toolWindowId,
        boolean logByDefault
    ) {
        return new NotificationGroup(id, displayName, NotificationDisplayType.TOOL_WINDOW, logByDefault, toolWindowId);
    }

    
    @Deprecated
    public static NotificationGroup toolWindowGroup(String id, String toolWindowId) {
        return toolWindowGroup(id, toolWindowId, true);
    }

    
    public static NotificationGroup toolWindowGroup(String id, LocalizeValue displayName, String toolWindowId) {
        return toolWindowGroup(id, displayName, toolWindowId, true);
    }

    
    public String getId() {
        return myId;
    }

    
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    @Deprecated
    @DeprecationInfo("Use NotificationService.newError/newWarning/newInfo/newOfType()...create()")
    
    public Notification createNotification(String content, NotificationType type) {
        return NotificationService.getInstance()
            .newOfType(this, type)
            .content(LocalizeValue.of(content))
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use NotificationService.newError/newWarning/newInfo/newOfType()...create()")
    
    public Notification createNotification(
        String title,
        String content,
        NotificationType type,
        @Nullable NotificationListener listener
    ) {
        return NotificationService.getInstance()
            .newOfType(this, type)
            .title(LocalizeValue.of(title))
            .content(LocalizeValue.of(content))
            .optionalHyperlinkListener(listener)
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use NotificationService.newError/newWarning/newInfo/newOfType()...create()")
    
    public Notification createNotification() {
        return NotificationService.getInstance().newInfo(this).create();
    }

    @Deprecated
    @DeprecationInfo("Use NotificationService.newError/newWarning/newInfo/newOfType()...create()")
    
    public Notification createNotification(NotificationType type) {
        return NotificationService.getInstance().newOfType(this, type).create();
    }

    @Deprecated
    @DeprecationInfo("Use NotificationService.newError/newWarning/newInfo/newOfType()...create()")
    
    public Notification createNotification(
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable String content,
        NotificationType type
    ) {
        return NotificationService.getInstance().newOfType(this, type)
            .title(LocalizeValue.ofNullable(title))
            .subtitle(LocalizeValue.ofNullable(subtitle))
            .subtitle(LocalizeValue.ofNullable(content))
            .create();
    }

    @Deprecated
    @DeprecationInfo("Use NotificationService.newError/newWarning/newInfo/newOfType()...create()")
    
    public Notification createNotification(
        @Nullable String title,
        @Nullable String subtitle,
        @Nullable String content,
        NotificationType type,
        @Nullable NotificationListener listener
    ) {
        return NotificationService.getInstance().newOfType(this, type)
            .title(LocalizeValue.ofNullable(title))
            .subtitle(LocalizeValue.ofNullable(subtitle))
            .subtitle(LocalizeValue.ofNullable(content))
            .optionalHyperlinkListener(listener)
            .create();
    }

    
    public NotificationDisplayType getDisplayType() {
        return myDisplayType;
    }

    public boolean isLogByDefault() {
        return myLogByDefault;
    }

    public @Nullable String getToolWindowId() {
        return myToolWindowId;
    }
}
