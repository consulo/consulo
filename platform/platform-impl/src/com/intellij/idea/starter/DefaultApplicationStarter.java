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

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.CommandLineProcessor;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.customize.CustomizeUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.IdeaApplication;
import com.intellij.idea.StartupUtil;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.openapi.wm.impl.WindowManagerImpl;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.ui.Splash;
import com.intellij.util.SandboxUtil;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class DefaultApplicationStarter extends ApplicationStarter {
  private static final Logger LOG = Logger.getInstance(DefaultApplicationStarter.class);

  private IdeaApplication myIdeaApplication;
  private Splash mySplash;

  public DefaultApplicationStarter(IdeaApplication ideaApplication) {
    myIdeaApplication = ideaApplication;
  }

  @Override
  @Nullable
  protected Splash createSplash(@NotNull String[] args) {
    if (StartupUtil.shouldShowSplash(args)) {
      final ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();
      final SplashScreen splashScreen = getSplashScreen();
      if (splashScreen == null) {
        mySplash = new Splash(appInfo);
        mySplash.show();
        return mySplash;
      }
    }
    return null;
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
  public void main(String[] args) {
    SystemDock.updateMenu();

    // Event queue should not be changed during initialization of application components.
    // It also cannot be changed before initialization of application components because IdeEventQueue uses other
    // application components. So it is proper to perform replacement only here.
    final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
    WindowManagerImpl windowManager = (WindowManagerImpl)WindowManager.getInstance();
    IdeEventQueue.getInstance().setWindowManager(windowManager);

    Ref<Boolean> willOpenProject = new Ref<Boolean>(Boolean.FALSE);
    AppLifecycleListener lifecyclePublisher = app.getMessageBus().syncPublisher(AppLifecycleListener.TOPIC);
    lifecyclePublisher.appFrameCreated(args, willOpenProject);

    LOG.info("App initialization took " + (System.nanoTime() - PluginManager.startupStart) / 1000000 + " ms");
    PluginManagerCore.dumpPluginClassStatistics();

    app.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        if (mySplash != null) {
          mySplash.dispose();
          mySplash = null; // Allow GC collect the splash window
        }
      }
    }, ModalityState.NON_MODAL);

    if (Registry.is("ide.firstStartup") && !SandboxUtil.isInsideSandbox()) {
      CustomizeUtil.show(true);
    }

    if (!willOpenProject.get()) {
      WelcomeFrame.showNow();
      lifecyclePublisher.welcomeScreenDisplayed();
    }
    else {
      windowManager.showFrame();
    }

    app.invokeLater(new Runnable() {
      @Override
      public void run() {
        Project projectFromCommandLine = null;
        if (myIdeaApplication.isPerformProjectLoad()) {
          projectFromCommandLine = loadProjectFromExternalCommandLine();
        }

        final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
        bus.syncPublisher(AppLifecycleListener.TOPIC).appStarting(projectFromCommandLine);

        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            PluginManager.reportPluginError();
          }
        });

        //safe for headless and unit test modes
        UsageTrigger.trigger(app.getName() + "app.started");
      }
    }, ModalityState.NON_MODAL);
  }

  private Project loadProjectFromExternalCommandLine() {
    String[] args = myIdeaApplication.getCommandLineArguments();
    Project project = null;
    if (args != null && args.length > 0 && args[0] != null) {
      LOG.info("DefaultApplicationStarter.loadProjectFromExternalCommandLine");
      project = CommandLineProcessor.processExternalCommandLine(Arrays.asList(args), null);
    }
    return project;
  }
}
