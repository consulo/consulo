/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.main;

import com.intellij.ide.StartupProgress;
import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ex.ApplicationEx;
import consulo.container.util.StatCollector;
import consulo.start.CommandLineArgs;
import consulo.util.lang.ref.SimpleReference;
import consulo.web.application.impl.WebApplicationImpl;
import consulo.web.application.impl.WebStartupProgressImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author VISTALL
 * @since 15-May-16
 */
public class WebApplicationStarter extends ApplicationStarter {
  public WebApplicationStarter(@Nonnull CommandLineArgs args, @Nonnull StatCollector stat) {
    super(args, stat);
  }

  @Nullable
  @Override
  public StartupProgress createSplash(CommandLineArgs args) {
    return new WebStartupProgressImpl();
  }

  @Nonnull
  @Override
  protected Application createApplication(boolean isHeadlessMode, SimpleReference<StartupProgress> splashRef, CommandLineArgs args) {
    return new WebApplicationImpl(splashRef);
  }

  @Override
  public void main(StatCollector stat, Runnable appInitalizeMark, ApplicationEx app, boolean newConfigFolder, @Nonnull CommandLineArgs args) {
    StartupProgress startupProgress = mySplashRef.get();
    if (startupProgress != null) {
      startupProgress.dispose();
      mySplashRef.set(null);
    }

    appInitalizeMark.run();

    /*AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
      System.out.println("Save All");

      Application application = ApplicationManager.getApplication();
      if(application == null || application.isDisposed()) {
        return;
      }

      SwingUtilities.invokeLater(() -> application.saveSettings());
    }, 1, 5, TimeUnit.MINUTES); */
  }
}
