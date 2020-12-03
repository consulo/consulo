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

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.plugins.InstalledPluginsState;
import consulo.ide.updateSettings.impl.PlatformOrPluginDialog;
import consulo.ide.updateSettings.impl.PlatformOrPluginNode;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateResult;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author lloix
 */
public class InstallPluginAction extends AnAction implements DumbAware {
  private static final Set<PluginDescriptor> ourInstallingNodes = new HashSet<>();

  private final PluginManagerMain myCurrentPage;

  public InstallPluginAction(PluginManagerMain currentPage) {
    super(IdeLocalize.actionDownloadAndInstallPlugin(), IdeLocalize.actionDownloadAndInstallPlugin(), AllIcons.Actions.Install);

    myCurrentPage = currentPage;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    PluginDescriptor[] selection = getPluginTable().getSelectedObjects();
    boolean enabled = (selection != null);

    if (enabled) {
      for (PluginDescriptor descr : selection) {
        presentation.setTextValue(IdeLocalize.actionDownloadAndInstallPlugin());
        presentation.setDescriptionValue(IdeLocalize.actionDownloadAndInstallPlugin());
        enabled &= !ourInstallingNodes.contains(descr);
        if (descr instanceof PluginNode) {
          enabled &= !PluginManagerColumnInfo.isDownloaded(descr);
          if (((PluginNode)descr).getStatus() == PluginNode.STATUS_INSTALLED) {
            presentation.setTextValue(IdeLocalize.actionUpdatePlugin());
            presentation.setDescriptionValue(IdeLocalize.actionUpdatePlugin());
            enabled &= InstalledPluginsTableModel.hasNewerVersion(descr.getPluginId());
          }
        }
        else if (descr.isLoaded()) {
          presentation.setTextValue(IdeLocalize.actionUpdatePlugin());
          presentation.setDescriptionValue(IdeLocalize.actionUpdatePlugin());
          PluginId id = descr.getPluginId();
          enabled = enabled && InstalledPluginsTableModel.hasNewerVersion(id);
        }
      }
    }

    presentation.setEnabled(enabled);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    install(e.getProject(), null);
  }

