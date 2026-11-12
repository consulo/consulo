/*
 * Copyright 2013-2022 consulo.io
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

import com.uber.nullaway.annotations.Contract;
import consulo.platform.Platform;

import org.jspecify.annotations.Nullable;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Some file path trim util which depends to user home path.
 *
 * @author VISTALL
 * @since 2022-08-10
 */
public class UserHomeFileUtil {
  @Contract("null -> null; !null -> !null")
  public static @Nullable String getLocationRelativeToUserHome(@Nullable String path) {
    return getLocationRelativeToUserHome(path, true);
  }                                                                                

  @Contract("null,_ -> null; !null,_ -> !null")
  public static @Nullable String getLocationRelativeToUserHome(@Nullable String path, boolean unixOnly) {
    return getLocationRelativeToUserHome(path, unixOnly, Platform.current());
  }

  /**
   * @param platform the machine of the path - its user home, file system and separator are used
   */
  @Contract("null,_,_ -> null; !null,_,_ -> !null")
  public static @Nullable String getLocationRelativeToUserHome(@Nullable String path, boolean unixOnly, Platform platform) {
    if (path == null) {
      return null;
    }

    if (platform.os().isUnix() || !unixOnly) {
      Path projectDir;
      try {
        projectDir = platform.fs().getPath(path);
      }
      catch (InvalidPathException e) {
        return path;
      }
      Path userHomeDir = platform.user().homePath();
      if (projectDir.startsWith(userHomeDir) && !projectDir.equals(userHomeDir)) {
        return '~' + platform.fs().getSeparator() + userHomeDir.relativize(projectDir);
      }
    }

    return path;
  }

  public static String expandUserHome(String path) {
    if (path.startsWith("~/") || path.startsWith("~\\")) {
      path = Platform.current().user().homePath() + path.substring(1);
    }
    return path;
  }
}
