/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.container.internal.PluginManagerInternal;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStub;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
public class LightPluginManager implements PluginManagerInternal {
  private PluginDescriptor myTestPlugin = new PluginDescriptorStub() {
    @Override
    public PluginId getPluginId() {
      return PluginIds.CONSULO_BASE;
    }

    @Override
    public ClassLoader getPluginClassLoader() {
      return LightPluginManager.class.getClassLoader();
    }
  };

  private List<PluginDescriptor> myPlugins = List.of(myTestPlugin);

  @Override
  public List<PluginDescriptor> getPlugins() {
    return myPlugins;
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public File getPluginPath(Class<?> pluginClass) {
    return null;
  }

  @Override
  public PluginDescriptor getPlugin(Class<?> pluginClass) {
    return myTestPlugin;
  }
}
