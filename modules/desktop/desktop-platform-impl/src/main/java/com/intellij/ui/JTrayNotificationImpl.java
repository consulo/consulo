// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author Alexander Lobas
 */
final class JTrayNotificationImpl implements SystemNotificationsImpl.Notifier {
  private static JTrayNotificationImpl ourInstance;

  @Nullable
  static synchronized JTrayNotificationImpl getWin10Instance() throws AWTException {
    if (ourInstance == null && SystemTray.isSupported()) {
      ourInstance = new JTrayNotificationImpl();
    }
    return ourInstance;
  }

  private final TrayIcon myTrayIcon;

  private JTrayNotificationImpl() throws AWTException {
    String tooltip = Application.get().getName().getValue();
    myTrayIcon = new TrayIcon(AppUIUtil.loadWindowIcon(false), tooltip);
    myTrayIcon.setImageAutoSize(true);
    myTrayIcon.addActionListener(e -> {
      IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();

      if(frame != null) {
        consulo.ui.Window window = frame.getWindow();

        Window awtWindow = TargetAWT.to(window);
        if (awtWindow != null) {
          UIUtil.toFront(awtWindow);
        }
      }
    });

    SystemTray.getSystemTray().add(myTrayIcon);
  }

  @Override
  public void notify(@Nonnull String name, @Nonnull String title, @Nonnull String description) {
    myTrayIcon.displayMessage(title, description, TrayIcon.MessageType.INFO);
  }
}