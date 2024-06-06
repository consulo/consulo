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
package consulo.ide.impl.idea.ide;

import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.platform.Platform;
import consulo.process.local.ExecUtil;
import consulo.ide.impl.idea.ide.browsers.BrowserLauncherAppless;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.documentation.PlatformDocumentationUtil;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.webBrowser.BrowserLauncher;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static consulo.ide.impl.idea.util.containers.ContainerUtilRt.newArrayList;

public class BrowserUtil {
  private BrowserUtil() {
  }

  public static boolean isAbsoluteURL(String url) {
    return URLUtil.isAbsoluteURL(url);
  }

  @Deprecated
  public static String getDocURL(String url) {
    return PlatformDocumentationUtil.getDocURL(url);
  }

  @Nullable
  public static URL getURL(String url) throws MalformedURLException {
    return isAbsoluteURL(url) ? VfsUtil.convertToURL(url) : new URL("file", "", url);
  }

  public static void browse(@Nonnull VirtualFile file) {
    browse(VfsUtil.toUri(file));
  }

  public static void browse(@Nonnull File file) {
    getBrowserLauncher().browse(file);
  }

  public static void browse(@Nonnull URL url) {
    browse(url.toExternalForm());
  }

  @SuppressWarnings("UnusedDeclaration")
  @Deprecated
  /**
   * @deprecated Use {@link #browse(String)}
   */
  public static void launchBrowser(@Nonnull @NonNls String url) {
    browse(url);
  }

  public static void browse(@Nonnull @NonNls String url) {
    getBrowserLauncher().browse(url, null);
  }

  private static BrowserLauncher getBrowserLauncher() {
    BrowserLauncher launcher = ApplicationManager.getApplication() == null ? null : BrowserLauncher.getInstance();
    return launcher == null ? BrowserLauncherAppless.INSTANCE : launcher;
  }

  public static void open(@Nonnull @NonNls String url) {
    getBrowserLauncher().open(url);
  }

  /**
   * Main method: tries to launch a browser using every possible way
   */
  public static void browse(@Nonnull URI uri) {
    getBrowserLauncher().browse(uri);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Nonnull
  @Deprecated
  public static List<String> getOpenBrowserCommand(@NonNls @Nonnull String browserPathOrName) {
    return getOpenBrowserCommand(browserPathOrName, false);
  }

  @Nonnull
  public static List<String> getOpenBrowserCommand(@NonNls @Nonnull String browserPathOrName, boolean newWindowIfPossible) {
    if (new File(browserPathOrName).isFile()) {
      return Collections.singletonList(browserPathOrName);
    }
    else if (Platform.current().os().isMac()) {
      List<String> command = newArrayList(ExecUtil.getOpenCommandPath(), "-a", browserPathOrName);
      if (newWindowIfPossible) {
        command.add("-n");
      }
      return command;
    }
    else if (Platform.current().os().isWindows()) {
      return Arrays.asList(ExecUtil.getWindowsShellName(), "/c", "start", GeneralCommandLine.inescapableQuote(""), browserPathOrName);
    }
    else {
      return Collections.singletonList(browserPathOrName);
    }
  }

  public static boolean isOpenCommandSupportArgs() {
    return SystemInfo.isMacOSSnowLeopard;
  }

  @Nonnull
  public static String getDefaultAlternativeBrowserPath() {
    if (Platform.current().os().isWindows()) {
      return "C:\\Program Files\\Internet Explorer\\IExplore.exe";
    }
    else if (Platform.current().os().isMac()) {
      return "open";
    }
    else if (Platform.current().os().isUnix()) {
      return "/usr/bin/firefox";
    }
    else {
      return "";
    }
  }
}
