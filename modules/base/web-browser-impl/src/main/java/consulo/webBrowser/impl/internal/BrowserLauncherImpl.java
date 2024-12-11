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
package consulo.webBrowser.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.local.ExecUtil;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.StringUtil;
import consulo.webBrowser.DefaultBrowserPolicy;
import consulo.webBrowser.WebBrowser;
import consulo.webBrowser.WebBrowserManager;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

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
            WebBrowserManager browserManager = WebBrowserManager.getInstance();
            if (browserManager.getDefaultBrowserPolicy() == DefaultBrowserPolicy.FIRST) {
                effectiveBrowser = browserManager.getFirstActiveBrowser();
            }
        }
        return effectiveBrowser;
    }

    @Override
    protected void doShowError(@Nullable final String error, @Nullable final WebBrowser browser, @Nullable final Project project, final String title, @Nullable final Runnable launchTask) {
        AppUIUtil.invokeOnEdt(() -> {
            if (Messages.showYesNoDialog(project, StringUtil.notNullize(error, "Unknown error"),
                title == null ? WebBrowserLocalize.browserError().get() : title, Messages.OK_BUTTON,
                WebBrowserLocalize.browserFix().get(), null) == Messages.NO) {
                UIAccess uiAccess = UIAccess.current();

                Application.get().getInstance(ShowConfigurableService.class).show(project, BrowserSettings.class).whenCompleteAsync((o, throwable) -> {
                    if (launchTask != null) {
                        launchTask.run();
                    }
                }, uiAccess);
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
            final Future<?> future = Application.get().executeOnPooledThread(new Runnable() {
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
            AppExecutorUtil.getAppScheduledExecutorService().schedule((Runnable) () -> future.cancel(true), 10, TimeUnit.SECONDS);
        }
    }
}