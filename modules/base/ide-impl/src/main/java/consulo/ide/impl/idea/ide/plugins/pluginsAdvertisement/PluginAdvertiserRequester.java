/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ide.plugins.pluginsAdvertisement;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.impl.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.idea.ide.plugins.RepositoryHelper;
import consulo.ide.impl.plugins.InstalledPluginsState;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2024-08-18
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class PluginAdvertiserRequester {
    private List<PluginDescriptor> myLoadedPluginDescriptors;

    private final Application myApplication;
    private final ApplicationConcurrency myApplicationConcurrency;

    @Inject
    public PluginAdvertiserRequester(Application application, ApplicationConcurrency applicationConcurrency) {
        myApplication = application;
        myApplicationConcurrency = applicationConcurrency;
    }

    @Nonnull
    public List<PluginDescriptor> getLoadedPluginDescriptors() {
        return myLoadedPluginDescriptors == null ? List.of() : myLoadedPluginDescriptors;
    }

    @Nonnull
    public CompletableFuture<List<PluginDescriptor>> doRequest() {
        UpdateSettings updateSettings = UpdateSettings.getInstance();
        if (!updateSettings.isEnable()) {
            return CompletableFuture.completedFuture(null);
        }

        if (myLoadedPluginDescriptors != null) {
            return CompletableFuture.completedFuture(myLoadedPluginDescriptors);
        }

        return CompletableFuture.supplyAsync(() -> {
            List<PluginDescriptor> pluginDescriptors = List.of();

            try {
                pluginDescriptors = RepositoryHelper.loadOnlyPluginsFromRepository(
                    null,
                    updateSettings.getChannel(),
                    EarlyAccessProgramManager.getInstance()
                );
            }
            catch (Exception ignored) {
            }

            if (myApplication.isDisposed()) {
                return List.of();
            }

            update(pluginDescriptors);

            return pluginDescriptors;
        }, myApplicationConcurrency.getExecutorService());
    }

    public void update(@Nullable List<PluginDescriptor> list) {
        myLoadedPluginDescriptors = ContainerUtil.isEmpty(list) ? null : list;

        if (list != null) {
            InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

            for (PluginDescriptor newPluginDescriptor : list) {
                final PluginDescriptor installed = PluginManager.findPlugin(newPluginDescriptor.getPluginId());
                if (installed != null) {
                    int state = StringUtil.compareVersionNumbers(newPluginDescriptor.getVersion(), installed.getVersion());

                    if (state > 0 &&
                        !PluginValidator.isIncompatible(newPluginDescriptor) &&
                        !pluginsState.getUpdatedPlugins().contains(newPluginDescriptor.getPluginId())) {
                        pluginsState.getOutdatedPlugins().add(newPluginDescriptor.getPluginId());
                    }
                }
            }
        }
    }
}
