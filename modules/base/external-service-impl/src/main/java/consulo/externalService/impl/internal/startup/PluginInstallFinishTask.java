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
package consulo.externalService.impl.internal.startup;

import consulo.application.progress.ProgressIndicator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.update.PluginDownloader;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeFinishTask;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.logging.Logger;

import java.util.List;

/**
 * Downloads the chosen plugins and leaves them to be unpacked at the next start, as every other install does.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class PluginInstallFinishTask implements StartupCustomizeFinishTask {
    private static final Logger LOG = Logger.getInstance(PluginInstallFinishTask.class);

    @Override
    public boolean run(StartupCustomizeContext context, ProgressIndicator indicator) {
        StartupCustomizePlugins plugins = new StartupCustomizePlugins(context.getRepositoryPlugins());

        List<PluginId> ids = context.getPluginsToInstall().stream().filter(id -> !StartupCustomizePlugins.isInstalled(id)).toList();

        boolean installed = false;
        for (int i = 0; i < ids.size(); i++) {
            indicator.checkCanceled();
            indicator.setFraction(i / (double) ids.size());

            PluginDescriptor descriptor = plugins.find(ids.get(i));
            if (descriptor == null) {
                continue;
            }

            try {
                PluginDownloader downloader = PluginDownloader.createDownloader(descriptor, false);
                downloader.download(indicator);
                downloader.install(indicator, true);
                installed = true;
            }
            catch (Exception e) {
                LOG.warn("Failed to install " + descriptor.getPluginId(), e);
                context.addProblem(ExternalServiceLocalize.startupCustomizeInstallFailed(descriptor.getName(), e.getMessage()));
            }
        }
        return installed;
    }
}
