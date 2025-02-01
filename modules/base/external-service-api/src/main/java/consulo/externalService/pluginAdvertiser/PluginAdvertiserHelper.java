/*
 * Copyright 2013-2025 consulo.io
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
package consulo.externalService.pluginAdvertiser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-01-30
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PluginAdvertiserHelper {
    @Nonnull
    static PluginAdvertiserHelper getInstance() {
        return Application.get().getInstance(PluginAdvertiserHelper.class);
    }

    record PluginsInfo(List<PluginDescriptor> allPlugins, Set<PluginDescriptor> featurePlugins) {
    }

    PluginsInfo getLoadedPlugins(ExtensionPreview extensionPreview);

    @RequiredUIAccess
    void showDialog(PluginsInfo pluginsInfo);
}
