/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import consulo.annotations.RequiredDispatchThread;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class CheckForUpdateAction extends AnAction implements DumbAware {
  private final UpdateSettings myUpdateSettings;

  @Inject
  public CheckForUpdateAction(UpdateSettings updateSettings) {
    myUpdateSettings = updateSettings;
  }

  @RequiredDispatchThread
  @Override
  public void update(@Nonnull AnActionEvent e) {
    String place = e.getPlace();
    if (ActionPlaces.WELCOME_SCREEN.equals(place)) {
      e.getPresentation().setEnabledAndVisible(true);
    }
    else {
      e.getPresentation().setVisible(!SystemInfo.isMacSystemMenu || !ActionPlaces.MAIN_MENU.equals(place));
    }
  }

  @Override
  @RequiredDispatchThread
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);

    actionPerformed(project, myUpdateSettings);
  }

  public static void actionPerformed(Project project, final UpdateSettings updateSettings) {
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Checking for updates", true) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);

        PlatformOrPluginUpdateChecker.checkAndNotifyForUpdates(project, true, indicator).doWhenDone(() -> updateSettings.setLastTimeCheck(System.currentTimeMillis()));
      }
    });
  }
}
