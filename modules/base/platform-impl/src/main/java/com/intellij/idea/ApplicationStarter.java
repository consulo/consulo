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

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.ex.ApplicationEx;
import consulo.application.TransactionGuardEx;
import consulo.container.boot.ContainerPathManager;
import consulo.container.util.StatCollector;
import consulo.localize.LocalizeManager;
import consulo.localize.impl.LocalizeManagerImpl;
import consulo.logging.Logger;
import consulo.plugins.internal.PluginsInitializeInfo;
import consulo.plugins.internal.PluginsLoader;
import consulo.start.CommandLineArgs;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ApplicationStarter {
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

  protected final SimpleReference<StartupProgress> mySplashRef = SimpleReference.create();

  protected PluginsInitializeInfo myPluginsInitializeInfo;

  public ApplicationStarter(@Nonnull CommandLineArgs args) {
    LOG.assertTrue(ourInstance == null);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourInstance = this;

    myArgs = args;

    initApplication(false, args);
  }

  @Nonnull
  protected abstract Application createApplication(boolean isHeadlessMode, SimpleReference<StartupProgress> splashRef, CommandLineArgs args);

  protected abstract void main(StatCollector stat, Runnable appInitalizeMark, ApplicationEx app, boolean newConfigFolder, @Nonnull CommandLineArgs args);

  public boolean needStartInTransaction() {
    return false;
  }

  @Nullable
  public abstract StartupProgress createSplash(CommandLineArgs args);

  protected void initApplication(boolean isHeadlessMode, CommandLineArgs args) {
    StartupProgress splash = createSplash(args);
    if (splash != null) {
      mySplashRef.set(splash);
    }

    PluginsLoader.setVersionChecker();

    myPluginsInitializeInfo = PluginsLoader.initPlugins(splash, isHeadlessMode);

    LocalizeManagerImpl localizeManager = (LocalizeManagerImpl)LocalizeManager.getInstance();

    localizeManager.initialize();

    createApplication(isHeadlessMode, mySplashRef, args);
  }

  public void run(StatCollector stat, Runnable appInitalizeMark, boolean newConfigFolder) {
    try {
      ApplicationEx app = (ApplicationEx)Application.get();
      app.load(ContainerPathManager.get().getOptionsPath());

      if (needStartInTransaction()) {
        ((TransactionGuardEx)TransactionGuard.getInstance()).performUserActivity(() -> main(stat, appInitalizeMark, app, newConfigFolder, myArgs));
      }
      else {
        main(stat, appInitalizeMark, app, newConfigFolder, myArgs);
      }

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
