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
package consulo.desktop.awt.uiOld;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationActivationListener;
import consulo.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import consulo.project.ui.wm.IdeFrame;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.component.messagebus.MessageBusConnection;
import javax.annotation.Nonnull;

import static consulo.application.util.mac.foundation.Foundation.invoke;
import static consulo.application.util.mac.foundation.Foundation.nsString;

/**
 * @author Dennis.Ushakov
 */
class MountainLionNotifications implements SystemNotificationsImpl.Notifier {
  private static MountainLionNotifications ourInstance;

  public static synchronized MountainLionNotifications getInstance() {
    if (ourInstance == null) {
      ourInstance = new MountainLionNotifications();
    }
    return ourInstance;
  }

  private MountainLionNotifications() {
    final MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
    connection.subscribe(ApplicationActivationListener.TOPIC, new ApplicationActivationListener() {
      @Override
      public void applicationActivated(IdeFrame ideFrame) {
        cleanupDeliveredNotifications();
      }
    });
    connection.subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        cleanupDeliveredNotifications();
      }
    });
  }

  @Override
  public void notify(@Nonnull String name, @Nonnull String title, @Nonnull String description) {
    final ID notification = invoke(Foundation.getObjcClass("NSUserNotification"), "new");
    invoke(notification, "setTitle:", nsString(StringUtil.stripHtml(title, true).replace("%", "%%")));
    invoke(notification, "setInformativeText:", nsString(StringUtil.stripHtml(description, true).replace("%", "%%")));
    final ID center = invoke(Foundation.getObjcClass("NSUserNotificationCenter"), "defaultUserNotificationCenter");
    invoke(center, "deliverNotification:", notification);
  }

  private static void cleanupDeliveredNotifications() {
    final ID center = invoke(Foundation.getObjcClass("NSUserNotificationCenter"), "defaultUserNotificationCenter");
    invoke(center, "removeAllDeliveredNotifications");
  }
}
