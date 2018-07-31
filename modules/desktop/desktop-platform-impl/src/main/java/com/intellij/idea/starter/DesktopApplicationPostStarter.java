/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.idea.starter;

import com.intellij.ide.*;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.ApplicationStarter;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.impl.DesktopApplicationImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.DesktopWindowManagerImpl;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrameHelper;
import com.intellij.ui.DesktopSplash;
import consulo.application.ApplicationProperties;
import consulo.ide.customize.FirstStartCustomizeUtil;
import consulo.start.CommandLineArgs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class DesktopApplicationPostStarter extends ApplicationPostStarter {
  private static final Logger LOG = Logger.getInstance(DesktopApplicationPostStarter.class);

  @Inject
  private RecentProjectsManager myRecentProjectsManager;
  @Inject
  private WelcomeFrameHelper myWelcomeFrameHelper;
  @Inject
  private WindowManager myWindowManager;

  public DesktopApplicationPostStarter(ApplicationStarter applicationStarter) {
    super(applicationStarter);
  }

  @Nonnull
  @Override
  public Application createApplication(boolean isHeadlessMode, CommandLineArgs args) {
    if (!args.isNoSplash()) {
      final SplashScreen splashScreen = getSplashScreen();
      if (splashScreen == null) {
        DesktopSplash splash = new DesktopSplash(false);
        mySplashRef.set(splash);
        splash.show();
      }
    }

    return new DesktopApplicationImpl(isHeadlessMode, mySplashRef);
  }

  @Nullable
  private SplashScreen getSplashScreen() {
    try {
      return SplashScreen.getSplashScreen();
    }
    catch (Throwable t) {
      LOG.warn(t);
      return null;
    }
  }

  @Override
  public void main(Application application, boolean newConfigFolder, @Nonnull CommandLineArgs args) {
    SystemDock.updateMenu();

    // Event queue should not be changed during initialization of application components.
    // It also cannot be changed before initialization of application components because IdeEventQueue uses other
    // application components. So it is proper to perform replacement only here.
    ApplicationEx app = (ApplicationEx)application;

    DesktopWindowManagerImpl windowManager = (DesktopWindowManagerImpl)myWindowManager;

    IdeEventQueue.getInstance().setWindowManager(windowManager);

    RecentProjectsManagerBase recentProjectsManager = (RecentProjectsManagerBase)myRecentProjectsManager;

    LOG.info("application init took " + (System.nanoTime() - PluginManager.startupStart) / 1000000 + " ms");
    PluginManagerCore.dumpPluginClassStatistics();

    app.invokeAndWait(() -> {
      StartupProgress desktopSplash = mySplashRef.get();
      if (desktopSplash != null) {
        desktopSplash.dispose();
        mySplashRef.set(null);  // Allow GC collect the splash window
      }
    }, ModalityState.NON_MODAL);

    if (newConfigFolder && !ApplicationProperties.isInSandbox()) {
      FirstStartCustomizeUtil.show(true);
    }

    boolean willOpenProject = recentProjectsManager.willReopenProjectOnStart() && !args.isNoRecentProjects();

    AppLifecycleListener lifecyclePublisher = app.getMessageBus().syncPublisher(AppLifecycleListener.TOPIC);
    lifecyclePublisher.appFrameCreated(args, willOpenProject);

    if (recentProjectsManager.willReopenProjectOnStart() && !args.isNoRecentProjects()) {
      windowManager.showFrame();
    }
    else {
      myWelcomeFrameHelper.showNow();
    }

    app.invokeLater(() -> {
      if (!args.isNoRecentProjects()) {
        Project projectFromCommandLine = null;
        if (myApplicationStarter.isPerformProjectLoad()) {
          projectFromCommandLine = CommandLineProcessor.processExternalCommandLine(args, null);
        }

        if (projectFromCommandLine == null) {
          recentProjectsManager.doReopenLastProject();
        }
      }

      SwingUtilities.invokeLater(PluginManager::reportPluginError);

      UsageTrigger.trigger("consulo.app.started");
    }, ModalityState.NON_MODAL);
  }
}