  @RequiredUIAccess
  public void install(@Nullable Project project, @Nullable final Runnable onSuccess) {
    PluginDescriptor[] selection = getPluginTable().getSelectedObjects();
    PluginManagerMain installed = myCurrentPage.getInstalled();
    PluginManagerMain available = myCurrentPage.getAvailable();

    final List<PluginDescriptor> list = new ArrayList<>();
    for (PluginDescriptor descr : selection) {
      PluginNode pluginNode = null;
      if (descr instanceof PluginNode) {
        pluginNode = (PluginNode)descr;
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
          if (Messages.showOkCancelDialog("Are you sure install experimental plugin? Plugin can make IDE unstable, and may not implement expected features", "Consulo", Messages.getWarningIcon()) !=
              Messages.OK) {
            return;
          }
        }

        list.add(pluginNode);
        ourInstallingNodes.add(pluginNode);
      }
      final InstalledPluginsTableModel pluginsModel = (InstalledPluginsTableModel)installed.getPluginsModel();
      final Set<PluginDescriptor> disabled = new HashSet<>();
      final Set<PluginDescriptor> disabledDependants = new HashSet<>();
      for (PluginDescriptor node : list) {
        final PluginId pluginId = node.getPluginId();
        if (pluginsModel.isDisabled(pluginId)) {
          disabled.add(node);
        }
        for (PluginId dependantId : node.getDependentPluginIds()) {
          final PluginDescriptor pluginDescriptor = PluginManager.getPlugin(dependantId);
          if (pluginDescriptor != null && pluginsModel.isDisabled(dependantId)) {
            disabledDependants.add(pluginDescriptor);
          }
        }
      }
      if (suggestToEnableInstalledPlugins(pluginsModel, disabled, disabledDependants, list)) {
        installed.setRequireShutdown(true);
      }
    }
    final Consumer<Collection<PluginDescriptor>> afterCallback = pluginNodes -> {
      if (pluginNodes.isEmpty()) {
        return;
      }
      UIUtil.invokeLaterIfNeeded(() -> installedPluginsToModel(pluginNodes));

      if (!installed.isDisposed()) {
        UIUtil.invokeLaterIfNeeded(() -> {
          getPluginTable().updateUI();
          installed.setRequireShutdown(true);
        });
      }
      else {
        boolean needToRestart = false;
        for (PluginDescriptor node : pluginNodes) {
          final PluginDescriptor pluginDescriptor = PluginManager.getPlugin(node.getPluginId());
          if (pluginDescriptor == null || pluginDescriptor.isEnabled()) {
            needToRestart = true;
            break;
          }
        }

        if (needToRestart) {
          PluginManagerMain.notifyPluginsWereInstalled(pluginNodes, null);
        }
      }

      if (onSuccess != null) {
        onSuccess.run();
      }

      ourInstallingNodes.removeAll(pluginNodes);
    };
    downloadAndInstallPlugins(project, list, available.getPluginsModel().getAllPlugins(), afterCallback);
  }

  public static boolean downloadAndInstallPlugins(@Nullable Project project,
                                                  @Nonnull final List<PluginDescriptor> toInstall,
                                                  @Nonnull final List<PluginDescriptor> allPlugins,
                                                  @Nullable final Consumer<Collection<PluginDescriptor>> afterCallback) {
    Set<PluginDescriptor> pluginsForInstallWithDependencies = PluginInstallUtil.getPluginsForInstall(toInstall, allPlugins);

    List<PlatformOrPluginNode> remap = pluginsForInstallWithDependencies.stream().map(x -> new PlatformOrPluginNode(x.getPluginId(), null, x)).collect(Collectors.toList());
    PlatformOrPluginUpdateResult result = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResult.Type.PLUGIN_INSTALL, remap);
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

  private static boolean suggestToEnableInstalledPlugins(final InstalledPluginsTableModel pluginsModel,
                                                         final Set<PluginDescriptor> disabled,
                                                         final Set<PluginDescriptor> disabledDependants,
                                                         final List<PluginDescriptor> list) {
    if (!disabled.isEmpty() || !disabledDependants.isEmpty()) {
      String message = "";
      if (disabled.size() == 1) {
        message += "Updated plugin '" + disabled.iterator().next().getName() + "' is disabled.";
      }
      else if (!disabled.isEmpty()) {
        message += "Updated plugins " + StringUtil.join(disabled, PluginDescriptor::getName, ", ") + " are disabled.";
      }

      if (!disabledDependants.isEmpty()) {
        message += "<br>";
        message += "Updated plugin" + (list.size() > 1 ? "s depend " : " depends ") + "on disabled";
        if (disabledDependants.size() == 1) {
          message += " plugin '" + disabledDependants.iterator().next().getName() + "'.";
        }
        else {
          message += " plugins " + StringUtil.join(disabledDependants, PluginDescriptor::getName, ", ") + ".";
        }
      }
      message += " Disabled plugins and plugins which depends on disabled plugins won't be activated after restart.";

      int result;
      if (!disabled.isEmpty() && !disabledDependants.isEmpty()) {
        result = Messages.showYesNoCancelDialog(XmlStringUtil.wrapInHtml(message), CommonBundle.getWarningTitle(), "Enable all", "Enable updated plugin" + (disabled.size() > 1 ? "s" : ""),
                                                CommonBundle.getCancelButtonText(), Messages.getQuestionIcon());
        if (result == Messages.CANCEL) return false;
      }
      else {
        message += "<br>Would you like to enable ";
        if (!disabled.isEmpty()) {
          message += "updated plugin" + (disabled.size() > 1 ? "s" : "");
        }
        else {
          //noinspection SpellCheckingInspection
          message += "plugin dependenc" + (disabledDependants.size() > 1 ? "ies" : "y");
        }
        message += "?</body></html>";
        result = Messages.showYesNoDialog(message, CommonBundle.getWarningTitle(), Messages.getQuestionIcon());
        if (result == Messages.NO) return false;
      }

      if (result == Messages.YES) {
        disabled.addAll(disabledDependants);
        pluginsModel.enableRows(disabled.toArray(new PluginDescriptor[disabled.size()]), true);
      }
      else if (result == Messages.NO && !disabled.isEmpty()) {
        pluginsModel.enableRows(disabled.toArray(new PluginDescriptor[disabled.size()]), true);
      }
      return true;
    }
    return false;
  }

  private void installedPluginsToModel(Collection<PluginDescriptor> list) {
    for (PluginDescriptor pluginNode : list) {
      final PluginId id = pluginNode.getPluginId();
      final InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

      pluginsState.getInstalledPlugins().add(id);
      pluginsState.getOutdatedPlugins().remove(id);
    }

    final InstalledPluginsTableModel installedPluginsModel = (InstalledPluginsTableModel)myCurrentPage.getInstalled().getPluginsModel();
    for (PluginDescriptor node : list) {
      installedPluginsModel.appendOrUpdateDescriptor(node);
    }
  }

  public PluginTable getPluginTable() {
    return myCurrentPage.getAvailable().getPluginTable();
  }
}
