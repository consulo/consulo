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
package consulo.container.impl;

import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.container.plugin.internal.PluginManagerInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PluginManagerInternalImpl implements PluginManagerInternal {

  @Nonnull
  @Override
  public List<PluginDescriptor> getPlugins() {
    return PluginHolderModificator.getPlugins();
  }

  @Override
  public boolean isInitialized() {
    return PluginHolderModificator.isInitialized();
  }

  @Nullable
  @Override
  public File getPluginPath(@Nonnull Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    assert temp instanceof PluginClassLoader : "classloader is not plugin";
    PluginClassLoader classLoader = (PluginClassLoader)temp;
    PluginId pluginId = classLoader.getPluginId();
    PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
    assert plugin != null : "plugin is not found";
    return plugin.getPath();
  }

  @Nullable
  @Override
  public PluginDescriptor getPlugin(@Nonnull Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    if (!(temp instanceof PluginClassLoader)) {
      return null;
    }
    return ((PluginClassLoader)temp).getPluginDescriptor();
  }

  @Nonnull
  @Override
  public List<String> getDisabledPlugins() {
    return PluginValidator.getDisabledPlugins();
  }

  @Override
  public boolean shouldSkipPlugin(@Nonnull PluginDescriptor descriptor) {
    return PluginValidator.shouldSkipPlugin(descriptor);
  }

  @Override
  public PluginManager.PluginSkipReason calcPluginSkipReason(PluginDescriptor descriptor) {
    return PluginValidator.calcPluginSkipReason(descriptor);
  }

  @Override
  public boolean disablePlugin(String id) {
    return PluginValidator.disablePlugin(id);
  }

  @Override
  public boolean enablePlugin(String id) {
    return PluginValidator.enablePlugin(id);
  }

  @Override
  public void replaceDisabledPlugins(List<String> ids) {
    PluginValidator.replaceDisabledPlugins(ids);
  }
}
