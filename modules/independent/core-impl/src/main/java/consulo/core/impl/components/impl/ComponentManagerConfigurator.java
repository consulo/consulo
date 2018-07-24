/*
 * Copyright 2013-2018 consulo.io
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
package consulo.core.impl.components.impl;

import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.extensions.PluginDescriptor;

import javax.annotation.Nonnull;

class ComponentManagerConfigurator {
  private final ComponentManagerImpl myComponentManager;

  public ComponentManagerConfigurator(final ComponentManagerImpl componentManager) {
    myComponentManager = componentManager;
  }

  private void loadConfiguration(@Nonnull ComponentConfig[] configs, final boolean defaultProject, final PluginDescriptor descriptor) {
    for (ComponentConfig config : configs) {
      loadSingleConfig(defaultProject, config, descriptor);
    }
  }

  private void loadSingleConfig(final boolean defaultProject, @Nonnull ComponentConfig config, final PluginDescriptor descriptor) {
    if (defaultProject && !config.isLoadForDefaultProject()) return;
    if (!myComponentManager.isComponentSuitable(config.options)) return;

    myComponentManager.registerComponent(config, descriptor);
  }

  public void loadComponentsConfiguration(final ComponentConfig[] components, final PluginDescriptor descriptor, final boolean defaultProject) {
    if (components == null) return;

    loadConfiguration(components, defaultProject, descriptor);
  }
}
