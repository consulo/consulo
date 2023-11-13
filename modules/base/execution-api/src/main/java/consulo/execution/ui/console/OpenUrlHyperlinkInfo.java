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
package consulo.execution.ui.console;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.webBrowser.BrowserLauncher;
import consulo.webBrowser.WebBrowser;
import consulo.webBrowser.WebBrowserManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

public final class OpenUrlHyperlinkInfo implements HyperlinkWithPopupMenuInfo {
  private final String url;
  private final WebBrowser browser;
  private final Predicate<WebBrowser> browserCondition;

  public OpenUrlHyperlinkInfo(@Nonnull String url) {
    this(url, webBrowser -> true, null);
  }

  public OpenUrlHyperlinkInfo(@Nonnull String url, @Nullable WebBrowser browser) {
    this(url, null, browser);
  }

  public OpenUrlHyperlinkInfo(@Nonnull String url, @Nonnull Predicate<WebBrowser> browserCondition) {
    this(url, browserCondition, null);
  }

  private OpenUrlHyperlinkInfo(@Nonnull String url, @Nullable Predicate<WebBrowser> browserCondition, @Nullable WebBrowser browser) {
    this.url = url;
    this.browserCondition = browserCondition;
    this.browser = browser;
  }

  @Override
  public ActionGroup getPopupMenuGroup(@Nonnull MouseEvent event) {
    ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
    for (final WebBrowser browser : WebBrowserManager.getInstance().getActiveBrowsers()) {
      if (browserCondition == null ? (this.browser == null || browser.equals(this.browser)) : browserCondition.test(browser)) {
        builder.add(new DumbAwareAction("Open in " + browser.getName(), "Open URL in " + browser.getName(), browser.getIcon()) {
          @RequiredUIAccess
          @Override
          public void actionPerformed(@Nonnull AnActionEvent e) {
            BrowserLauncher.getInstance().browse(url, browser, e.getData(Project.KEY));
          }
        });
      }
    }

    builder.add(new AnAction("Copy URL", "Copy URL to clipboard", PlatformIconGroup.actionsCopy()) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        CopyPasteManager.getInstance().setContents(new StringSelection(url));
      }
    });
    return builder.build();
  }

  @RequiredUIAccess
  @Override
  public void navigate(Project project) {
    BrowserLauncher.getInstance().browse(url, browser, project);
  }
}
