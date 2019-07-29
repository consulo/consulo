/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.container.boot;

import com.intellij.concurrency.IdeaForkJoinWorkerThreadFactory;
import com.intellij.ide.ClassUtilCore;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.DesktopImportantFolderLocker;
import com.intellij.idea.StartupUtil;
import com.intellij.idea.starter.DesktopApplicationPostStarter;
import com.intellij.idea.starter.DesktopApplicationStarter;
import com.intellij.openapi.application.PathManager;
import com.intellij.util.ui.UIUtil;
import consulo.container.boot.ContainerStartup;
import consulo.startup.StartupActionLogger;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.IOException;

/**
 * @author VISTALL
 * @see consulo.desktop.boot.main.Main
 * @since 2019-07-15
 */
@SuppressWarnings("unused")
public class DesktopContainerStartup implements ContainerStartup {
  public static long startupStart;

  @Override
  public void run(@Nonnull String[] args) {
    startupStart = System.nanoTime();

    IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool();

    PathManager.loadProperties();

    UIUtil.hackAWT();
    UIUtil.initDefaultLAF();

    ThreadGroup threadGroup = new ThreadGroup("Consulo Thread Group") {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        PluginManager.processException(e);
      }
    };

    Runnable runnable = () -> {
      try {
        ClassUtilCore.clearJarURLCache();

        start(args);
      }
      catch (Throwable t) {
        throw new PluginManager.StartupAbortedException(t);
      }
    };

    new Thread(threadGroup, runnable, "Consulo Main Thread").start();
  }

  private static void start(String[] args) {
    StartupActionLogger logger = null;
    try {
      logger = new StartupActionLogger();

      try {
        StartupActionScriptManager.executeActionScript(logger);
      }
      catch (IOException e) {
        logger.error(e);

        StartupUtil.showMessage("Plugin Installation Error", e);
        return;
      }
    }
    finally {
      if (logger != null) {
        try {
          logger.close();
        }
        catch (IOException ignored) {
        }
      }
    }

    StartupUtil.prepareAndStart(args, DesktopImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
      ApplicationStarter app = new DesktopApplicationStarter(DesktopApplicationPostStarter.class, commandLineArgs);

      SwingUtilities.invokeLater(() -> {
        PluginManager.installExceptionHandler();
        app.run(newConfigFolder);
      });
    });
  }
}
