/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui.win;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.ReopenProjectAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.Application;
import consulo.application.ApplicationProperties;
import consulo.container.boot.ContainerPathManager;
import consulo.platform.Platform;
import consulo.wm.impl.DesktopSystemDockImpl;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Denis Fokin
 */
public class WinDockDelegate implements DesktopSystemDockImpl.Delegate {
  private ExecutorService myExecutorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "Windows JumpList Updater"));
  
  private final Application myApplication;

  public WinDockDelegate(Application application) {
    myApplication = application;
  }

  @Override
  public void updateRecentProjectsMenu() {
    if (ApplicationProperties.isInSandbox()) {
      return;
    }

    // we need invoke it in own thread, due we don't want it call inside UI thread, or Write thread (if it separate)
    myExecutorService.execute(() -> {
      RecentTasks.clear(myApplication);

      final AnAction[] recentProjectActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);

      if (recentProjectActions.length == 0) {
        return;
      }

      String executable = Platform.current().mapWindowsExecutable(myApplication.getName().toLowerCase().get(), "exe");
      File exePath = new File(ContainerPathManager.get().getAppHomeDirectory(), executable);
      if (!exePath.exists()) {
        throw new IllegalArgumentException("Executable is not exists. Path: " + exePath.getPath());
      }

      String launcher = RecentTasks.getShortenPath(myApplication, exePath.getPath());
      Task[] tasks = new Task[recentProjectActions.length];
      for (int i = 0; i < recentProjectActions.length; i++) {
        ReopenProjectAction rpa = (ReopenProjectAction)recentProjectActions[i];
        tasks[i] = new Task(launcher, RecentTasks.getShortenPath(myApplication, rpa.getProjectPath()), rpa.getTemplatePresentation().getText());
      }

      RecentTasks.addTasks(myApplication, "Recent", tasks);
    });
  }

  @Override
  public void dispose() {
    myExecutorService.shutdown();
  }
}
