// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ide.impl.idea.ide.SystemHealthMonitorImpl;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.UIAccess;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ExtensionImpl
public class WindowsDefenderCheckerActivity implements BackgroundStartupActivity, DumbAware {
    private static final Logger LOG = Logger.getInstance(WindowsDefenderCheckerActivity.class);

    private final Application myApplication;
    private final Provider<WindowsDefenderChecker> myWindowsDefenderChecker;
    private final Provider<EarlyAccessProgramManager> myEarlyAccessProgramManager;
    private final NotificationService myNotificationService;

    @Inject
    public WindowsDefenderCheckerActivity(
        Application application,
        Provider<WindowsDefenderChecker> windowsDefenderChecker,
        Provider<EarlyAccessProgramManager> earlyAccessProgramManager,
        NotificationService notificationService
    ) {
        myApplication = application;
        myWindowsDefenderChecker = windowsDefenderChecker;
        myEarlyAccessProgramManager = earlyAccessProgramManager;
        myNotificationService = notificationService;
    }

    @Override
    public void runActivity(Project project, UIAccess uiAccess) {
        if (!Platform.current().os().isWindows()) {
            return;
        }

        if (!myEarlyAccessProgramManager.get().getState(WindowsDefenderCheckerEarlyAccessDescriptor.class)) {
            return;
        }

        WindowsDefenderChecker checker = myWindowsDefenderChecker.get();
        if (checker.isVirusCheckIgnored(project)) {
            LOG.info("status check is disabled");
            return;
        }

        Boolean protection = checker.isRealTimeProtectionEnabled();
        if (!Boolean.TRUE.equals(protection)) {
            LOG.info("real-time protection: " + protection);
            return;
        }

        String basePath = project.getBasePath();
        Path projectDir = basePath == null ? null : Paths.get(basePath);
        if (projectDir != null && checker.isUntrustworthyLocation(projectDir)) {
            LOG.info("untrustworthy location: " + projectDir);
            return;
        }

        List<Path> paths = checker.filterDevDrivePaths(checker.getPathsToExclude(project));
        if (paths.isEmpty()) {
            LOG.info("all paths are on a DevDrive");
            return;
        }

        WindowsDefenderNotification notification = new WindowsDefenderNotification(
            myNotificationService.newWarn(SystemHealthMonitorImpl.GROUP)
                .content(ExternalServiceLocalize.virusScanningWarnMessage(
                    Application.get().getName(),
                    StringUtil.join(paths, "<br/>")
                ))
                .important(true),
            paths
        );

        notification.setCollapseActionsDirection(Notification.CollapseActionsDirection.KEEP_LEFTMOST);
        checker.configureActions(project, notification);

        myApplication.invokeLater(() -> notification.notify(project));
    }
}
