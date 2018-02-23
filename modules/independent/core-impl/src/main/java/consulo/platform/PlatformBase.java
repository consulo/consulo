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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public abstract class PlatformBase implements Platform {
  private final PluginId myPluginId;

  protected PlatformBase(@Nonnull String pluginId) {
    myPluginId = PluginId.getId(pluginId);
  }

  @Nullable
  @Override
  public String getRuntimeProperty(@Nonnull String key) {
    return System.getProperty(key);
  }

  @Nullable
  @Override
  public String getEnvironmentVariable(@Nonnull String key) {
    return System.getenv(key);
  }

  @Nonnull
  @Override
  public PluginId getPluginId() {
    return myPluginId;
  }

  @Override
  public boolean isDesktop() {
    return false;
  }

  @Override
  public boolean isWebService() {
    return false;
  }
}
