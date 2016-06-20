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
import com.intellij.idea.starter.ApplicationStarter;
import com.intellij.idea.starter.DefaultApplicationStarter;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.X11UiUtil;
import com.intellij.ui.Splash;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.application.ApplicationProperties;

import javax.swing.*;

public class IdeaApplication {
  private static final Logger LOG = Logger.getInstance(IdeaApplication.class);

  private static IdeaApplication ourInstance;
  public volatile static boolean ourLoaded;

  public static IdeaApplication getInstance() {
    return ourInstance;
  }

  public static boolean isLoaded() {
    return ourLoaded;
  }

  private final String[] myArgs;
  private boolean myPerformProjectLoad = true;
  private ApplicationStarter myStarter;

  public IdeaApplication(String[] args) {
    LOG.assertTrue(ourInstance == null);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourInstance = this;

    myArgs = args;
    boolean isInternal = Boolean.getBoolean(ApplicationProperties.IDEA_IS_INTERNAL);
    boolean isUnitTest = Boolean.getBoolean(ApplicationProperties.CONSULO_IN_UNIT_TEST);

    boolean headless = Main.isHeadless();

    patchSystem(headless);

    myStarter = createStarter(isUnitTest);

    Splash splash = myStarter.createSplash(myArgs);

    ApplicationManagerEx.createApplication(isInternal, isUnitTest, headless, isUnitTest, ApplicationManagerEx.IDEA_APPLICATION, splash);

    myStarter.premain(args);
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
  private ApplicationStarter createStarter(boolean isUnitTest) {
    if(isUnitTest) {
      // this class exists in another module. We can't move it to this module, due it will provide junit dependency
      Class unityTestStarter = ReflectionUtil.forName("com.intellij.idea.starter.UnitTestStarter");
      return (ApplicationStarter)ReflectionUtil.newInstance(unityTestStarter);
    }
    return new DefaultApplicationStarter(this);
  }

  public void run() {
    try {
      ApplicationEx app = ApplicationManagerEx.getApplicationEx();
      app.load(PathManager.getOptionsPath());

      ((TransactionGuardImpl) TransactionGuard.getInstance()).performUserActivity(new Runnable() {
        @Override
        public void run() {
          myStarter.main(myArgs);
        }
      });

      myStarter = null; //GC it

      ourLoaded = true;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public String[] getCommandLineArguments() {
    return myArgs;
  }

  public boolean isPerformProjectLoad() {
    return myPerformProjectLoad;
  }

  public void setPerformProjectLoad(boolean performProjectLoad) {
    myPerformProjectLoad = performProjectLoad;
  }
}
