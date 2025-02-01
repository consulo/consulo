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
package consulo.externalService.impl.internal.plugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.SystemHealthMonitor;
import consulo.application.plugin.PluginActionListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.ui.action.UninstallPluginAction;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2025-01-30
 */
@ExtensionImpl
public class ObsoletePluginSystemHealthMonitor implements SystemHealthMonitor {
    @Override
    public void check(@Nonnull Reporter reporter) {
        List<PluginDescriptor> plugins = PluginManager.getPlugins().stream()
            .filter(it -> PluginIds.getObsoletePlugins().contains(it.getPluginId()))
            .collect(Collectors.toList());
        if (plugins.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(StringUtil.join(plugins, PluginDescriptor::getName, ", "));
        if (plugins.size() == 1) {
            builder.append(" is ");
        }
        else {
            builder.append(" are ");
        }
        builder.append(" obsolete. You can uninstall without consequences.");

        reporter.warning(builder.toString(), "Uninstall...", () -> {
            List<PluginId> pluginIds = new ArrayList<>();
            for (PluginDescriptor plugin : plugins) {
                if (UninstallPluginAction.uninstallPlugin(plugin, null)) {
                    pluginIds.add(plugin.getPluginId());
                }
            }

            if (!pluginIds.isEmpty()) {
                Application.get().getMessageBus()
                    .syncPublisher(PluginActionListener.class)
                    .pluginsUninstalled(pluginIds.toArray(PluginId[]::new));
            }
        });
    }
}
