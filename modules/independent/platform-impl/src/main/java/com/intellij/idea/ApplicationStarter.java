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
package com.intellij.idea;

import com.intellij.Patches;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.IdeRepaintManager;
import com.intellij.idea.starter.ApplicationPostStarter;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.X11UiUtil;
import com.intellij.util.ReflectionUtil;
import consulo.application.ApplicationProperties;
import consulo.start.CommandLineArgs;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.Constructor;

public class ApplicationStarter {
  private static final Logger LOG = Logger.getInstance(ApplicationStarter.class);

  private static ApplicationStarter ourInstance;
  public volatile static boolean ourLoaded;

  public static ApplicationStarter getInstance() {
    return ourInstance;
  }

  public static boolean isLoaded() {
    return ourLoaded;
  }

  private final CommandLineArgs myArgs;
  private boolean myPerformProjectLoad = true;
  private ApplicationPostStarter myPostStarter;

  public ApplicationStarter(CommandLineArgs args) {
    LOG.assertTrue(ourInstance == null);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourInstance = this;

    myArgs = args;
    boolean isInternal = Boolean.getBoolean(ApplicationProperties.IDEA_IS_INTERNAL);
    boolean isUnitTest = Boolean.getBoolean(ApplicationProperties.CONSULO_IN_UNIT_TEST);
    boolean asWebApp = Boolean.getBoolean(ApplicationProperties.CONSULO_AS_WEB_APP);

    boolean headless = Main.isHeadless();

    patchSystem(headless);

    myPostStarter = createPostStarter(isUnitTest, asWebApp);
    myPostStarter.createApplication(isInternal, isUnitTest, headless, isUnitTest, args);
    myPostStarter.premain(args);
  }

  private static void patchSystem(boolean headless) {
    System.setProperty("sun.awt.noerasebackground", "true");

    IdeEventQueue.getInstance(); // replace system event queue

    if (headless) return;

    if (Patches.SUN_BUG_ID_6209673) {
      RepaintManager.setCurrentManager(new IdeRepaintManager());
    }

    if (SystemInfo.isXWindow) {
      String wmName = X11UiUtil.getWmName();
      LOG.info("WM detected: " + wmName);
      if (wmName != null) {
        X11UiUtil.patchDetectedWm(wmName);
      }
    }

    IconLoader.activate();

    new JFrame().pack(); // this peer will prevent shutting down our application
  }

  @NotNull
  private ApplicationPostStarter createPostStarter(boolean isUnitTest, boolean asWebApp) {
    Class<?> starterClass = ReflectionUtil.forName(getStarterClass(isUnitTest, asWebApp));

    try {
      Constructor constructor = starterClass.getConstructor(ApplicationStarter.class);
      constructor.setAccessible(true);
      return (ApplicationPostStarter)ReflectionUtil.createInstance(constructor, this);
    }
    catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }

  @NotNull
  private static String getStarterClass(boolean isUnitTest, boolean asWebApp) {
    if (isUnitTest) {
      return "consulo.start.UnitTestPostStarter";
    }

    return asWebApp ? "consulo.web.main.WebPostStarter" : "com.intellij.idea.starter.DesktopApplicationPostStarter";
  }

  public void run(boolean newConfigFolder) {
    try {
      ApplicationEx app = ApplicationManagerEx.getApplicationEx();
      app.load(PathManager.getOptionsPath());

      if (myPostStarter.needStartInTransaction()) {
        ((TransactionGuardImpl)TransactionGuard.getInstance()).performUserActivity(() -> myPostStarter.main(newConfigFolder, myArgs));
      }
      else {
        myPostStarter.main(newConfigFolder, myArgs);
      }

      myPostStarter = null;

      ourLoaded = true;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isPerformProjectLoad() {
    return myPerformProjectLoad;
  }

  public void setPerformProjectLoad(boolean performProjectLoad) {
    myPerformProjectLoad = performProjectLoad;
  }
}
