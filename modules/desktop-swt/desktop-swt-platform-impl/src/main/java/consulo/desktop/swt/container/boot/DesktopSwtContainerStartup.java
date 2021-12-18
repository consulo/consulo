/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.container.boot;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.StartupUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.bootstrap.concurrent.IdeaForkJoinWorkerThreadFactory;
import consulo.container.boot.ContainerPathManager;
import consulo.container.boot.ContainerStartup;
import consulo.container.util.StatCollector;
import consulo.desktop.swt.starter.DesktopSwtApplicationStarter;
import consulo.desktop.swt.starter.DesktopSwtImportantFolderLocker;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public class DesktopSwtContainerStartup implements ContainerStartup {
  @Nonnull
  @Override
  public ContainerPathManager createPathManager(@Nonnull Map<String, Object> args) {
    return new DesktopSwtContainerPathManager();
  }

  @Override
  public void run(@Nonnull Map<String, Object> map) {
    StatCollector stat = (StatCollector)map.get(ContainerStartup.STAT_COLLECTOR);
    String[] args = (String[])map.get(ContainerStartup.ARGS);

    IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool();

    StartupUtil.initializeLogger();

    Runnable appInitializeMark = stat.mark(StatCollector.APP_INITIALIZE);

    ThreadGroup threadGroup = new ThreadGroup("Consulo Thread Group") {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        PluginManager.processException(e);
      }
    };

    Runnable runnable = () -> {
      try {
        PluginManager.installExceptionHandler();

        start(stat, appInitializeMark, args);
      }
      catch (Throwable t) {
        t.printStackTrace();
        throw new PluginManager.StartupAbortedException(t);
      }
    };

    new Thread(threadGroup, runnable, "Consulo Main Thread").start();
  }

  @Override
  public void destroy() {

  }

  private static void start(StatCollector stat, Runnable appInitalizeMark, String[] args) {
    try {
      StartupActionScriptManager.executeActionScript();
    }
    catch (IOException e) {
      Logger.getInstance(DesktopSwtContainerStartup.class).error(e);

      StartupUtil.showMessage("Plugin Installation Error", e);
      return;
    }

    StartupUtil.prepareAndStart(args, stat, DesktopSwtImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
      ApplicationStarter app = new DesktopSwtApplicationStarter(commandLineArgs, stat);

      AppExecutorUtil.getAppExecutorService().execute(() -> app.run(stat, appInitalizeMark, newConfigFolder));
    });
  }
}
