/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.ide.StartupProgress;
import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.util.Ref;
import consulo.container.util.StatCollector;
import consulo.localize.LocalizeManager;
import consulo.localize.impl.LocalizeManagerImpl;
import consulo.plugins.internal.PluginsInitializeInfo;
import consulo.plugins.internal.PluginsLoader;
import consulo.start.CommandLineArgs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ApplicationPostStarter {
  protected final Ref<StartupProgress> mySplashRef = Ref.create();
  protected ApplicationStarter myApplicationStarter;

  protected PluginsInitializeInfo myPluginsInitializeInfo;

  public ApplicationPostStarter(ApplicationStarter applicationStarter) {
    myApplicationStarter = applicationStarter;
  }

  @Nullable
  public abstract StartupProgress createSplash(CommandLineArgs args);

  public void initApplication(boolean isHeadlessMode, CommandLineArgs args) {
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

  @Nonnull
  protected abstract Application createApplication(boolean isHeadlessMode, Ref<StartupProgress> splashRef, CommandLineArgs args);

  public void main(StatCollector stat, Runnable appInitalizeMark, ApplicationEx app, boolean newConfigFolder, @Nonnull CommandLineArgs args) {
  }

  public boolean needStartInTransaction() {
    return true;
  }
}
