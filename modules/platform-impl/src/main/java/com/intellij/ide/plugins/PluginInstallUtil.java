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
package com.intellij.ide.plugins;

import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.containers.ArrayListSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author stathik
 * @since Nov 29, 2003
 */
public class PluginInstallUtil {

  private PluginInstallUtil() {
  }

  @NotNull
  public static Set<IdeaPluginDescriptor> getPluginsForInstall(List<IdeaPluginDescriptor> pluginsToInstall, List<IdeaPluginDescriptor> allPlugins) {
    final List<PluginId> pluginIds = new ArrayList<>();
    for (IdeaPluginDescriptor pluginNode : pluginsToInstall) {
      pluginIds.add(pluginNode.getPluginId());
    }

    final Set<IdeaPluginDescriptor> toInstallAll = new ArrayListSet<>();

    for (IdeaPluginDescriptor toInstall : pluginsToInstall) {
      Set<PluginNode> depends = new ArrayListSet<>();
      collectDepends(toInstall, pluginIds, depends, allPlugins);

      toInstallAll.addAll(depends);
      toInstallAll.add(toInstall);
    }

    if(toInstallAll.isEmpty()) {
      throw new IllegalArgumentException("No plugins for install");
    }
    return toInstallAll;
  }

  private static void collectDepends(@NotNull IdeaPluginDescriptor toInstall,
                                     @NotNull List<PluginId> toInstallOthers,
                                     @NotNull Set<PluginNode> depends,
                                     @NotNull List<IdeaPluginDescriptor> repoPlugins) {
    PluginId[] dependentPluginIds = toInstall.getDependentPluginIds();
    PluginManagerUISettings pluginManagerUISettings = PluginManagerUISettings.getInstance();

    for (PluginId dependentPluginId : dependentPluginIds) {

      if (PluginManager.isPluginInstalled(dependentPluginId) || toInstallOthers.contains(dependentPluginId)) {
        // ignore installed or installing plugins
        continue;
      }

      if (pluginManagerUISettings.getInstalledPlugins().contains(dependentPluginId.getIdString())) {
        // downloaded plugin
        continue;
      }

      PluginNode dependInRepo = (PluginNode)findDescriptionInRepo(dependentPluginId, repoPlugins);
      if (dependInRepo != null) {
        depends.add(dependInRepo);

        collectDepends(dependInRepo, toInstallOthers, depends, repoPlugins);
      }
    }
  }

  @Nullable
  private static IdeaPluginDescriptor findDescriptionInRepo(PluginId depPluginId, List<IdeaPluginDescriptor> allPlugins) {
    for (IdeaPluginDescriptor plugin : allPlugins) {
      if (plugin.getPluginId().equals(depPluginId)) {
        return plugin;
      }
    }
    return null;
  }

  public static void prepareToUninstall(PluginId pluginId) throws IOException {
    if (PluginManager.isPluginInstalled(pluginId)) {
      // add command to delete the 'action script' file
      IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
      if (pluginDescriptor != null) {
        StartupActionScriptManager.ActionCommand deleteOld = new StartupActionScriptManager.DeleteCommand(pluginDescriptor.getPath());
        StartupActionScriptManager.addActionCommand(deleteOld);
      }
      else {
        PluginManagerMain.LOG.error("Plugin not found: " + pluginId);
      }
    }
  }
}
