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
package consulo.externalService.impl.internal.pluginAdvertiser;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2025-01-30
 */
@ServiceImpl
@Singleton
public class PluginAdvertiserHelperImpl implements PluginAdvertiserHelper {
    @Override
    public PluginsInfo getLoadedPlugins(ExtensionPreview extensionPreview) {
        List<PluginDescriptor> allPlugins = Application.get().getInstance(PluginAdvertiserRequester.class).getLoadedPluginDescriptors();
        Set<PluginDescriptor> featurePlugins = PluginAdvertiserImpl.findImpl(allPlugins, extensionPreview);
        return new PluginsInfo(allPlugins, featurePlugins);
    }

    @RequiredUIAccess
    @Override
    public void showDialog(PluginsInfo pluginsInfo) {
        final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, pluginsInfo.allPlugins(), new ArrayList<>(pluginsInfo.featurePlugins()));
        advertiserDialog.show();
    }
}
