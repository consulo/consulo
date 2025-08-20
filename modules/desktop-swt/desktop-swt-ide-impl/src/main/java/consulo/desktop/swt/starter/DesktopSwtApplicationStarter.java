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
package consulo.desktop.swt.starter;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.application.internal.StartupProgress;
import consulo.application.internal.ApplicationEx;
import consulo.component.internal.ComponentBinding;
import consulo.container.util.StatCollector;
import consulo.desktop.swt.application.impl.DesktopSwtApplicationImpl;
import consulo.logging.Logger;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.util.lang.ref.SimpleReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtApplicationStarter extends ApplicationStarter {
  private static final Logger LOG = Logger.getInstance(DesktopSwtApplicationStarter.class);

  public DesktopSwtApplicationStarter(CommandLineArgs commandLineArgs, @Nonnull StatCollector stat) {
    super(commandLineArgs, stat);
  }

  @Nullable
  @Override
  public StartupProgress createSplash(CommandLineArgs args) {
    return null;
  }

  @Nonnull
  @Override
  protected Application createApplication(ComponentBinding componentBinding, boolean isHeadlessMode, SimpleReference<StartupProgress> splashRef, CommandLineArgs args) {
    return new DesktopSwtApplicationImpl(componentBinding, splashRef);
  }

  @Override
  protected void initializeEnviroment(boolean isHeadlessMode, CommandLineArgs args, StatCollector stat) {
    System.setProperty("swt.autoScale", "exact");

    super.initializeEnviroment(isHeadlessMode, args, stat);
  }

  @Override
  protected void main(StatCollector stat,
                      Runnable appInitializeMark,
                      ApplicationEx app,
                      boolean newConfigFolder,
                      @Nonnull CommandLineArgs args) {
    appInitializeMark.run();

    stat.dump("Startup statistics", LOG::info);

    //SwingUtilities.invokeLater(() -> {
    //  StartupProgress desktopSplash = mySplashRef.get();
    //  if (desktopSplash != null) {
    //    desktopSplash.dispose();
    //    mySplashRef.set(null);
    //  }
    //});

    app.invokeLater(() -> WelcomeFrameManager.getInstance().showFrame(), IdeaModalityState.any());
  }
}
