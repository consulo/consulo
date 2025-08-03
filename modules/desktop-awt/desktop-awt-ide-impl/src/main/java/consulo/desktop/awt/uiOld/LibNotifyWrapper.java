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

import consulo.application.internal.AppLifecycleListener;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import consulo.container.boot.ContainerPathManager;
import consulo.desktop.awt.ui.util.AppIconUtil;

import jakarta.annotation.Nonnull;

/**
 * @author Denis Fokin
 */
class LibNotifyWrapper implements SystemNotificationsImpl.Notifier {
  private static LibNotifyWrapper ourInstance;

  public static synchronized LibNotifyWrapper getInstance() {
    if (ourInstance == null) {
      ourInstance = new LibNotifyWrapper();
    }
    return ourInstance;
  }

  @SuppressWarnings("SpellCheckingInspection")
  private interface LibNotify extends Library {
    int notify_init(String appName);
    void notify_uninit();
    Pointer notify_notification_new(String summary, String body, String icon);
    int notify_notification_show(Pointer notification, Pointer error);
  }

  private final LibNotify myLibNotify;
  private final String myIcon;
  private final Object myLock = new Object();
  private boolean myDisposed = false;

  private LibNotifyWrapper() {
    myLibNotify = Native.load("libnotify.so.4", LibNotify.class);

    String appName = Application.get().getName().getValue();
    if (myLibNotify.notify_init(appName) == 0) {
      throw new IllegalStateException("notify_init failed");
    }

    String icon = AppIconUtil.findIcon(ContainerPathManager.get().getAppHomeDirectory().getPath());
    myIcon = icon != null ? icon : "dialog-information";

    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
    connection.subscribe(AppLifecycleListener.class, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        synchronized (myLock) {
          myDisposed = true;
          myLibNotify.notify_uninit();
        }
      }
    });
  }

  @Override
  public void notify(@Nonnull String name, @Nonnull String title, @Nonnull String description) {
    synchronized (myLock) {
      if (!myDisposed) {
        Pointer notification = myLibNotify.notify_notification_new(title, description, myIcon);
        myLibNotify.notify_notification_show(notification, null);
      }
    }
  }
}
