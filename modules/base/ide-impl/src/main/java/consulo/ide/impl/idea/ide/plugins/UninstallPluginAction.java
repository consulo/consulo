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
package consulo.ide.impl.idea.ide.plugins;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.ide.IdeBundle;
import consulo.ide.impl.plugins.InstalledPluginsState;
import consulo.ide.impl.plugins.PluginActionListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
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

    List<PluginId> uninstalledPluginIds = new ArrayList<>();
    for (PluginDescriptor descriptor : selection) {

      boolean actualDelete = true;

      //  Get the list of plugins which depend on this one. If this list is
      //  not empty - issue warning instead of simple prompt.
      List<PluginDescriptor> dependant = host.getDependentList(descriptor);
      if (dependant.size() > 0) {
        message = IdeBundle.message("several.plugins.depend.on.0.continue.to.remove", descriptor.getName());
        actualDelete = (Messages.showYesNoDialog(host.getMainPanel(), message, IdeBundle.message("title.plugin.uninstall"), Messages.getQuestionIcon()) == Messages.YES);
      }

      if (actualDelete && uninstallPlugin(descriptor, host)) {
        uninstalledPluginIds.add(descriptor.getPluginId());
      }
    }

    if (!uninstalledPluginIds.isEmpty()) {
      Application.get().getMessageBus().syncPublisher(PluginActionListener.class).pluginsUninstalled(uninstalledPluginIds.toArray(PluginId[]::new));
    }
  }

  public static boolean uninstallPlugin(PluginDescriptor descriptor, @Nullable PluginManagerMain host) {
    PluginId pluginId = descriptor.getPluginId();

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

      return true;
    }
    catch (IOException e) {
      PluginManagerMain.LOG.error(e);
    }

    return false;
  }
}
