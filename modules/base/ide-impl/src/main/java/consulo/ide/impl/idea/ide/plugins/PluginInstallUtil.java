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
package consulo.ide.impl.idea.ide.plugins;

import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.impl.idea.ide.startup.StartupActionScriptManager;
import consulo.ide.impl.idea.util.containers.ArrayListSet;
import consulo.ide.impl.plugins.InstalledPluginsState;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.ex.awt.Messages;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    return showShutDownIDEADialog(IdeLocalize.titlePluginsChanged().get());
  }

  @Messages.YesNoResult
  private static int showShutDownIDEADialog(final String title) {
    String message = IdeLocalize.messageIdeaShutdownRequired(Application.get().getName()).get();
    return Messages.showYesNoDialog(
      message,
      title,
      "Shut Down",
      POSTPONE,
      Messages.getQuestionIcon()
    );
  }

  @Messages.YesNoResult
  public static int showRestartIDEADialog() {
    return showRestartIDEADialog(IdeLocalize.titlePluginsChanged().get());
  }

  @Messages.YesNoResult
  private static int showRestartIDEADialog(final String title) {
    LocalizeValue message = IdeLocalize.messageIdeaRestartRequired(Application.get().getName());
    return Messages.showYesNoDialog(message.get(), title, "Restart", POSTPONE, Messages.getQuestionIcon());
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
