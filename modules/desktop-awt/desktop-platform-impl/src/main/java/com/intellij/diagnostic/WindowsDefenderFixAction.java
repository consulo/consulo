/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.diagnostic;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Collection;

/**
 * from kotlin
 */
public class WindowsDefenderFixAction extends NotificationAction {
  private final Collection<Path> myPaths;

  public WindowsDefenderFixAction(Collection<Path> paths) {
    super("Fix...");
    myPaths = paths;
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
    int rc = Messages.showDialog(e.getProject(), DiagnosticBundle
                                         .message("virus.scanning.fix.explanation", ApplicationNamesInfo.getInstance().getFullProductName(), WindowsDefenderChecker.getInstance().getConfigurationInstructionsUrl()),
                                 DiagnosticBundle.message("virus.scanning.fix.title"),
                                 new String[]{DiagnosticBundle.message("virus.scanning.fix.automatically"), DiagnosticBundle.message("virus.scanning.fix.manually"),
                                         CommonBundle.getCancelButtonText()}, 0, null);

    switch (rc) {
      case Messages.OK:
        notification.expire();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          if (WindowsDefenderChecker.getInstance().runExcludePathsCommand(e.getProject(), myPaths)) {
            UIUtil.invokeLaterIfNeeded(() -> {
              Notifications.Bus.notifyAndHide(new Notification("System Health", "", DiagnosticBundle.message("virus.scanning.fix.success.notification"), NotificationType.INFORMATION), e.getProject());
            });
          }
        });

        break;
      case Messages.CANCEL:
        BrowserUtil.browse(WindowsDefenderChecker.getInstance().getConfigurationInstructionsUrl());
        break;
    }
  }
}
