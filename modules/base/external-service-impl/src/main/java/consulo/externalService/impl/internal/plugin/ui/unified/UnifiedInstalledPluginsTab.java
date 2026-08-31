/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedInstalledPluginsTab extends UnifiedPluginTab {
    @RequiredUIAccess
    public UnifiedInstalledPluginsTab() {
        super(true);
        reload();
    }

    @Override
    @RequiredUIAccess
    public void reload() {
        List<PluginDescriptor> descriptors = new ArrayList<>(PluginManager.getPlugins());
        descriptors.addAll(InstalledPluginsState.getInstance().getAllPlugins());
        descriptors.removeIf(descriptor -> PluginIds.isPlatformPlugin(descriptor.getPluginId()));

        setPlugins(descriptors);
    }
}
