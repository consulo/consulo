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
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ArrayListSet;
import com.intellij.util.ui.UIUtil;
import lombok.val;
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
public class PluginInstaller {

  private PluginInstaller() {
  }

  @Nullable("Will return null is download failed")
  public static Set<PluginNode> prepareToInstall(List<PluginNode> pluginsToInstall, List<IdeaPluginDescriptor> allPlugins) {
    final List<PluginId> pluginIds = new ArrayList<PluginId>();
    for (PluginNode pluginNode : pluginsToInstall) {
      pluginIds.add(pluginNode.getPluginId());
    }

    val result = new ArrayListSet<PluginNode>();

    for (final PluginNode pluginNode : pluginsToInstall) {
      try {
        Set<PluginNode> pluginNodes = prepareToInstall(pluginNode, pluginIds, allPlugins);
        if(pluginNodes == null) {
          return null;
        }
        else {
          result.addAll(pluginNodes);
        }
      }
      catch (IOException e) {
        String title = IdeBundle.message("title.plugin.error");
        Notifications.Bus.notify(new Notification(title, title, pluginNode.getName() + ": " + e.getMessage(), NotificationType.ERROR));
        return null;
      }
    }

    return result;
  }

  @Nullable("Will return null is download failed")
  private static Set<PluginNode> prepareToInstall(@NotNull PluginNode toInstall, @NotNull List<PluginId> toInstallAll,
                                            @NotNull List<IdeaPluginDescriptor> allPlugins)
          throws IOException {
    val depends = new ArrayListSet<PluginNode>();
    collectDepends(toInstall, toInstallAll, depends, allPlugins);

    val toDownloadList = new ArrayListSet<PluginNode>();
    if(!depends.isEmpty()) {

      UIUtil.invokeAndWaitIfNeeded(new Runnable() {
        @Override
        public void run() {
          String mergedIds = StringUtil.join(depends, new Function<PluginNode, String>() {
            @Override
            public String fun(PluginNode pluginNode) {
              return pluginNode.getName();
            }
          }, ", ");

          String title = IdeBundle.message("plugin.manager.dependencies.detected.title");
          String message = IdeBundle.message("plugin.manager.dependencies.detected.message", depends.size(), mergedIds);
          if(Messages.showYesNoDialog(message, title, Messages.getWarningIcon()) == Messages.YES) {
            toDownloadList.addAll(depends);
          }
        }
      });
    }

    toDownloadList.add(toInstall);

    for (PluginNode pluginNode : toDownloadList) {
      PluginDownloader downloader = null;
      final String repositoryName = pluginNode.getRepositoryName();
      if (repositoryName != null) {
        try {
          final List<PluginDownloader> downloaders = new ArrayList<PluginDownloader>();
          if (!UpdateChecker.checkPluginsHost(repositoryName, downloaders)) {
            return null;
          }
          for (PluginDownloader pluginDownloader : downloaders) {
            if (Comparing.strEqual(pluginDownloader.getPluginId(), pluginNode.getPluginId().getIdString())) {
              downloader = pluginDownloader;
              break;
            }
          }
          if (downloader == null) return null;
        }
        catch (Exception e) {
          return null;
        }
      }
      else {
        downloader = PluginDownloader.createDownloader(pluginNode);
      }
      if (downloader.prepareToInstall(ProgressManager.getInstance().getProgressIndicator())) {
        downloader.install();
        pluginNode.setStatus(PluginNode.STATUS_DOWNLOADED);
      }
      else {
        return null;
      }
    }
    return toDownloadList;
  }

  private static void collectDepends(@NotNull IdeaPluginDescriptor toInstall,
                                     @NotNull List<PluginId> toInstallOthers,
                                     @NotNull Set<PluginNode> depends,
                                     @NotNull List<IdeaPluginDescriptor> repoPlugins) {
    PluginId[] dependentPluginIds = toInstall.getDependentPluginIds();

    for (PluginId dependentPluginId : dependentPluginIds) {

      if (PluginManager.isPluginInstalled(dependentPluginId) || toInstallOthers.contains(dependentPluginId)) {
        // ignore installed or installing plugins
        continue;
      }

      PluginNode depPlugin = new PluginNode(dependentPluginId);
      depPlugin.setSize("-1");
      depPlugin.setName(dependentPluginId.getIdString()); //prevent from exceptions

      IdeaPluginDescriptor dependInRepo = findDescriptionInRepo(dependentPluginId, repoPlugins);
      if (dependInRepo != null) {
        depends.add(depPlugin);

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
