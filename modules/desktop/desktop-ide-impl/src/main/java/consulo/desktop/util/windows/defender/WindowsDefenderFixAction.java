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

import consulo.application.Application;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ide.impl.idea.ide.SystemHealthMonitorImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.project.ui.notification.Notifications;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Collection;

/**
 * from kotlin
 */
public class WindowsDefenderFixAction extends NotificationAction {
    private final Collection<Path> myPaths;

    public WindowsDefenderFixAction(Collection<Path> paths) {
        super(LocalizeValue.localizeTODO("Fix..."));
        myPaths = paths;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
        int rc = Messages.showDialog(
            e.getData(Project.KEY),
            ExternalServiceLocalize.virusScanningFixExplanation(
                Application.get().getName().get(),
                WindowsDefenderChecker.getInstance().getConfigurationInstructionsUrl()
            ).get(),
            ExternalServiceLocalize.virusScanningFixTitle().get(),
            new String[]{
                ExternalServiceLocalize.virusScanningFixAutomatically().get(),
                ExternalServiceLocalize.virusScanningFixManually().get(),
                CommonLocalize.buttonCancel().get()
            },
            0,
            null
        );

        switch (rc) {
            case Messages.OK:
                notification.expire();
                Application.get().executeOnPooledThread(() -> {
                    if (WindowsDefenderChecker.getInstance().runExcludePathsCommand(e.getData(Project.KEY), myPaths)) {
                        UIUtil.invokeLaterIfNeeded(() -> Notifications.Bus.notifyAndHide(
                            SystemHealthMonitorImpl.GROUP.newInfo()
                                .content(ExternalServiceLocalize.virusScanningFixSuccessNotification())
                                .create(),
                            e.getData(Project.KEY)
                        ));
                    }
                });

                break;
            case Messages.CANCEL:
                BrowserUtil.browse(WindowsDefenderChecker.getInstance().getConfigurationInstructionsUrl());
                break;
        }
    }
}
