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
package consulo.platform;

import consulo.annotation.DeprecationInfo;
import consulo.platform.os.MacOperatingSystem;
import consulo.platform.os.WindowsOperatingSystem;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public interface PlatformOperatingSystem extends Platform.OperatingSystem {
  boolean isWindows();

  @Nonnull
  default WindowsOperatingSystem asWindows() {
    return (WindowsOperatingSystem)this;
  }

  boolean isMac();

  @Nonnull
  default MacOperatingSystem asMac() {
    return (MacOperatingSystem)this;
  }

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

  // region Windows stuff which was moved to WindowsOperationSystem

  @Deprecated
  @DeprecationInfo("Use MacOperatingSystem")
  default boolean isMacMojave() {
    return isMac() && asMac().isMacMojave();
  }

  @Deprecated
  default boolean isWindows7OrNewer() {
    return isWindows() && asWindows().isWindows7OrNewer();
  }

  @Deprecated
  default boolean isWindows8OrNewer() {
    return isWindows() && asWindows().isWindows8OrNewer();
  }

  @Deprecated
  default boolean isWindows10OrNewer() {
    return isWindows() && asWindows().isWindows10OrNewer();
  }

  @Deprecated
  default boolean isWindows11OrNewer() {
    return isWindows() && asWindows().isWindows11OrNewer();
  }

  @Nonnull
  @Deprecated
  default String getWindowsFileVersion(@Nonnull Path path) {
    return asWindows().getWindowsFileVersion(path);
  }

  @Nonnull
  @Deprecated
  default String getWindowsFileVersion(@Nonnull Path path, int parts) {
    return asWindows().getWindowsFileVersion(path, parts);
  }
  // endregion
}
