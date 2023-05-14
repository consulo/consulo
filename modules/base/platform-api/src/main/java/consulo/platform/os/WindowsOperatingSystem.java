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
package consulo.platform.os;

import consulo.platform.LineSeparator;
import consulo.platform.PlatformOperatingSystem;

import jakarta.annotation.Nonnull;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public interface WindowsOperatingSystem extends PlatformOperatingSystem {
  @Override
  @Nonnull
  default LineSeparator lineSeparator() {
    return LineSeparator.CRLF;
  }

  @Override
  boolean isWindows7OrNewer();

  @Override
  boolean isWindows8OrNewer();

  @Override
  boolean isWindows10OrNewer();

  @Override
  boolean isWindows11OrNewer();

  @Override
  @Nonnull
  default String getWindowsFileVersion(@Nonnull Path path) {
    // 1.1 - 2
    // 1.1.1 - 3
    // 1.2.3.4 - 4
    return getWindowsFileVersion(path, 4);
  }

  @Override
  @Nonnull
  String getWindowsFileVersion(@Nonnull Path path, int parts);
}
