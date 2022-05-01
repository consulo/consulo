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
package consulo.desktop.util.windows.defender;

import consulo.ide.impl.idea.diagnostic.DiagnosticBundle;
import consulo.ide.impl.idea.ide.BrowserUtil;
import consulo.ide.impl.idea.notification.NotificationAction;
import consulo.ui.ex.awt.Messages;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.language.editor.CommonDataKeys;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIUtil;

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
    int rc = Messages.showDialog(e.getData(CommonDataKeys.PROJECT),
                                 DiagnosticBundle.message("virus.scanning.fix.explanation", Application.get().getName().get(), WindowsDefenderChecker.getInstance().getConfigurationInstructionsUrl()),
                                 DiagnosticBundle.message("virus.scanning.fix.title"),
                                 new String[]{DiagnosticBundle.message("virus.scanning.fix.automatically"), DiagnosticBundle.message("virus.scanning.fix.manually"),
                                         CommonBundle.getCancelButtonText()}, 0, null);

    switch (rc) {
      case Messages.OK:
        notification.expire();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          if (WindowsDefenderChecker.getInstance().runExcludePathsCommand(e.getData(CommonDataKeys.PROJECT), myPaths)) {
            UIUtil.invokeLaterIfNeeded(() -> {
              Notifications.Bus.notifyAndHide(new Notification("System Health", "", DiagnosticBundle.message("virus.scanning.fix.success.notification"), NotificationType.INFORMATION),
                                              e.getData(CommonDataKeys.PROJECT));
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
