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
import consulo.application.SystemHealthMonitor;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.pluginHistory.UpdateHistory;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2025-01-30
 */
@ExtensionImpl
public class ExperimentalPluginSystemHealthMonitor implements SystemHealthMonitor {
    private final Provider<UpdateHistory> myUpdateHistoryProvider;

    @Inject
    public ExperimentalPluginSystemHealthMonitor(Provider<UpdateHistory> updateHistoryProvider) {
        myUpdateHistoryProvider = updateHistoryProvider;
    }

    @Override
    public void check(@Nonnull Reporter reporter) {
        List<PluginDescriptor> plugins = PluginManager.getPlugins().stream()
            .filter(PluginDescriptor::isExperimental)
            .collect(Collectors.toList());
        if (plugins.isEmpty()) {
            return;
        }

        UpdateHistory updateHistory = myUpdateHistoryProvider.get();
        if (!updateHistory.isShowExperimentalWarning()) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(StringUtil.join(plugins, (it) -> "<b>" + it.getName() + "</b>", ", "));
        if (plugins.size() == 1) {
            builder.append(" is ");
        }
        else {
            builder.append(" are ");
        }
        builder.append(" experimental");

        reporter.warning(builder.toString(), "Got it!", () -> {
            updateHistory.setShowExperimentalWarning(false);
        });
    }
}
