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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ide.impl.idea.ide.SystemHealthMonitorImpl;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.UIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
@ExtensionImpl
public class WindowsDefenderCheckerActivity implements BackgroundStartupActivity, DumbAware {
    @Nonnull
    private final Application myApplication;
    private final Provider<WindowsDefenderChecker> myWindowsDefenderChecker;
    private final Provider<EarlyAccessProgramManager> myEarlyAccessProgramManager;
    @Nonnull
    private final NotificationService myNotificationService;

    @Inject
    public WindowsDefenderCheckerActivity(
        @Nonnull Application application,
        Provider<WindowsDefenderChecker> windowsDefenderChecker,
        Provider<EarlyAccessProgramManager> earlyAccessProgramManager,
        @Nonnull NotificationService notificationService
    ) {
        myApplication = application;
        myWindowsDefenderChecker = windowsDefenderChecker;
        myEarlyAccessProgramManager = earlyAccessProgramManager;
        myNotificationService = notificationService;
    }

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        Platform platform = Platform.current();
        if (!platform.os().isWindows()) {
            return;
        }

        EarlyAccessProgramManager earlyAccessProgramManager = myEarlyAccessProgramManager.get();
        if (!earlyAccessProgramManager.getState(WindowsDefenderCheckerEarlyAccessDescriptor.class)) {
            return;
        }

        WindowsDefenderChecker windowsDefenderChecker = myWindowsDefenderChecker.get();
        if (windowsDefenderChecker.isVirusCheckIgnored(project)) {
            return;
        }

        WindowsDefenderChecker.CheckResult checkResult = windowsDefenderChecker.checkWindowsDefender(project);

        if (checkResult.status == WindowsDefenderChecker.RealtimeScanningStatus.SCANNING_ENABLED
            && ContainerUtil.any(checkResult.pathStatus, it -> !it.getValue())) {
            List<Path> nonExcludedPaths = checkResult.pathStatus.entrySet().stream()
                .filter(it -> !it.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            WindowsDefenderNotification notification = new WindowsDefenderNotification(
                myNotificationService.newWarn(SystemHealthMonitorImpl.GROUP)
                    .content(ExternalServiceLocalize.virusScanningWarnMessage(
                            Application.get().getName(),
                            StringUtil.join(nonExcludedPaths, "<br/>")
                        )
                    )
                    .important(true),
                nonExcludedPaths
            );

            notification.setCollapseActionsDirection(Notification.CollapseActionsDirection.KEEP_LEFTMOST);
            windowsDefenderChecker.configureActions(project, notification);

            myApplication.invokeLater(() -> notification.notify(project));
        }
    }
}
