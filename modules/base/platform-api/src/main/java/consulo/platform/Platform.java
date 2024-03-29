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

import consulo.annotation.DeprecationInfo;
import consulo.platform.internal.PlatformInternal;
import consulo.ui.image.Image;

import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public interface Platform extends UserDataHolder {
  static String LOCAL = "local";

  @Deprecated
  @DeprecationInfo("Use PlatformFileSystem")
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

  @Deprecated
  @DeprecationInfo("Use PlatformOperatingSystem")
  interface OperatingSystem {
    boolean isWindows();

    boolean isWindows7OrNewer();

    boolean isWindows8OrNewer();

    boolean isWindows10OrNewer();

    boolean isWindows11OrNewer();

    boolean isMac();
 
    boolean isMacMojave();

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
    default LineSeparator lineSeparator() {
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
    Map<String, String> environmentVariables();

    @Nullable
    String getEnvironmentVariable(@Nonnull String key);

    @Nullable
    default String getEnvironmentVariable(@Nonnull String key, @Nonnull String defaultValue) {
      String environmentVariable = getEnvironmentVariable(key);
      return environmentVariable == null ? defaultValue : environmentVariable;
    }

    @Nonnull
    default String getWindowsFileVersion(@Nonnull Path path) {
      // 1.1 - 2
      // 1.1.1 - 3
      // 1.2.3.4 - 4
      return getWindowsFileVersion(path, 4);
    }

    @Nonnull
    String getWindowsFileVersion(@Nonnull Path path, int parts);
  }

  @Deprecated
  @DeprecationInfo("Use PlatformJvm")
  interface Jvm {
    @Nonnull
    String version();

    @Nonnull
    String runtimeVersion();

    @Nonnull
    String vendor();

    @Nonnull
    String name();

    @Nullable
    String getRuntimeProperty(@Nonnull String key);

    @Nullable
    default String getRuntimeProperty(@Nonnull String key, @Nonnull String defaultValue) {
      String runtimeProperty = getRuntimeProperty(key);
      return runtimeProperty == null ? defaultValue : runtimeProperty;
    }

    @Nonnull
    Map<String, String> getRuntimeProperties();

    default boolean isArm64() {
      return CpuArchitecture.AARCH64 == arch();
    }

    default boolean isAmd64() {
      return CpuArchitecture.X86_64 == arch();
    }

    default boolean isAny64Bit() {
      return arch().getWidth() == 64;
    }

    @Nonnull
    CpuArchitecture arch();
  }

  @Deprecated
  @DeprecationInfo("Use PlatformUser")
  interface User {
    boolean superUser();

    @Nonnull
    String name();

    @Nonnull
    Path homePath();

    default boolean darkTheme() {
      return false;
    }
  }

  @Nonnull
  static Platform current() {
    return PlatformInternal.current();
  }

  @Nonnull
  String getId();

  @Nonnull
  String getName();

  @Nonnull
  PlatformFileSystem fs();

  @Nonnull
  PlatformOperatingSystem os();

  @Nonnull
  PlatformJvm jvm();

  @Nonnull
  PlatformUser user();

  default void openInBrowser(String url) {
    try {
      openInBrowser(new URL(url));
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  void openInBrowser(@Nonnull URL url);

  default void openFileInFileManager(@Nonnull Path path) {
    openFileInFileManager(path.toFile());
  }

  void openFileInFileManager(@Nonnull File file);

  default void openDirectoryInFileManager(@Nonnull Path path) {
    openFileInFileManager(path.toFile());
  }

  void openDirectoryInFileManager(@Nonnull File file);

  @Nonnull
  default String mapExecutableName(@Nonnull String baseName) {
    if (jvm().isAmd64()) {
      return baseName + "64";
    }

    if (jvm().isArm64()) {
      return baseName + "-aarch64";
    }

    return baseName;
  }

  @Nonnull
  default String mapAnyExecutableName(@Nonnull String baseName) {
    if (os().isWindows()) {
      return mapWindowsExecutable(baseName, "exe");
    }

    return mapExecutableName(baseName);
  }

  @Nonnull
  default String mapWindowsExecutable(@Nonnull String baseName, @Nonnull String extension) {
    if (!os().isWindows()) {
      throw new IllegalArgumentException("Must be Windows");
    }

    return mapExecutableName(baseName) + "." + extension;
  }

  @Nonnull
  default String mapLibraryName(@Nonnull String libName) {
    String baseName = libName;
    if (jvm().isAmd64()) {
      baseName = baseName + "64";
    }
    else if (jvm().isArm64()) {
      baseName = baseName + "-aarch64";
    }

    String fileName = System.mapLibraryName(baseName);
    if (os().isMac()) {
      fileName = fileName.replace(".jnilib", ".dylib");
    }
    return fileName;
  }
}
