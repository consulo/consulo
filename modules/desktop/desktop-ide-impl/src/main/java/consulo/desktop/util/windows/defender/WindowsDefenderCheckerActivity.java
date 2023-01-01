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
import consulo.ide.impl.idea.diagnostic.DiagnosticBundle;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
@ExtensionImpl
public class WindowsDefenderCheckerActivity implements BackgroundStartupActivity, DumbAware {
  private final Application myApplication;
  private final Provider<WindowsDefenderChecker> myWindowsDefenderChecker;

  @Inject
  public WindowsDefenderCheckerActivity(Application application, Provider<WindowsDefenderChecker> windowsDefenderChecker) {
    myApplication = application;
    myWindowsDefenderChecker = windowsDefenderChecker;
  }

  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    if (!Boolean.getBoolean("consulo.windows.defender.activity")) {
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

      WindowsDefenderNotification notification =
              new WindowsDefenderNotification(DiagnosticBundle.message("virus.scanning.warn.message", Application.get().getName(), StringUtil.join(nonExcludedPaths, "<br/>")), nonExcludedPaths);

      notification.setImportant(true);
      notification.setCollapseActionsDirection(Notification.CollapseActionsDirection.KEEP_LEFTMOST);
      windowsDefenderChecker.configureActions(project, notification);

      myApplication.invokeLater(() -> notification.notify(project));
    }
  }
}
