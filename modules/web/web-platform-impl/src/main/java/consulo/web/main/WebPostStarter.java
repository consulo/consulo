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
import com.intellij.idea.starter.ApplicationPostStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Ref;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.annotations.Internal;
import consulo.start.CommandLineArgs;
import consulo.web.application.impl.WebApplicationImpl;
import consulo.web.application.impl.WebStartupProgressImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

/**
 * Used via reflection
 *
 * @author VISTALL
 * @see com.intellij.idea.ApplicationStarter#getStarterClass(boolean, boolean)
 * @since 15-May-16
 */
@SuppressWarnings("unused")
@Internal
public class WebPostStarter extends ApplicationPostStarter {
  public WebPostStarter(ApplicationStarter applicationStarter) {
    super(applicationStarter);
  }

  @Override
  public void createApplication(boolean internal, boolean isUnitTestMode, boolean isHeadlessMode, boolean isCommandline, CommandLineArgs args) {
    mySplashRef.set(new WebStartupProgressImpl());

    new WebApplicationImpl(internal, isUnitTestMode, isHeadlessMode, isCommandline, Ref.create());
  }

  @Override
  public void main(boolean newConfigFolder, @NotNull CommandLineArgs args) {
    StartupProgress startupProgress = mySplashRef.get();
    if (startupProgress != null) {
      startupProgress.dispose();
      mySplashRef.set(null);
    }

    AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
      System.out.println("Save All");

      Application application = ApplicationManager.getApplication();
      if(application == null || application.isDisposed()) {
        return;
      }

      SwingUtilities.invokeLater(() -> application.saveSettings());
    }, 1, 5, TimeUnit.MINUTES);
  }

  @Override
  public boolean needStartInTransaction() {
    return false;
  }
}
