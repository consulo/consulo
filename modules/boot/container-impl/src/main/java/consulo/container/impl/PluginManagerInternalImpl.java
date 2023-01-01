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
import consulo.container.internal.PluginManagerInternal;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.container.plugin.PluginDescriptorStatus;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PluginManagerInternalImpl implements PluginManagerInternal {
  @Override
  public List<PluginDescriptor> getPlugins() {
    return PluginHolderModificator.getPlugins();
  }

  @Override
  public boolean isInitialized() {
    return PluginHolderModificator.isInitialized();
  }

  @Override
  public File getPluginPath(Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    assert temp instanceof PluginClassLoader : "classloader is not plugin";
    PluginClassLoader classLoader = (PluginClassLoader)temp;
    PluginId pluginId = classLoader.getPluginId();
    PluginDescriptor plugin = PluginManager.findPlugin(pluginId);
    assert plugin != null : "plugin is not found";
    return plugin.getPath();
  }

  @Override
  public PluginDescriptor getPlugin(Class<?> pluginClass) {
    ClassLoader temp = pluginClass.getClassLoader();
    if (!(temp instanceof PluginClassLoader)) {
      return null;
    }
    return ((PluginClassLoader)temp).getPluginDescriptor();
  }


  @Override
  public Set<PluginId> getDisabledPlugins() {
    return PluginValidator.getDisabledPlugins();
  }

  @Override
  public boolean disablePlugin(PluginId id) {
    return PluginValidator.disablePlugin(id);
  }

  @Override
  public boolean enablePlugin(PluginId id) {
    return PluginValidator.enablePlugin(id);
  }

  @Override
  public void replaceDisabledPlugins(Set<PluginId> ids) {
    PluginValidator.replaceDisabledPlugins(ids);
  }
}
