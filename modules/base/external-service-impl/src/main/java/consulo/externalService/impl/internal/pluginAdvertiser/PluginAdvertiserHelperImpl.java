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
import consulo.container.plugin.PluginManager;
import consulo.externalService.pluginAdvertiser.PluginAdvertiserHelper;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author VISTALL
 * @since 2025-01-30
 */
@ServiceImpl
@Singleton
public class PluginAdvertiserHelperImpl implements PluginAdvertiserHelper {
    private final PluginAdvertiserRequester myPluginAdvertiserRequester;

    @Inject
    public PluginAdvertiserHelperImpl(PluginAdvertiserRequester pluginAdvertiserRequester) {
        myPluginAdvertiserRequester = pluginAdvertiserRequester;
    }

    @Override
    public CompletableFuture<PluginsInfo> findPluginsForSuggest(ExtensionPreview extensionPreview) {
        return myPluginAdvertiserRequester.doRequest().thenApply(allPlugins -> {
            Set<PluginDescriptor> featurePlugins = PluginAdvertiserImpl.findImpl(allPlugins, extensionPreview);

            Set<PluginDescriptor> featuredPlugins = featurePlugins
                .stream()
                .filter(it -> PluginManager.findPlugin(it.getPluginId()) == null)
                .collect(Collectors.toSet());

            return new PluginsInfo(allPlugins, featuredPlugins);
        });
    }

    @Override
    public PluginsInfo getLoadedPlugins(ExtensionPreview extensionPreview) {
        List<PluginDescriptor> allPlugins = myPluginAdvertiserRequester.getLoadedPluginDescriptors();
        Set<PluginDescriptor> featurePlugins = PluginAdvertiserImpl.findImpl(allPlugins, extensionPreview);
        return new PluginsInfo(allPlugins, featurePlugins);
    }

    @Override
    @RequiredUIAccess
    public void showDialog(PluginsInfo pluginsInfo) {
        PreloadedPluginsAdvertiserDialog advertiserDialog =
            new PreloadedPluginsAdvertiserDialog(null, pluginsInfo.allPlugins(), new ArrayList<>(pluginsInfo.featurePlugins()));
        advertiserDialog.show();
    }

    @RequiredUIAccess
    @Override
    public void showDialogForExtension(ExtensionPreview preview) {
        new WaitingPluginsAdvertiserDialog(null, preview, this).showAsync();
    }
}
