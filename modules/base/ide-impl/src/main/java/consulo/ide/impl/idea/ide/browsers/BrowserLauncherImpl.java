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
package consulo.ide.impl.idea.ide.browsers;

import consulo.annotation.component.ServiceImpl;
import consulo.application.internal.JobScheduler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.local.ExecUtil;
import consulo.ide.IdeBundle;
import consulo.application.ApplicationManager;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.project.ui.util.AppUIUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.DefaultBrowserPolicy;
import consulo.webBrowser.WebBrowser;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceImpl
public final class BrowserLauncherImpl extends BrowserLauncherAppless {
  @Override
  @Nullable
  protected WebBrowser getEffectiveBrowser(@Nullable WebBrowser browser) {
    WebBrowser effectiveBrowser = browser;

    if (browser == null) {
      // https://youtrack.jetbrains.com/issue/WEB-26547
      WebBrowserManager browserManager = WebBrowserManager.getInstance();
      if (browserManager.getDefaultBrowserPolicy() == DefaultBrowserPolicy.FIRST) {
        effectiveBrowser = browserManager.getFirstActiveBrowser();
      }
    }
    return effectiveBrowser;
  }

  @Override
  protected void doShowError(@Nullable final String error, @Nullable final WebBrowser browser, @Nullable final Project project, final String title, @Nullable final Runnable launchTask) {
    AppUIUtil.invokeOnEdt(new Runnable() {
      @Override
      public void run() {
        if (Messages.showYesNoDialog(project, StringUtil.notNullize(error, "Unknown error"), title == null ? IdeBundle.message("browser" + ".error") : title, Messages.OK_BUTTON,
                                     IdeBundle.message("button.fix"), null) == Messages.NO) {
          final BrowserSettings browserSettings = new BrowserSettings();

          AsyncResult<Void> result = ShowSettingsUtil.getInstance().editConfigurable(project, browserSettings, browser == null ? null :() -> browserSettings.selectBrowser(browser));
          result.doWhenDone(() -> {
            if (launchTask != null) {
              launchTask.run();
            }
          });
        }
      }
    }, project == null ? null : project.getDisposed());
  }

  @Override
  protected void checkCreatedProcess(@Nullable final WebBrowser browser,
                                     @Nullable final Project project,
                                     @Nonnull final GeneralCommandLine commandLine,
                                     @Nonnull final Process process,
                                     @Nullable final Runnable launchTask) {
    if (isOpenCommandUsed(commandLine)) {
      final Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        @Override
        public void run() {
          try {
            if (process.waitFor() == 1) {
              doShowError(ExecUtil.readFirstLine(process.getErrorStream(), null), browser, project, null, launchTask);
            }
          }
          catch (InterruptedException ignored) {
          }
        }
      });
      // 10 seconds is enough to start
      JobScheduler.getScheduler().schedule(new Runnable() {
        @Override
        public void run() {
          future.cancel(true);
        }
      }, 10, TimeUnit.SECONDS);
    }
  }
}