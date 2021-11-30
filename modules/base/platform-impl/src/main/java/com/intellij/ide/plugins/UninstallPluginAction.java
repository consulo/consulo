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
package com.intellij.ide.plugins;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.plugins.PluginActionListener;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class UninstallPluginAction extends AnAction implements DumbAware {
  private final PluginTable pluginTable;
  private final PluginManagerMain host;

  public UninstallPluginAction(PluginManagerMain mgr, PluginTable table) {
    super(IdeBundle.message("action.uninstall.plugin"), IdeBundle.message("action.uninstall.plugin"), AllIcons.Actions.Uninstall);

    pluginTable = table;
    host = mgr;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    if (!pluginTable.isShowing()) {
      presentation.setEnabled(false);
      return;
    }
    PluginDescriptor[] selection = pluginTable.getSelectedObjects();
    boolean enabled = (selection != null);

    if (enabled) {
      for (PluginDescriptor descriptor : selection) {
        if (descriptor.isLoaded()) {
          if (descriptor.isDeleted() || PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
            enabled = false;
            break;
          }
        }
        if (descriptor instanceof PluginNode) {
          enabled = false;
          break;
        }
      }
    }
    presentation.setEnabled(enabled);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    uninstall(host, pluginTable.getSelectedObjects());
    pluginTable.updateUI();
  }

  public static void uninstall(PluginManagerMain host, PluginDescriptor... selection) {
    String message;

    if (selection.length == 1) {
      message = IdeBundle.message("prompt.uninstall.plugin", selection[0].getName());
    }
    else {
      message = IdeBundle.message("prompt.uninstall.several.plugins", selection.length);
    }
    if (Messages.showYesNoDialog(host.getMainPanel(), message, IdeBundle.message("title.plugin.uninstall"), Messages.getQuestionIcon()) != Messages.YES) return;

    for (PluginDescriptor descriptor : selection) {

      boolean actualDelete = true;

      //  Get the list of plugins which depend on this one. If this list is
      //  not empty - issue warning instead of simple prompt.
      List<PluginDescriptor> dependant = host.getDependentList(descriptor);
      if (dependant.size() > 0) {
        message = IdeBundle.message("several.plugins.depend.on.0.continue.to.remove", descriptor.getName());
        actualDelete = (Messages.showYesNoDialog(host.getMainPanel(), message, IdeBundle.message("title.plugin.uninstall"), Messages.getQuestionIcon()) == Messages.YES);
      }

      if (actualDelete) {
        uninstallPlugin(descriptor, host);
      }
    }
  }

  public static void uninstallPlugin(PluginDescriptor descriptor, @Nullable PluginManagerMain host) {
    PluginId pluginId = descriptor.getPluginId();

    Application.get().getMessageBus().syncPublisher(PluginActionListener.TOPIC).pluginUninstalled(pluginId);

    try {
      PluginInstallUtil.prepareToUninstall(pluginId);

      PluginManagerCore.markAsDeletedPlugin(descriptor);
      
      final Set<PluginId> installedPlugins = InstalledPluginsState.getInstance().getInstalledPlugins();
      while (installedPlugins.contains(pluginId)) {
        installedPlugins.remove(pluginId);
      }
      if (host != null) {
        host.setRequireShutdown(descriptor.isEnabled());
      }
    }
    catch (IOException e) {
      PluginManagerMain.LOG.error(e);
    }
  }
}
