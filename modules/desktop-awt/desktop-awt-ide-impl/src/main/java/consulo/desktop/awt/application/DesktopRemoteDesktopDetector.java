/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.application;

import com.sun.jna.platform.win32.User32;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.ui.RemoteDesktopService;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.awt.*;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionListener;

@Singleton
@ServiceImpl
public class DesktopRemoteDesktopDetector extends RemoteDesktopService {
  private static final Logger LOG = Logger.getInstance(DesktopRemoteDesktopDetector.class);
  public static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("Remote Desktop", NotificationDisplayType.BALLOON, false);

  private volatile boolean myFailureDetected;
  private volatile boolean myRemoteDesktopConnected;

  @Inject
  DesktopRemoteDesktopDetector() {
    if (Platform.current().os().isWindows()) {
      Desktop.getDesktop().addAppEventListener(new UserSessionListener() {
        @Override
        public void userSessionDeactivated(UserSessionEvent e) {
          updateState();
        }

        @Override
        public void userSessionActivated(UserSessionEvent e) {
          updateState();
        }
      });

      updateState();
    }
  }

  private void updateState() {
    if (!myFailureDetected) {
      try {
        // This might not work in all cases, but hopefully is a more reliable method than the current one (checking for font smoothing)
        // see https://msdn.microsoft.com/en-us/library/aa380798%28v=vs.85%29.aspx
        boolean newValue = User32.INSTANCE.GetSystemMetrics(0x1000) != 0; // 0x1000 is SM_REMOTESESSION
        LOG.debug("Detected remote desktop: ", newValue);
        if (newValue != myRemoteDesktopConnected) {
          myRemoteDesktopConnected = newValue;
          if (myRemoteDesktopConnected) {
            // We postpone notification to avoid recursive initialization of RemoteDesktopDetector
            // (in case it's initialized by request from consulo.ide.impl.idea.notification.EventLog)
            ApplicationManager.getApplication().invokeLater(() -> Notifications.Bus
                    .notify(NOTIFICATION_GROUP.createNotification(ApplicationBundle.message("remote.desktop.detected.message"), NotificationType.INFORMATION)
                                    .setTitle(ApplicationBundle.message("remote.desktop.detected.title"))));
          }
        }
      }
      catch (Throwable e) {
        myRemoteDesktopConnected = false;
        myFailureDetected = true;
        LOG.warn("Error while calling GetSystemMetrics", e);
      }
    }
  }

  @Override
  public boolean isRemoteDesktopConnected() {
    return myRemoteDesktopConnected;
  }
}
