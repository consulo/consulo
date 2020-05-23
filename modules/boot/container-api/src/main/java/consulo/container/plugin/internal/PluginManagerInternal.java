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
package consulo.container.plugin.internal;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public interface PluginManagerInternal {
  @Nonnull
  List<PluginDescriptor> getPlugins();

  boolean isInitialized();

  @Nullable
  File getPluginPath(@Nonnull Class<?> pluginClass);

  @Nullable
  PluginDescriptor getPlugin(@Nonnull Class<?> pluginClass);

  @Nonnull
  List<String> getDisabledPlugins();

  boolean shouldSkipPlugin(@Nonnull PluginDescriptor descriptor);

  PluginManager.PluginSkipReason calcPluginSkipReason(final PluginDescriptor descriptor);

  boolean disablePlugin(String id);

  boolean enablePlugin(String id);

  void replaceDisabledPlugins(List<String> ids);
}
