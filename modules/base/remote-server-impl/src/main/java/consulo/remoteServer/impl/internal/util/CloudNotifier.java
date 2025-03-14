// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.impl.internal.util;

import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;

/**
 * @author michael.golubev
 */
public class CloudNotifier {
    private final String myNotificationDisplayId;

    public CloudNotifier(String notificationDisplayId) {
        myNotificationDisplayId = notificationDisplayId;
    }

    public void showMessage(String message, NotificationType messageType) {
        // TODO this is invalid due group must be registered before !
        NotificationGroup group = NotificationGroup.balloonGroup(myNotificationDisplayId);

        group.createNotification(message, messageType).notify(null);
    }
}
