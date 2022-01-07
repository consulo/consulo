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

import com.intellij.ide.IdeBundle;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.ArrayListSet;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.plugins.InstalledPluginsState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author stathik
 * @since Nov 29, 2003
 */
public class PluginInstallUtil {
  private static final String POSTPONE = "&Postpone";

  private PluginInstallUtil() {
  }

  @Messages.YesNoResult
  public static int showShutDownIDEADialog() {
    return showShutDownIDEADialog(IdeBundle.message("title.plugins.changed"));
  }

  @Messages.YesNoResult
  private static int showShutDownIDEADialog(final String title) {
    String message = IdeBundle.message("message.idea.shutdown.required", ApplicationNamesInfo.getInstance().getFullProductName());
    return Messages.showYesNoDialog(message, title, "Shut Down", POSTPONE, Messages.getQuestionIcon());
  }

  @Messages.YesNoResult
  public static int showRestartIDEADialog() {
    return showRestartIDEADialog(IdeBundle.message("title.plugins.changed"));
  }

  @Messages.YesNoResult
  private static int showRestartIDEADialog(final String title) {
    String message = IdeBundle.message("message.idea.restart.required", ApplicationNamesInfo.getInstance().getFullProductName());
    return Messages.showYesNoDialog(message, title, "Restart", POSTPONE, Messages.getQuestionIcon());
  }

  public static void shutdownOrRestartApp(String title) {
    final ApplicationEx app = (ApplicationEx)Application.get();
    int response = app.isRestartCapable() ? showRestartIDEADialog(title) : showShutDownIDEADialog(title);
    if (response == Messages.YES) app.restart(true);
  }

  @Nonnull
  public static Set<PluginDescriptor> getPluginsForInstall(List<PluginDescriptor> pluginsToInstall, List<PluginDescriptor> allPlugins) {
    final List<PluginId> pluginIds = new ArrayList<>();
    for (PluginDescriptor pluginNode : pluginsToInstall) {
      pluginIds.add(pluginNode.getPluginId());
    }

    final Set<PluginDescriptor> toInstallAll = new ArrayListSet<>();

    for (PluginDescriptor toInstall : pluginsToInstall) {
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

  private static void collectDepends(@Nonnull PluginDescriptor toInstall,
                                     @Nonnull List<PluginId> toInstallOthers,
                                     @Nonnull Set<PluginNode> depends,
                                     @Nonnull List<PluginDescriptor> repoPlugins) {
    PluginId[] dependentPluginIds = toInstall.getDependentPluginIds();
    InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

    for (PluginId dependentPluginId : dependentPluginIds) {

      if (PluginManager.isPluginInstalled(dependentPluginId) || toInstallOthers.contains(dependentPluginId)) {
        // ignore installed or installing plugins
        continue;
      }

      if (pluginsState.getInstalledPlugins().contains(dependentPluginId)) {
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
  private static PluginDescriptor findDescriptionInRepo(PluginId depPluginId, List<PluginDescriptor> allPlugins) {
    for (PluginDescriptor plugin : allPlugins) {
      if (plugin.getPluginId().equals(depPluginId)) {
        return plugin;
      }
    }
    return null;
  }

  public static void prepareToUninstall(PluginId pluginId) throws IOException {
    if (PluginManager.isPluginInstalled(pluginId)) {
      // add command to delete the 'action script' file
      PluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
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
