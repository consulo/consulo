/*
 * Copyright 2013-2017 consulo.io
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
package consulo.platform;

import com.intellij.util.LineSeparator;
import com.intellij.util.ObjectUtil;
import consulo.annotation.DeprecationInfo;
import consulo.platform.internal.PlatformInternal;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public interface Platform {
  //region Migration staff
  @Deprecated
  @DeprecationInfo("This is marker for running tasks if desktop platform, in ideal future, must be removed")
  @SuppressWarnings("deprecation")
  static void runIfDesktopPlatform(@Nonnull Runnable runnable) {
    if (current().isDesktop()) {
      runnable.run();
    }
  }
  //endregion

  interface FileSystem {
    boolean isCaseSensitive();

    boolean areSymLinksSupported();

    /**
     * @return image filemanager image for file. If return null it will use default icon from IDE
     */
    @Nullable
    default Image getImage(@Nonnull File file) {
      return null;
    }
  }

  interface OperatingSystem {
    boolean isWindows();

    boolean isWindowsVistaOrNewer();

    boolean isWindows7OrNewer();

    boolean isWindows8OrNewer();

    boolean isWindows10OrNewer();

    boolean isMac();

    boolean isLinux();

    default boolean isUnix() {
      return !isWindows();
    }

    default boolean isXWindow() {
      return isUnix() && !isMac();
    }

    boolean isKDE();

    boolean isGNOME();

    @Nonnull
    default LineSeparator getLineSeparator() {
      if (isWindows()) {
        return LineSeparator.CRLF;
      }
      return LineSeparator.LF;
    }

    @Nonnull
    String name();

    @Nonnull
    String version();

    @Nonnull
    String arch();

    @Nonnull
    Map<String, String> getEnvironmentVariables();

    @Nullable
    String getEnvironmentVariable(@Nonnull String key);

    @Nullable
    default String getEnvironmentVariable(@Nonnull String key, @Nonnull String defaultValue) {
      return ObjectUtil.notNull(getEnvironmentVariable(key), defaultValue);
    }
  }

  interface Jvm {
    @Nonnull
    String version();

    @Nonnull
    String runtimeVersion();

    @Nonnull
    String vendor();

    @Nullable
    String getRuntimeProperty(@Nonnull String key);

    @Nullable
    default String getRuntimeProperty(@Nonnull String key, @Nonnull String defaultValue) {
      return ObjectUtil.notNull(getRuntimeProperty(key), defaultValue);
    }

    @Nonnull
    Map<String, String> getRuntimeProperties();

    boolean isArm64();

    boolean isAmd64();

    default boolean isAny64Bit() {
      return isArm64() || isAmd64();
    }
  }

  interface User {
    boolean superUser();

    @Nonnull
    String name();

    @Nonnull
    Path homePath();

    @Deprecated
    default boolean isSuperUser() {
      return superUser();
    }

    @Nonnull
    @Deprecated
    default String getName() {
      return name();
    }

    @Nonnull
    @Deprecated
    default Path getHomePath() {
      return homePath();
    }
  }

  @Nonnull
  static Platform current() {
    return PlatformInternal.current();
  }

  @Nonnull
  FileSystem fs();

  @Nonnull
  OperatingSystem os();

  @Nonnull
  Jvm jvm();

  @Nonnull
  User user();

  boolean isDesktop();

  boolean isWebService();

  @Nonnull
  default String mapWindowsExecutable(@Nonnull String baseName, @Nonnull String extension) {
    if (!os().isWindows()) {
      throw new IllegalArgumentException("Must be Windows");
    }

    if (jvm().isAmd64()) {
      return baseName + "64" + "." + extension;
    }

    if (jvm().isArm64()) {
      return baseName + "-arm64" + "." + extension;
    }

    return baseName + "." + extension;
  }

  @Nonnull
  default String mapLibraryName(@Nonnull String libName) {
    String baseName = libName;
    if (jvm().isAny64Bit()) {
      baseName = baseName.replace("32", "") + "64";
    }

    String fileName = System.mapLibraryName(baseName);
    if (os().isMac()) {
      fileName = fileName.replace(".jnilib", ".dylib");
    }
    return fileName;
  }
}
