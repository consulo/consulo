/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project.ui.notification.event;

import consulo.platform.Platform;
import consulo.project.ui.notification.Notification;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;

/**
 * Called when a link in the HTML content of a notification is clicked. {@link HyperlinkEvent#getDescription()} is the
 * {@code href} of the link.
 */
@FunctionalInterface
public interface NotificationListener {
    NotificationListener URL_OPENING_LISTENER = new UrlOpeningListener(false);

    @RequiredUIAccess
    void hyperlinkActivated(Notification notification, HyperlinkEvent event);

    class UrlOpeningListener implements NotificationListener {
        private final boolean myExpireNotification;

        public UrlOpeningListener(boolean expireNotification) {
            myExpireNotification = expireNotification;
        }

        @RequiredUIAccess
        @Override
        public void hyperlinkActivated(Notification notification, HyperlinkEvent event) {
            Platform.current().openInBrowser(event.getDescription());
            if (myExpireNotification) {
                notification.expire();
            }
        }
    }
}
