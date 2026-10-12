/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalService.impl.internal.plugin.ui.action;

import consulo.container.internal.plugin.PluginDescriptorImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.logging.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class UninstallPluginAction {
    private static final Logger LOG = Logger.getInstance(UninstallPluginAction.class);

    public static boolean uninstallPlugin(PluginDescriptor descriptor) {
        PluginId pluginId = descriptor.getPluginId();

        try {
            PluginInstallUtil.prepareToUninstall(pluginId);

            if (descriptor instanceof PluginDescriptorImpl descriptorImpl) {
                descriptorImpl.setDeleted(true);
            }

            Set<PluginId> installedPlugins = InstalledPluginsState.getInstance().getInstalledPlugins();
            while (installedPlugins.contains(pluginId)) {
                installedPlugins.remove(pluginId);
            }

            return true;
        }
        catch (IOException e) {
            LOG.error(e);
        }

        return false;
    }
}
