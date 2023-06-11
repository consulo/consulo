/*
 * Copyright 2013-2023 consulo.io
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
package consulo.process.internal;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.process.cmd.GeneralCommandLine;
import consulo.util.dataholder.Key;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11/06/2023
 */
public class SystemExecutableInfo {
  private static final Logger LOG = Logger.getInstance(SystemExecutableInfo.class);

  private static final Key<SystemExecutableInfo> KEY = Key.create(SystemExecutableInfo.class);

  public static SystemExecutableInfo get(Platform platform) {
    SystemExecutableInfo data = platform.getUserData(KEY);
    if (data != null) {
      return data;
    }

    data = new SystemExecutableInfo(platform);
    return platform.putUserDataIfAbsent(KEY, data);
  }

  public final Supplier<Boolean> hasGkSudo;
  public final Supplier<Boolean> hasKdeSudo;
  public final Supplier<Boolean> hasPkExec;
  public final Supplier<Boolean> hasGnomeTerminal;
  public final Supplier<Boolean> hasKdeTerminal;
  public final Supplier<Boolean> hasXTerm;

  private final Platform myPlatform;

  public SystemExecutableInfo(Platform platform) {
    myPlatform = platform;

    hasGkSudo = LazyValue.notNull(executableChecker("/usr/bin/gksudo"));
    hasKdeSudo = LazyValue.notNull(executableChecker("/usr/bin/kdesudo"));
    hasPkExec = LazyValue.notNull(executableChecker("/usr/bin/pkexec"));
    hasGnomeTerminal = LazyValue.notNull(executableChecker("/usr/bin/gnome-terminal"));
    hasKdeTerminal = LazyValue.notNull(executableChecker("/usr/bin/konsole"));
    hasXTerm = LazyValue.notNull(executableChecker("/usr/bin/xterm"));
  }

  private Supplier<Boolean> executableChecker(String path) {
    return () -> {
      try {
        return Files.isExecutable(myPlatform.fs().getPath(path));
      }
      catch (Exception e) {
        LOG.warn(path, e);
        return false;
      }
    };
  }

  @Nonnull
  public List<String> getTerminalCommand(@Nullable String title, @Nonnull String command) {
    PlatformOperatingSystem os = myPlatform.os();

    if (os.isWindows()) {
      title = title != null ? title.replace("\"", "'") : "";
      return Arrays.asList(getWindowsShellName(), "/c", "start", GeneralCommandLine.inescapableQuote(title), command);
    }
    else if (os.isMac()) {
      return Arrays.asList(getOpenCommandPath(), "-a", "Terminal", command);
    }
    else if (hasKdeTerminal.get()) {
      return Arrays.asList("/usr/bin/konsole", "-e", command);
    }
    else if (hasGnomeTerminal.get()) {
      return title != null ? Arrays.asList("/usr/bin/gnome-terminal", "-t", title, "-x", command)
        : Arrays.asList("/usr/bin/gnome-terminal", "-x", command);
    }
    else if (hasXTerm.get()) {
      return title != null ? Arrays.asList("/usr/bin/xterm", "-T", title, "-e", command)
        : Arrays.asList("/usr/bin/xterm", "-e", command);
    }

    throw new UnsupportedSystemException(Platform.current());
  }

  @Nonnull
  public String getOsascriptPath() {
    return "/usr/bin/osascript";
  }

  @Nonnull
  public String getOpenCommandPath() {
    return "/usr/bin/open";
  }

  @Nonnull
  public static String getWindowsShellName() {
    return "cmd.exe";
  }

  public boolean hasTerminalApp() {
    PlatformOperatingSystem os = myPlatform.os();
    return os.isWindows() || os.isMac() || hasKdeTerminal.get() || hasGnomeTerminal.get() || hasXTerm.get();
  }
}
