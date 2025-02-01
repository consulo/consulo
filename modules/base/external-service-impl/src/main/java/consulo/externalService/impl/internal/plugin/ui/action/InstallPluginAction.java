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

import consulo.application.Application;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginTab;
import consulo.externalService.impl.internal.plugin.ui.PluginsPanel;
import consulo.externalService.impl.internal.update.PlatformOrPluginDialog;
import consulo.externalService.impl.internal.update.PlatformOrPluginNode;
import consulo.externalService.impl.internal.update.PlatformOrPluginUpdateResult;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author lloix
 */
public class InstallPluginAction {
    @RequiredUIAccess
    public static void install(@Nullable PluginsPanel pluginsPanel,
                               @Nonnull PluginTab pluginTab,
                               @Nullable Project project,
                               @Nullable final Runnable onSuccess) {
        PluginDescriptor descr = pluginTab.getSelectedPlugin();
        if (descr == null) {
            return;
        }

        PluginTab installed = pluginTab.getInstalled();
        PluginTab available = pluginTab.getAvailable();

        final List<PluginDescriptor> list = new ArrayList<>();
        PluginNode pluginNode = null;
        if (descr instanceof PluginNode) {
            pluginNode = (PluginNode) descr;
        }
        else if (descr.isLoaded()) {
            final PluginId pluginId = descr.getPluginId();
            pluginNode = new PluginNode(pluginId);
            pluginNode.setName(descr.getName());
            pluginNode.setExperimental(descr.isExperimental());
            pluginNode.addDependency(descr.getDependentPluginIds());
            pluginNode.addOptionalDependency(descr.getOptionalDependentPluginIds());
            pluginNode.setSize("-1");
        }

        if (pluginNode != null) {
            if (pluginNode.isExperimental()) {
                if (Messages.showOkCancelDialog(
                    "Are you sure install experimental plugin? Plugin can make IDE unstable, and may not implement expected features",
                    Application.get().getName().get(),
                    Messages.getWarningIcon()
                ) != Messages.OK) {
                    return;
                }
            }

            list.add(pluginNode);
        }

        final Consumer<Collection<PluginDescriptor>> afterCallback = pluginNodes -> {
            if (pluginNodes.isEmpty()) {
                return;
            }

            UIUtil.invokeLaterIfNeeded(() -> installedPluginsToModel(pluginsPanel, pluginNodes));

            if (!installed.isDisposed()) {
                UIUtil.invokeLaterIfNeeded(() -> {
                    installed.setRequireShutdown(true);
                });
            }
            else {
                boolean needToRestart = false;
                for (PluginDescriptor node : pluginNodes) {
                    final PluginDescriptor pluginDescriptor = PluginManager.findPlugin(node.getPluginId());
                    if (pluginDescriptor == null || pluginDescriptor.isEnabled()) {
                        needToRestart = true;
                        break;
                    }
                }

                if (needToRestart) {
                    PluginTab.notifyPluginsWereInstalled(pluginNodes, null);
                }
            }

            if (onSuccess != null) {
                onSuccess.run();
            }
        };

        downloadAndInstallPlugins(project, list, available.getAvailable().getPluginList().getAll(), afterCallback);
    }

    @RequiredUIAccess
    public static boolean downloadAndInstallPlugins(@Nullable Project project,
                                                    @Nonnull final List<PluginDescriptor> toInstall,
                                                    @Nonnull final List<PluginDescriptor> allPlugins,
                                                    @Nullable final Consumer<Collection<PluginDescriptor>> afterCallback) {
        Set<PluginDescriptor> pluginsForInstallWithDependencies = PluginInstallUtil.getPluginsForInstall(toInstall, allPlugins);

        List<PlatformOrPluginNode> remap = pluginsForInstallWithDependencies.stream().map(x -> new PlatformOrPluginNode(x.getPluginId(), null, x)).collect(Collectors.toList());
        PlatformOrPluginUpdateResult result = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.PLUGIN_INSTALL, remap);
        Predicate<PluginId> greenNodeStrategy = pluginId -> {
            // do not mark target node as green, only depend
            for (PluginDescriptor node : toInstall) {
                if (node.getPluginId().equals(pluginId)) {
                    return false;
                }
            }
            return true;
        };
        PlatformOrPluginDialog dialog = new PlatformOrPluginDialog(project, result, greenNodeStrategy, afterCallback);
        if (pluginsForInstallWithDependencies.size() == toInstall.size()) {
            dialog.doOKAction();
            return true;
        }
        else {
            return dialog.showAndGet();
        }
    }
    private static void installedPluginsToModel(@Nullable PluginsPanel pluginsPanel,
                                                Collection<PluginDescriptor> list) {
        for (PluginDescriptor pluginNode : list) {
            final PluginId id = pluginNode.getPluginId();
            final InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

            pluginsState.getInstalledPlugins().add(id);
            pluginsState.getOutdatedPlugins().remove(id);
        }

        if (pluginsPanel != null) {
            for (PluginDescriptor node : list) {
                pluginsPanel.appendOrUpdateDescriptor(node);
            }
        }
    }
}
