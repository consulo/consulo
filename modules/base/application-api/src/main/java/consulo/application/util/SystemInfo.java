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
package consulo.application.util;

import consulo.annotation.DeprecationInfo;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Deprecated
@DeprecationInfo("Use Platform#current()")
public class SystemInfo {
  public static final String OS_NAME = System.getProperty("os.name");
  public static final String OS_VERSION = System.getProperty("os.version").toLowerCase();
  public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");
  public static final String ARCH_DATA_MODEL = System.getProperty("sun.arch.data.model");

  public static final boolean is32Bit = ARCH_DATA_MODEL == null || ARCH_DATA_MODEL.equals("32");
  public static final boolean is64Bit = !is32Bit;

  public static final String OS_ARCH = System.getProperty("os.arch");
  public static final String JAVA_VERSION = System.getProperty("java.version");
  public static final String SUN_DESKTOP = System.getProperty("sun.desktop", "");

  protected static final String _OS_NAME = OS_NAME.toLowerCase();
  public static final boolean isWindows = _OS_NAME.startsWith("windows");
  public static final boolean isMac = _OS_NAME.startsWith("mac");
  public static final boolean isLinux = _OS_NAME.startsWith("linux");
  public static final boolean isUnix = !isWindows;
  public static final boolean isFreeBSD = _OS_NAME.startsWith("freebsd");

  public static final boolean isFileSystemCaseSensitive =
    isUnix && !isMac || "true".equalsIgnoreCase(System.getProperty("consulo.case.sensitive.fs"));

  public static final boolean isJetBrainsJvm = isJetbrainsJvm() || isConsuloJvm();

  public static boolean isOsVersionAtLeast(@Nonnull String version) {
    return StringUtil.compareVersionNumbers(OS_VERSION, version) >= 0;
  }

  // version numbers from http://msdn.microsoft.com/en-us/library/windows/desktop/ms724832.aspx
  public static final boolean isWin2kOrNewer = isWindows && isOsVersionAtLeast("5.0");
  public static final boolean isWinXpOrNewer = isWindows && isOsVersionAtLeast("5.1");
  public static final boolean isWinVistaOrNewer = isWindows && isOsVersionAtLeast("6.0");
  public static final boolean isWin7OrNewer = isWindows && isOsVersionAtLeast("6.1");
  public static final boolean isWin8OrNewer = isWindows && isOsVersionAtLeast("6.2");
  public static final boolean isWin10OrNewer = isWindows && isOsVersionAtLeast("10.0");

  public static final boolean isXWindow = isUnix && !isMac;

  // http://www.freedesktop.org/software/systemd/man/os-release.html
  private static Supplier<Map<String, String>> ourOsReleaseInfo = LazyValue.atomicNotNull(() -> {
    if (isUnix && !isMac) {
      try {
        List<String> lines = FileUtil.loadLines("/etc/os-release");
        Map<String, String> info = new HashMap<>();
        for (String line : lines) {
          int p = line.indexOf('=');
          if (p > 0) {
            String name = line.substring(0, p);
            String value = StringUtil.unquoteString(line.substring(p + 1));
            if (!StringUtil.isEmptyOrSpaces(name) && !StringUtil.isEmptyOrSpaces(value)) {
              info.put(name, value);
            }
          }
        }
        return info;
      }
      catch (IOException ignored) {
      }
    }

    return Collections.emptyMap();
  });

  @Nullable
  public static String getUnixReleaseName() {
    return ourOsReleaseInfo.get().get("NAME");
  }

  @Nullable
  public static String getUnixReleaseVersion() {
    return ourOsReleaseInfo.get().get("VERSION");
  }

  // public static final boolean isMacSystemMenu = isMac && "true".equals(System.getProperty("apple.laf.useScreenMenuBar"));

  public static final boolean areSymLinksSupported = isUnix || isWinVistaOrNewer;

  public static final boolean isAMD64 = "amd64".equals(OS_ARCH);

  private static final Supplier<Boolean> ourHasXdgOpen = LazyValue.atomicNotNull(() -> {
    return isUnix && new File("/usr/bin/xdg-open").canExecute();
  });

