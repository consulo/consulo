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
package consulo.platform.impl;

import consulo.platform.PlatformOperatingSystem;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class PlatformOperatingSystemImpl implements PlatformOperatingSystem {
  private final String myOSArch;
  protected final String OS_NAME;
  private final String OS_VERSION;

  private final boolean isWindows;
  private final boolean isMac;
  private final boolean isLinux;

  public final boolean isXWindow;
  public final boolean isGNOME;
  public final boolean isKDE;

  private final Function<String, String> getEnvFunc;
  private final Supplier<Map<String, String>> getEnvsSup;

  public PlatformOperatingSystemImpl(Map<String, String> jvmProperties,
                                     Function<String, String> getEnvFunc,
                                     Supplier<Map<String, String>> getEnvsSup) {
    this.getEnvFunc = getEnvFunc;
    this.getEnvsSup = getEnvsSup;

    myOSArch = StringUtil.notNullize(jvmProperties.get("os.arch"));
    OS_NAME = jvmProperties.get("os.name");
    OS_VERSION = jvmProperties.get("os.version").toLowerCase(Locale.ROOT);
    String osNameLowered = OS_NAME.toLowerCase(Locale.ROOT);
    isWindows = osNameLowered.startsWith("windows");
    isMac = osNameLowered.startsWith("mac");
    isLinux = osNameLowered.startsWith("linux");

    isXWindow = !isWindows && !isMac;
    /* http://askubuntu.com/questions/72549/how-to-determine-which-window-manager-is-running/227669#227669 */
    isGNOME = isXWindow &&
      (StringUtil.notNullize(getEnvFunc.apply("GDMSESSION")).startsWith("gnome") ||
        StringUtil.notNullize(getEnvFunc.apply("XDG_CURRENT_DESKTOP")).toLowerCase(Locale.ROOT).endsWith("gnome"));
    /* https://userbase.kde.org/KDE_System_Administration/Environment_Variables#KDE_FULL_SESSION */
    isKDE = isXWindow && !StringUtil.isEmpty(getEnvFunc.apply("KDE_FULL_SESSION"));
  }

  public boolean isOsVersionAtLeast(@Nonnull String version) {
    return StringUtil.compareVersionNumbers(OS_VERSION, version) >= 0;
  }

  @Override
  public boolean isWindows() {
    return isWindows;
  }

  @Override
  public boolean isMac() {
    return isMac;
  }

  @Override
  public boolean isLinux() {
    return isLinux;
  }

  @Override
  public boolean isKDE() {
    return isKDE;
  }

  @Override
  public boolean isGNOME() {
    return isGNOME;
  }

  @Nonnull
  @Override
  public String name() {
    return OS_NAME;
  }

  @Nonnull
  @Override
  public String version() {
    return OS_VERSION;
  }

  @Nullable
  @Override
  public String getEnvironmentVariable(@Nonnull String key) {
    return getEnvFunc.apply(key);
  }

  @Nonnull
  @Override
  public Map<String, String> environmentVariables() {
    return getEnvsSup.get();
  }

  @Nonnull
  @Override
  public String arch() {
    return myOSArch;
  }
}
