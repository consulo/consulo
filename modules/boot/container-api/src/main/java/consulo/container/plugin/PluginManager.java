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

import consulo.container.internal.PluginManagerInternal;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public final class PluginManager {
  private static final PluginManagerInternal ourInternal;

  static {
    ServiceLoader<PluginManagerInternal> loader = ServiceLoader.load(PluginManagerInternal.class, PluginManagerInternal.class.getClassLoader());
    Iterator<PluginManagerInternal> iterator = loader.iterator();
    if (iterator.hasNext()) {
      ourInternal = iterator.next();
    }
    else {
      throw new IllegalArgumentException("no plugin manager internal");
    }
  }

  public static List<PluginDescriptor> getPlugins() {
    return ourInternal.getPlugins();
  }

  public static void forEachEnabledPlugin(Consumer<PluginDescriptor> consumer) {
    for (PluginDescriptor plugin : getPlugins()) {
      if (shouldSkipPlugin(plugin)) {
        continue;
      }
      consumer.accept(plugin);
    }
  }

  public static int getPluginsCount() {
    return getPlugins().size();
  }

  public static PluginDescriptor findPlugin(PluginId pluginId) {
    for (PluginDescriptor descriptor : getPlugins()) {
      if (descriptor.getPluginId().equals(pluginId)) {
        return descriptor;
      }
    }
    return null;
  }

  public static PluginDescriptor getPlugin(Class<?> pluginClass) {
    return ourInternal.getPlugin(pluginClass);
  }

  public static PluginId getPluginId(Class<?> pluginClass) {
    PluginDescriptor plugin = getPlugin(pluginClass);
    return plugin == null ? null : plugin.getPluginId();
  }

  public static File getPluginPath(Class<?> pluginClass) {
    return ourInternal.getPluginPath(pluginClass);
  }

  public static boolean shouldSkipPlugin(PluginDescriptor descriptor) {
    return descriptor.getStatus() != PluginDescriptorStatus.OK;
  }

  public static boolean disablePlugin(PluginId id) {
    return ourInternal.disablePlugin(id);
  }

  public static boolean enablePlugin(PluginId id) {
    return ourInternal.enablePlugin(id);
  }

  public static void replaceDisabledPlugins(Set<PluginId> ids) {
    ourInternal.replaceDisabledPlugins(ids);
  }

  public static Set<PluginId> getDisabledPlugins() {
    return ourInternal.getDisabledPlugins();
  }

  public static boolean isInitialized() {
    return ourInternal.isInitialized();
  }
}
