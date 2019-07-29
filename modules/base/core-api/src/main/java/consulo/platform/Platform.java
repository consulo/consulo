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

import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.ObjectUtil;
import consulo.annotations.DeprecationInfo;
import consulo.platform.internal.PlatformInternal;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

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
    default Image getImage(File file) {
      return null;
    }
  }

  interface OperatingSystem {
    boolean isWindows();

    boolean isMac();
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
  PluginId getPluginId();

  boolean isDesktop();

  boolean isWebService();

  /**
   * @return is has administrative rights
   */
  boolean isUnderRoot();

  @Nullable
  String getRuntimeProperty(@Nonnull String key);

  @Nullable
  default String getRuntimeProperty(@Nonnull String key, @Nonnull String defaultValue) {
    return ObjectUtil.notNull(getRuntimeProperty(key), defaultValue);
  }

  @Nullable
  String getEnvironmentVariable(@Nonnull String key);

  @Nullable
  default String getEnvironmentVariable(@Nonnull String key, @Nonnull String defaultValue) {
    return ObjectUtil.notNull(getEnvironmentVariable(key), defaultValue);
  }
}
