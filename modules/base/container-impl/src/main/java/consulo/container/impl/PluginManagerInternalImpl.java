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

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.internal.PluginManagerInternal;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PluginManagerInternalImpl implements PluginManagerInternal {
  @Nonnull
  @Override
  public Iterable<PluginDescriptor> getPlugins() {
    return PluginHolderModificator.getPlugins();
  }

  @Override
  public int getPluginsCount() {
    return PluginHolderModificator.getPlugins().size();
  }

  @Override
  public boolean isInitialized() {
    return PluginHolderModificator.isInitialized();
  }
}
