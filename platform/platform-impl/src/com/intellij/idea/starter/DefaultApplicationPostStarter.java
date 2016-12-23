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

import com.intellij.ide.CommandLineProcessor;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.ApplicationStarter;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.ui.Splash;
import consulo.annotations.Internal;
import consulo.ide.customize.FirstStartCustomizeUtil;
import consulo.start.CommandLineArgs;
import consulo.util.SandboxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Used via reflection
 * @see com.intellij.idea.ApplicationStarter#getStarterClass(boolean, boolean)
 */
@SuppressWarnings("unused")
@Internal
public class DefaultApplicationPostStarter extends ApplicationPostStarter {
  private static final Logger LOG = Logger.getInstance(DefaultApplicationPostStarter.class);

  private ApplicationStarter myApplicationStarter;
  private Splash mySplash;

  public DefaultApplicationPostStarter(ApplicationStarter applicationStarter) {
    myApplicationStarter = applicationStarter;
  }

  @Override
  public void createApplication(boolean internal, boolean isUnitTestMode, boolean isHeadlessMode, boolean isCommandline, CommandLineArgs args) {
    if (!args.isNoSplash()) {
      final SplashScreen splashScreen = getSplashScreen();
      if (splashScreen == null) {
        mySplash = new Splash();
        mySplash.show();
      }
    }

    new ApplicationImpl(internal, isUnitTestMode, isHeadlessMode, isCommandline, IDEA_APPLICATION, mySplash);
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
  public void main(@NotNull CommandLineArgs args) {
    SystemDock.updateMenu();

    // Event queue should not be changed during initialization of application components.
    // It also cannot be changed before initialization of application components because IdeEventQueue uses other
    // application components. So it is proper to perform replacement only here.
    final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
    WindowManagerImpl windowManager = (WindowManagerImpl)WindowManager.getInstance();
    IdeEventQueue.getInstance().setWindowManager(windowManager);

    RecentProjectsManagerBase recentProjectsManager = RecentProjectsManagerBase.getInstanceEx();

    LOG.info("App initialization took " + (System.nanoTime() - PluginManager.startupStart) / 1000000 + " ms");
    PluginManagerCore.dumpPluginClassStatistics();

    app.invokeAndWait(() -> {
      if (mySplash != null) {
        mySplash.dispose();
        mySplash = null; // Allow GC collect the splash window
      }
    }, ModalityState.NON_MODAL);

    if (Registry.is("ide.firstStartup") && !SandboxUtil.isInsideSandbox()) {
      FirstStartCustomizeUtil.show(true);
    }

    if (recentProjectsManager.getLastProjectPath() != null) {
      WelcomeFrame.showNow();
    }
    else {
      windowManager.showFrame();
    }

    app.invokeLater(() -> {
      Project projectFromCommandLine = null;
      if (myApplicationStarter.isPerformProjectLoad()) {
        projectFromCommandLine = CommandLineProcessor.processExternalCommandLine(args, null);
      }

      if(projectFromCommandLine == null) {
        recentProjectsManager.doReopenLastProject();
      }

      SwingUtilities.invokeLater(PluginManager::reportPluginError);

      //safe for headless and unit test modes
      UsageTrigger.trigger(app.getName() + "app.started");
    }, ModalityState.NON_MODAL);
  }
}
