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

import jakarta.annotation.Nonnull;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;

public interface NotificationListener {
  @RequiredUIAccess
  void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event);

  abstract class Adapter implements NotificationListener {
    @Override
    public final void hyperlinkUpdate(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
      if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        hyperlinkActivated(notification, event);
      }
    }

    protected abstract void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e);
  }

  NotificationListener URL_OPENING_LISTENER = new UrlOpeningListener(false);

  class UrlOpeningListener extends Adapter {
    private final boolean myExpireNotification;

    public UrlOpeningListener(boolean expireNotification) {
      myExpireNotification = expireNotification;
    }

    @Override
    protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
      URL url = event.getURL();
      if (url == null) {
        Platform.current().openInBrowser(event.getDescription());
      }
      else {
        Platform.current().openInBrowser(url);
      }
      if (myExpireNotification) {
        notification.expire();
      }
    }
  }
}