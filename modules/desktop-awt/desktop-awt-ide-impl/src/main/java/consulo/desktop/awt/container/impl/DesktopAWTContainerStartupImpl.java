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
package consulo.desktop.awt.container.impl;

import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.StartupAbortedException;
import consulo.application.impl.internal.start.StartupUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.bootstrap.concurrent.IdeaForkJoinWorkerThreadFactory;
import consulo.container.boot.ContainerPathManager;
import consulo.container.boot.ContainerStartup;
import consulo.container.internal.ShowError;
import consulo.container.util.StatCollector;
import consulo.desktop.awt.startup.DesktopApplicationStarter;
import consulo.desktop.container.impl.DesktopContainerPathManager;
import consulo.desktop.startup.DesktopImportantFolderLocker;
import consulo.ide.impl.idea.ide.startup.StartupActionScriptManager;
import consulo.logging.Logger;
import consulo.virtualFileSystem.impl.internal.mediator.FileSystemMediatorOverride;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

/**
 * @author VISTALL
 * @see consulo.desktop.awt.boot.main.Main
 * @since 2019-07-15
 */
@SuppressWarnings("unused")
public class DesktopAWTContainerStartupImpl implements ContainerStartup {
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

    Runnable runnable = () -> {
      try {
        start(stat, appInitializeMark, args);
      }
      catch (Throwable t) {
        throw new StartupAbortedException(t);
      }
    };

    new Thread(runnable, "Consulo Main Thread").start();
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
    ApplicationStarter.installExceptionHandler(() -> Logger.getInstance(DesktopAWTContainerStartupImpl.class));

    try {
      StartupActionScriptManager.executeActionScript();
    }
    catch (IOException e) {
      Logger.getInstance(DesktopAWTContainerStartupImpl.class).error(e);

      ShowError.showErrorDialog("Plugin Installation Error", e.getMessage(), e);
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
