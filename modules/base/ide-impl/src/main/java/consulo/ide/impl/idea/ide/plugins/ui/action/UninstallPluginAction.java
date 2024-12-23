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
package consulo.ide.impl.idea.ide.plugins.ui.action;

import consulo.application.Application;
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.impl.idea.ide.plugins.PluginInstallUtil;
import consulo.ide.impl.idea.ide.plugins.ui.PluginTab;
import consulo.ide.impl.plugins.InstalledPluginsState;
import consulo.ide.impl.plugins.PluginActionListener;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class UninstallPluginAction {
    @RequiredUIAccess
    public static void uninstall(PluginTab host, PluginDescriptor... selection) {
        LocalizeValue message;

        if (selection.length == 1) {
            message = IdeLocalize.promptUninstallPlugin(selection[0].getName());
        }
        else {
            message = IdeLocalize.promptUninstallSeveralPlugins(selection.length);
        }
        if (Messages.showYesNoDialog(
            host.getMainPanel(),
            message.get(),
            IdeLocalize.titlePluginUninstall().get(),
            Messages.getQuestionIcon()
        ) != Messages.YES) {
            return;
        }

        List<PluginId> uninstalledPluginIds = new ArrayList<>();
        for (PluginDescriptor descriptor : selection) {
            boolean actualDelete = true;

            //  Get the list of plugins which depend on this one. If this list is
            //  not empty - issue warning instead of simple prompt.
            List<PluginDescriptor> dependant = host.getDependentList(descriptor);
            if (dependant.size() > 0) {
                message = IdeLocalize.severalPluginsDependOn0ContinueToRemove(descriptor.getName());
                actualDelete = (Messages.showYesNoDialog(
                    host.getMainPanel(),
                    message.get(),
                    IdeLocalize.titlePluginUninstall().get(),
                    Messages.getQuestionIcon()
                ) == Messages.YES);
            }

            if (actualDelete && uninstallPlugin(descriptor, host)) {
                uninstalledPluginIds.add(descriptor.getPluginId());
            }
        }

        if (!uninstalledPluginIds.isEmpty()) {
            Application.get().getMessageBus().syncPublisher(PluginActionListener.class).pluginsUninstalled(uninstalledPluginIds.toArray(PluginId[]::new));
        }
    }

    public static boolean uninstallPlugin(PluginDescriptor descriptor, @Nullable PluginTab host) {
        PluginId pluginId = descriptor.getPluginId();

        try {
            PluginInstallUtil.prepareToUninstall(pluginId);

            if (descriptor instanceof PluginDescriptorImpl) {
                ((PluginDescriptorImpl) descriptor).setDeleted(true);
            }

            final Set<PluginId> installedPlugins = InstalledPluginsState.getInstance().getInstalledPlugins();
            while (installedPlugins.contains(pluginId)) {
                installedPlugins.remove(pluginId);
            }

            if (host != null) {
                host.setRequireShutdown(descriptor.isEnabled());
            }

            return true;
        }
        catch (IOException e) {
            PluginTab.LOG.error(e);
        }

        return false;
    }
}
