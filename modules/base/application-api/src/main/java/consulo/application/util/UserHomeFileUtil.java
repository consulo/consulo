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

import consulo.platform.Platform;
import consulo.util.io.FileUtil;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;

/**
 * Some file path trim util which depends to user home path.
 *
 * @author VISTALL
 * @since 10-Aug-22
 */
public class UserHomeFileUtil {
  @Contract("null -> null; !null -> !null")
  public static String getLocationRelativeToUserHome(@Nullable String path) {
    return getLocationRelativeToUserHome(path, true);
  }                                                                                

  @Contract("null,_ -> null; !null,_ -> !null")
  public static String getLocationRelativeToUserHome(@Nullable String path, boolean unixOnly) {
    if (path == null) return null;

    Platform platform = Platform.current();
    if (platform.os().isUnix() || !unixOnly) {
      File projectDir = new File(path);
      File userHomeDir = platform.user().homePath().toFile();
      if (FileUtil.isAncestor(userHomeDir, projectDir, true)) {
        return '~' + File.separator + FileUtil.getRelativePath(userHomeDir, projectDir);
      }
    }

    return path;
  }

  @Nonnull
  public static String expandUserHome(@Nonnull String path) {
    if (path.startsWith("~/") || path.startsWith("~\\")) {
      path = Platform.current().user().homePath() + path.substring(1);
    }
    return path;
  }
}
