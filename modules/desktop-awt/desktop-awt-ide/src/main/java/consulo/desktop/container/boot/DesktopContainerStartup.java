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

import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.idea.ApplicationStarter;
import com.intellij.idea.DesktopImportantFolderLocker;
import com.intellij.idea.StartupUtil;
import com.intellij.idea.starter.DesktopApplicationStarter;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.bootstrap.concurrent.IdeaForkJoinWorkerThreadFactory;
import consulo.container.boot.ContainerPathManager;
import consulo.container.boot.ContainerStartup;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.vfs.impl.mediator.FileSystemMediatorOverride;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * @author VISTALL
 * @see consulo.desktop.boot.main.Main
 * @since 2019-07-15
 */
@SuppressWarnings("unused")
public class DesktopContainerStartup implements ContainerStartup {
  @Override
  public void run(@Nonnull Map<String, Object> map) {
    StatCollector stat = (StatCollector)map.get(ContainerStartup.STAT_COLLECTOR);
    String[] args = (String[])map.get(ContainerStartup.ARGS);

    Runnable appInitializeMark = stat.mark(StatCollector.APP_INITIALIZE);

    IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool();

    DesktopContainerPathManager pathManager = (DesktopContainerPathManager)ContainerPathManager.get();

    pathManager.loadProperties();

    StartupUtil.initializeLogger();

    stat.markWith("fs.mediator.replace", () -> {
      //noinspection Convert2MethodRef
      FileSystemMediatorOverride.replaceIfNeedMediator();
    });

    stat.markWith("boot.hack.awt", () -> {
      DesktopStartUIUtil.hackAWT();
      DesktopStartUIUtil.initDefaultLAF();
      DesktopStartUIUtil.initSystemFontData();
    });

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
        throw new PluginManager.StartupAbortedException(t);
      }
    };

    new Thread(threadGroup, runnable, "Consulo Main Thread").start();
  }

  @Nonnull
  @Override
  public ContainerPathManager createPathManager(@Nonnull Map<String, Object> args) {
    return new DesktopContainerPathManager();
  }

  @Override
  public void destroy() {
    // unused
  }

  private static void start(StatCollector stat, Runnable appInitalizeMark, String[] args) {
    try {
      StartupActionScriptManager.executeActionScript();
    }
    catch (IOException e) {
      Logger.getInstance(DesktopContainerStartup.class).error(e);

      StartupUtil.showMessage("Plugin Installation Error", e);
      return;
    }

    // desktop locker use netty. it will throw error due log4j2 initialize
    // see io.netty.channel.MultithreadEventLoopGroup.logger
    // it will override logger, which is active only if app exists
    // FIXME [VISTALL] see feac1737-76bf-4952-b770-d3f8d1978e59
    // InternalLoggerFactory.setDefaultFactory(ApplicationInternalLoggerFactory.INSTANCE);

    StartupUtil.prepareAndStart(args, stat, DesktopImportantFolderLocker::new, (newConfigFolder, commandLineArgs) -> {
      ApplicationStarter app = new DesktopApplicationStarter(commandLineArgs, stat);

      AppExecutorUtil.getAppExecutorService().execute(() -> app.run(stat, appInitalizeMark, newConfigFolder));
    });
  }
}