  public static boolean hasXdgOpen() {
    return ourHasXdgOpen.get();
  }

  private static final Supplier<Boolean> ourHasXdgMime = LazyValue.atomicNotNull(() -> {
    return isUnix && new File("/usr/bin/xdg-mime").canExecute();
  });

  public static boolean hasXdgMime() {
    return ourHasXdgOpen.get();
  }

  public static final boolean isMacOSTiger = isMac && isOsVersionAtLeast("10.4");
  public static final boolean isMacOSLeopard = isMac && isOsVersionAtLeast("10.5");
  public static final boolean isMacOSSnowLeopard = isMac && isOsVersionAtLeast("10.6");
  public static final boolean isMacOSLion = isMac && isOsVersionAtLeast("10.7");
  public static final boolean isMacOSMountainLion = isMac && isOsVersionAtLeast("10.8");
  public static final boolean isMacOSMavericks = isMac && isOsVersionAtLeast("10.9");
  public static final boolean isMacOSYosemite = isMac && isOsVersionAtLeast("10.10");
  public static final boolean isMacOSElCapitan = isMac && isOsVersionAtLeast("10.11");
  public static final boolean isMacOSCatalina = isMac && isOsVersionAtLeast("10.15");

  @Nonnull
  public static String getMacOSMajorVersion() {
    return getMacOSMajorVersion(OS_VERSION);
  }

  public static String getMacOSMajorVersion(String version) {
    int[] parts = getMacOSVersionParts(version);
    return String.format("%d.%d", parts[0], parts[1]);
  }

  @Nonnull
  public static String getMacOSVersionCode() {
    return getMacOSVersionCode(OS_VERSION);
  }

  @Nonnull
  public static String getMacOSMajorVersionCode() {
    return getMacOSMajorVersionCode(OS_VERSION);
  }

  @Nonnull
  public static String getMacOSMinorVersionCode() {
    return getMacOSMinorVersionCode(OS_VERSION);
  }

  @Nonnull
  public static String getMacOSVersionCode(@Nonnull String version) {
    int[] parts = getMacOSVersionParts(version);
    return String.format("%02d%d%d", parts[0], normalize(parts[1]), normalize(parts[2]));
  }

  @Nonnull
  public static String getMacOSMajorVersionCode(@Nonnull String version) {
    int[] parts = getMacOSVersionParts(version);
    return String.format("%02d%d%d", parts[0], normalize(parts[1]), 0);
  }

  @Nonnull
  public static String getMacOSMinorVersionCode(@Nonnull String version) {
    int[] parts = getMacOSVersionParts(version);
    return String.format("%02d%02d", parts[1], parts[2]);
  }

  private static int[] getMacOSVersionParts(@Nonnull String version) {
    List<String> parts = StringUtil.split(version, ".");
    while (parts.size() < 3) {
      parts.add("0");
    }
    return new int[]{toInt(parts.get(0)), toInt(parts.get(1)), toInt(parts.get(2))};
  }

  private static int normalize(int number) {
    return number > 9 ? 9 : number;
  }

  private static int toInt(String string) {
    try {
      return Integer.valueOf(string);
    }
    catch (NumberFormatException e) {
      return 0;
    }
  }

  public static boolean isJavaVersionAtLeast(int major) {
    return isJavaVersionAtLeast(major, 0, 0);
  }

  public static boolean isJavaVersionAtLeast(int major, int minor, int update) {
    return JavaVersion.current().compareTo(JavaVersion.compose(major, minor, update, 0, false)) >= 0;
  }

  /**
   * @deprecated use {@link #isJavaVersionAtLeast(int, int, int)} (to be removed in IDEA 2020)
   */
  public static boolean isJavaVersionAtLeast(String v) {
    return StringUtil.compareVersionNumbers(JAVA_RUNTIME_VERSION, v) >= 0;
  }

  private static boolean isJetbrainsJvm() {
    final String vendor = SystemProperties.getJavaVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "jetbrains");
  }

  private static boolean isConsuloJvm() {
    final String vendor = SystemProperties.getJavaVendor();
    return vendor != null && StringUtil.containsIgnoreCase(vendor, "consulo");
  }
}
