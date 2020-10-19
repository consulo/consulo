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

import com.intellij.notification.Notification;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import jakarta.inject.Provider;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
public class WindowsDefenderCheckerActivity implements StartupActivity.Background {
  private final Application myApplication;
  private final Provider<WindowsDefenderChecker> myWindowsDefenderChecker;

  @Inject
  public WindowsDefenderCheckerActivity(Application application, Provider<WindowsDefenderChecker> windowsDefenderChecker) {
    myApplication = application;
    myWindowsDefenderChecker = windowsDefenderChecker;
  }

  @Override
  public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    if(!Boolean.getBoolean("consulo.windows.defender.activity")) {
      return;
    }
    
    if (myApplication.isUnitTestMode()) {
      return;
    }

    WindowsDefenderChecker windowsDefenderChecker = myWindowsDefenderChecker.get();
    if (windowsDefenderChecker.isVirusCheckIgnored(project)) {
      return;
    }

    WindowsDefenderChecker.CheckResult checkResult = windowsDefenderChecker.checkWindowsDefender(project);

    if (checkResult.status == WindowsDefenderChecker.RealtimeScanningStatus.SCANNING_ENABLED && ContainerUtil.any(checkResult.pathStatus, (it) -> !it.getValue())) {
      List<Path> nonExcludedPaths = checkResult.pathStatus.entrySet().stream().filter(it -> !it.getValue()).map(Map.Entry::getKey).collect(Collectors.toList());

      WindowsDefenderNotification notification = new WindowsDefenderNotification(
              DiagnosticBundle.message("virus.scanning.warn.message", ApplicationNamesInfo.getInstance().getFullProductName(), StringUtil.join(nonExcludedPaths, "<br/>")), nonExcludedPaths);

      notification.setImportant(true);
      notification.setCollapseActionsDirection(Notification.CollapseActionsDirection.KEEP_LEFTMOST);
      windowsDefenderChecker.configureActions(project, notification);

      myApplication.invokeLater(() -> notification.notify(project));
    }
  }
}
