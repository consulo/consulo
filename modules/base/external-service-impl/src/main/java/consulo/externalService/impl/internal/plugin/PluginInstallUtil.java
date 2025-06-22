/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.start.StartupActionScriptManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.plugin.ui.PluginTab;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author stathik
 * @since 2003-11-29
 */
public class PluginInstallUtil {
    private PluginInstallUtil() {
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    public static int showShutDownIDEADialog() {
        return showShutDownIDEADialog(ExternalServiceLocalize.titlePluginsChanged().get());
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    private static int showShutDownIDEADialog(String title) {
        return Messages.showYesNoDialog(
            ExternalServiceLocalize.messageIdeaShutdownRequired(Application.get().getName()).get(),
            title,
            ExternalServiceLocalize.actionShutdownText().get(),
            ExternalServiceLocalize.actionPostponeText().get(),
            Messages.getQuestionIcon()
        );
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    public static int showRestartIDEADialog() {
        return showRestartIDEADialog(ExternalServiceLocalize.titlePluginsChanged().get());
    }

    @Messages.YesNoResult
    @RequiredUIAccess
    private static int showRestartIDEADialog(String title) {
        return Messages.showYesNoDialog(
            ExternalServiceLocalize.messageIdeaRestartRequired(Application.get().getName()).get(),
            title,
            ExternalServiceLocalize.actionRestartText().get(),
            ExternalServiceLocalize.actionPostponeText().get(),
            UIUtil.getQuestionIcon()
        );
    }

    @RequiredUIAccess
    public static void shutdownOrRestartApp(String title) {
        ApplicationEx app = (ApplicationEx) Application.get();
        int response = app.isRestartCapable() ? showRestartIDEADialog(title) : showShutDownIDEADialog(title);
        if (response == Messages.YES) {
            app.restart(true);
        }
    }

    @Nonnull
    public static Set<PluginDescriptor> getPluginsForInstall(List<PluginDescriptor> pluginsToInstall, List<PluginDescriptor> allPlugins) {
        List<PluginId> pluginIds = new ArrayList<>();
        for (PluginDescriptor pluginNode : pluginsToInstall) {
            pluginIds.add(pluginNode.getPluginId());
        }

        Set<PluginDescriptor> toInstallAll = new LinkedHashSet<>();

        for (PluginDescriptor toInstall : pluginsToInstall) {
            Set<PluginNode> depends = new LinkedHashSet<>();
            collectDepends(toInstall, pluginIds, depends, allPlugins);

            toInstallAll.addAll(depends);
            toInstallAll.add(toInstall);
        }

        if (toInstallAll.isEmpty()) {
            throw new IllegalArgumentException("No plugins for install");
        }
        return toInstallAll;
    }

    private static void collectDepends(
        @Nonnull PluginDescriptor toInstall,
        @Nonnull List<PluginId> toInstallOthers,
        @Nonnull Set<PluginNode> depends,
        @Nonnull List<PluginDescriptor> repoPlugins
    ) {
        PluginId[] dependentPluginIds = toInstall.getDependentPluginIds();
        InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

        for (PluginId dependentPluginId : dependentPluginIds) {

            if (PluginManager.findPlugin(dependentPluginId) != null || toInstallOthers.contains(dependentPluginId)) {
                // ignore installed or installing plugins
                continue;
            }

            if (pluginsState.getInstalledPlugins().contains(dependentPluginId)) {
                // downloaded plugin
                continue;
            }

            PluginNode dependInRepo = (PluginNode) findDescriptionInRepo(dependentPluginId, repoPlugins);
            if (dependInRepo != null) {
                depends.add(dependInRepo);

                collectDepends(dependInRepo, toInstallOthers, depends, repoPlugins);
            }
        }
    }

    @Nullable
    private static PluginDescriptor findDescriptionInRepo(PluginId depPluginId, List<PluginDescriptor> allPlugins) {
        for (PluginDescriptor plugin : allPlugins) {
            if (plugin.getPluginId().equals(depPluginId)) {
                return plugin;
            }
        }
        return null;
    }

    public static void prepareToUninstall(PluginId pluginId) throws IOException {
        if (PluginManager.findPlugin(pluginId) != null) {
            // add command to delete the 'action script' file
            PluginDescriptor pluginDescriptor = PluginManager.findPlugin(pluginId);
            if (pluginDescriptor != null) {
                StartupActionScriptManager.ActionCommand deleteOld =
                    new StartupActionScriptManager.DeleteCommand(pluginDescriptor.getPath());
                StartupActionScriptManager.addActionCommand(deleteOld);
            }
            else {
                PluginTab.LOG.error("Plugin not found: " + pluginId);
            }
        }
    }
}
