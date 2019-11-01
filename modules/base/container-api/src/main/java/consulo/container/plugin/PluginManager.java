/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.plugin;

import com.intellij.openapi.extensions.PluginId;
import consulo.container.plugin.internal.PluginManagerInternal;
import consulo.util.ServiceLoaderUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public final class PluginManager {
  private static final PluginManagerInternal ourInternal = ServiceLoaderUtil.loadSingleOrError(PluginManagerInternal.class);

  @Nonnull
  public static List<PluginDescriptor> getPlugins() {
    return ourInternal.getPlugins();
  }

  public static int getPluginsCount() {
    return getPlugins().size();
  }

  @Nullable
  public static PluginDescriptor findPlugin(@Nonnull PluginId pluginId) {
    for (PluginDescriptor descriptor : getPlugins()) {
      if(descriptor.getPluginId().equals(pluginId)) {
        return descriptor;
      }
    }

    return null;
  }

  @Nullable
  public static PluginDescriptor getPlugin(@Nonnull Class<?> pluginClass) {
    return ourInternal.getPlugin(pluginClass);
  }

  @Nullable
  public static File getPluginPath(@Nonnull Class<?> pluginClass) {
    return ourInternal.getPluginPath(pluginClass);
  }

  public static boolean isInitialized() {
    return ourInternal.isInitialized();
  }
}
