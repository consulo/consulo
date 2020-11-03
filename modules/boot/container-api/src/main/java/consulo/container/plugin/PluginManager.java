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

import consulo.container.plugin.internal.PluginManagerInternal;
import consulo.util.nodep.function.Condition;
import consulo.util.nodep.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

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

  public enum PluginSkipReason {
    NO,
    DISABLED,
    INCOMPATIBLE,
    DEPENDENCY_IS_NOT_RESOLVED
  }

  @Nonnull
  public static List<PluginDescriptor> getPlugins() {
    return ourInternal.getPlugins();
  }

  @Nonnull
  public static List<PluginDescriptor> getEnabledPlugins() {
    List<PluginDescriptor> plugins = getPlugins();
    List<PluginDescriptor> result = new ArrayList<PluginDescriptor>(plugins.size());
    for (PluginDescriptor plugin : plugins) {
      if(shouldSkipPlugin(plugin)) {
        continue;
      }

      result.add(plugin);
    }
    return result;
  }

  public static int getPluginsCount() {
    return getPlugins().size();
  }

  @Nullable
  public static PluginDescriptor findPlugin(@Nonnull PluginId pluginId) {
    for (PluginDescriptor descriptor : getPlugins()) {
      if (descriptor.getPluginId().equals(pluginId)) {
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
  public static PluginId getPluginId(@Nonnull Class<?> pluginClass) {
    PluginDescriptor plugin = getPlugin(pluginClass);
    return plugin == null ? null : plugin.getPluginId();
  }

  @Nullable
  public static File getPluginPath(@Nonnull Class<?> pluginClass) {
    return ourInternal.getPluginPath(pluginClass);
  }

  public static boolean shouldSkipPlugin(@Nonnull PluginDescriptor descriptor) {
    return ourInternal.shouldSkipPlugin(descriptor);
  }

  @Nonnull
  public static PluginSkipReason calcPluginSkipReason(@Nonnull PluginDescriptor pluginDescriptor) {
    return ourInternal.calcPluginSkipReason(pluginDescriptor);
  }

  public static boolean disablePlugin(@Nonnull String id) {
    return ourInternal.disablePlugin(id);
  }

  public static boolean enablePlugin(@Nonnull String id) {
    return ourInternal.enablePlugin(id);
  }

  public static void replaceDisabledPlugins(@Nonnull List<String> ids) {
    ourInternal.replaceDisabledPlugins(ids);
  }

  @Nonnull
  public static List<String> getDisabledPlugins() {
    return ourInternal.getDisabledPlugins();
  }

  public static boolean isInitialized() {
    return ourInternal.isInitialized();
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> Class<T> resolveClass(@Nonnull String className, @Nullable PluginDescriptor descriptor) {
    try {
      return (Class<T>)Class.forName(className, true, descriptor == null ? PluginManager.class.getClassLoader() : descriptor.getPluginClassLoader());
    }
    catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static void checkDependants(final PluginDescriptor pluginDescriptor, final Function<PluginId, PluginDescriptor> pluginId2Descriptor, final Condition<PluginId> check) {
    checkDependants(pluginDescriptor, pluginId2Descriptor, check, new HashSet<PluginId>());
  }

  private static boolean checkDependants(final PluginDescriptor pluginDescriptor,
                                         final Function<PluginId, PluginDescriptor> pluginId2Descriptor,
                                         final Condition<PluginId> check,
                                         final Set<PluginId> processed) {
    processed.add(pluginDescriptor.getPluginId());
    final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
    final Set<PluginId> optionalDependencies = new HashSet<PluginId>(Arrays.asList(pluginDescriptor.getOptionalDependentPluginIds()));
    for (final PluginId dependentPluginId : dependentPluginIds) {
      if (processed.contains(dependentPluginId)) continue;

      if (!optionalDependencies.contains(dependentPluginId)) {
        if (!check.value(dependentPluginId)) {
          return false;
        }
        final PluginDescriptor dependantPluginDescriptor = pluginId2Descriptor.fun(dependentPluginId);
        if (dependantPluginDescriptor != null && !checkDependants(dependantPluginDescriptor, pluginId2Descriptor, check, processed)) {
          return false;
        }
      }
    }
    return true;
  }
}
