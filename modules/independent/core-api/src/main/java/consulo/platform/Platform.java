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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 16-May-17
 */
public interface Platform {
  @NotNull
  static Platform current() {
    return _PlatformInternal.current();
  }

  @NotNull
  PluginId getPluginId();

  boolean isDesktop();

  boolean isWebService();

  @Nullable
  String getRuntimeProperty(@NotNull String key);

  @Nullable
  default String getRuntimeProperty(@NotNull String key, @NotNull String defaultValue) {
    return ObjectUtil.notNull(getRuntimeProperty(key), defaultValue);
  }

  @Nullable
  String getEnvironmentVariable(@NotNull String key);

  @Nullable
  default String getEnvironmentVariable(@NotNull String key, @NotNull String defaultValue) {
    return ObjectUtil.notNull(getEnvironmentVariable(key), defaultValue);
  }
}
