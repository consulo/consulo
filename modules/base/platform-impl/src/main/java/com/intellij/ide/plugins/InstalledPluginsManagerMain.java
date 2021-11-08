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
import com.intellij.ide.DataManager;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.io.ZipUtil;
import com.intellij.util.ui.StatusText;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.fileTypes.ArchiveFileType;
import consulo.ide.plugins.PluginsPanel;
import consulo.localize.LocalizeValue;
import consulo.plugins.internal.PluginsLoader;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.fileChooser.FileChooser;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * User: anna
 */
public class InstalledPluginsManagerMain extends PluginManagerMain {
  private class InstallPluginFromDiskAction extends DumbAwareAction {
    private InstallPluginFromDiskAction() {
      super("Install plugin from disk...", null, AllIcons.Actions.Install);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, false, true, true, false, false) {
        @RequiredUIAccess
        @Override
        public boolean isFileSelectable(VirtualFile file) {
          return file.getFileType() instanceof ArchiveFileType;
        }
      };
      descriptor.setTitle("Choose Plugin File");
      descriptor.setDescription("JAR and ZIP archives are accepted");

      FileChooser.chooseFile(descriptor, null, getMainPanel(), null).doWhenDone(virtualFile -> {
        final File file = VfsUtilCore.virtualToIoFile(virtualFile);
        try {
          final PluginDescriptor pluginDescriptor = loadDescriptionFromJar(file);
          if (pluginDescriptor == null) {
            Messages.showErrorDialog("Fail to load plugin descriptor from file " + file.getName(), CommonBundle.getErrorTitle());
            return;
          }
          if (PluginManagerCore.isIncompatible(pluginDescriptor)) {
            Messages.showErrorDialog("Plugin " + pluginDescriptor.getName() + " is incompatible with current installation", CommonBundle.getErrorTitle());
            return;
          }
          final PluginDescriptor alreadyInstalledPlugin = PluginManager.getPlugin(pluginDescriptor.getPluginId());
          if (alreadyInstalledPlugin != null) {
            final File oldFile = alreadyInstalledPlugin.getPath();
            if (oldFile != null) {
              StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.DeleteCommand(oldFile));
            }
          }
          if (((InstalledPluginsTableModel)myPluginsModel).appendOrUpdateDescriptor(pluginDescriptor)) {
            PluginDownloader.install(file, file.getName(), false);
            select(pluginDescriptor.getPluginId());
            checkInstalledPluginDependencies(pluginDescriptor);
            setRequireShutdown(true);
          }
          else {
            Messages.showInfoMessage(getMainPanel(), "Plugin " + pluginDescriptor.getName() + " was already installed", CommonBundle.getWarningTitle());
          }
        }
        catch (IOException ex) {
          Messages.showErrorDialog(ex.getMessage(), CommonBundle.getErrorTitle());
        }
      });
    }
  }

  public InstalledPluginsManagerMain() {
    super();
    init();

    final StatusText emptyText = myPluginTable.getEmptyText();
    emptyText.setText("Nothing to show.");
    emptyText.appendText(" Click ");
    emptyText.appendText("View available plugins...", SimpleTextAttributes.LINK_ATTRIBUTES, new BrowseRepoListener());
    emptyText.appendText(" to view available plugins.");
  }

  @Override
  protected void addCustomFilters(Consumer<JComponent> adder) {
    LabelPopup showPopupLabel = new LabelPopup(LocalizeValue.localizeTODO("Show:"), this::createShowGroupPopup);

    adder.accept(showPopupLabel);

    updateShowPopupText(showPopupLabel);
  }

  private void updateShowPopupText(LabelPopup labelPopup) {
    labelPopup.setPrefixedText(LocalizeValue.of(((InstalledPluginsTableModel)myPluginsModel).getEnabledFilter()));
  }

  private DefaultActionGroup createShowGroupPopup(LabelPopup labelPopup) {
    final DefaultActionGroup gr = new DefaultActionGroup();
    for (final String enabledValue : InstalledPluginsTableModel.ENABLED_VALUES) {
      gr.addAction(new DumbAwareAction(enabledValue) {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          final PluginDescriptor selection = myPluginTable.getSelectedObject();
          final String filter = myFilter.getFilter().toLowerCase(Locale.ROOT);
          ((InstalledPluginsTableModel)myPluginsModel).setEnabledFilter(enabledValue, filter);
          if (selection != null) {
            select(selection.getPluginId());
          }

          updateShowPopupText(labelPopup);
        }
      });
    }
    return gr;
  }

  @Nullable
  public static PluginDescriptor loadDescriptionFromJar(final File file) throws IOException {
    PluginDescriptor descriptor = null;
    if (file.getName().endsWith(".zip")) {
      final File outputDir = FileUtil.createTempDirectory("plugin", "");
      try {
        ZipUtil.extract(file, outputDir, null);
        final File[] files = outputDir.listFiles();
        if (files != null && files.length == 1) {
          descriptor = PluginsLoader.loadPluginDescriptor(files[0]);
        }
      }
      finally {
        FileUtil.delete(outputDir);
      }
    }
    return descriptor;
  }


  private void checkInstalledPluginDependencies(PluginDescriptor pluginDescriptor) {
    final Set<PluginId> notInstalled = new HashSet<>();
    final Set<PluginId> disabledIds = new HashSet<>();
    final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
    final PluginId[] optionalDependentPluginIds = pluginDescriptor.getOptionalDependentPluginIds();
    for (PluginId id : dependentPluginIds) {
      if (ArrayUtil.find(optionalDependentPluginIds, id) > -1) continue;
      final boolean disabled = ((InstalledPluginsTableModel)myPluginsModel).isDisabled(id);
      final boolean enabled = ((InstalledPluginsTableModel)myPluginsModel).isEnabled(id);
      if (!enabled && !disabled) {
        notInstalled.add(id);
      }
      else if (disabled) {
        disabledIds.add(id);
      }
    }
    if (!notInstalled.isEmpty()) {
      Messages.showWarningDialog(
              "Plugin " + pluginDescriptor.getName() + " depends on unknown plugin" + (notInstalled.size() > 1 ? "s " : " ") + StringUtil.join(notInstalled, PluginId::toString, ", "),
              CommonBundle.getWarningTitle());
    }
    if (!disabledIds.isEmpty()) {
      final Set<PluginDescriptor> dependencies = new HashSet<>();
      for (PluginDescriptor ideaPluginDescriptor : myPluginsModel.getAllPlugins()) {
        if (disabledIds.contains(ideaPluginDescriptor.getPluginId())) {
          dependencies.add(ideaPluginDescriptor);
        }
      }
      final String disabledPluginsMessage = "disabled plugin" + (dependencies.size() > 1 ? "s " : " ");
      String message = "Plugin " +
                       pluginDescriptor.getName() +
                       " depends on " +
                       disabledPluginsMessage +
                       StringUtil.join(dependencies, PluginDescriptor::getName, ", ") +
                       ". Enable " +
                       disabledPluginsMessage.trim() +
                       "?";

      if (Messages.showOkCancelDialog(getMainPanel(), message, CommonBundle.getWarningTitle(), Messages.getWarningIcon()) == Messages.OK) {
        ((InstalledPluginsTableModel)myPluginsModel).enableRows(dependencies.toArray(new PluginDescriptor[dependencies.size()]), Boolean.TRUE);
      }
    }
  }

  @Override
  protected void propagateUpdates(List<PluginDescriptor> list) {
  }


  @Nonnull
  @Override
  protected PluginTable createTable() {
    myPluginsModel = new InstalledPluginsTableModel();

    PluginTable table = new PluginTable(myPluginsModel);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.registerKeyboardAction(e -> {
      final int column = InstalledPluginsTableModel.getCheckboxColumn();
      final int[] selectedRows = table.getSelectedRows();
      boolean currentlyMarked = true;
      for (final int selectedRow : selectedRows) {
        if (selectedRow < 0 || !table.isCellEditable(selectedRow, column)) {
          return;
        }
        final Boolean enabled = (Boolean)table.getValueAt(selectedRow, column);
        currentlyMarked &= enabled == null || enabled;
      }
      final PluginDescriptor[] selected = new PluginDescriptor[selectedRows.length];
      for (int i = 0, selectedLength = selected.length; i < selectedLength; i++) {
        selected[i] = myPluginsModel.getObjectAt(table.convertRowIndexToModel(selectedRows[i]));
      }
      ((InstalledPluginsTableModel)myPluginsModel).enableRows(selected, currentlyMarked ? Boolean.FALSE : Boolean.TRUE);
      table.repaint();
    }, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
    table.setExpandableItemsEnabled(false);
    return table;
  }

  @Override
  public ActionGroup getActionGroup() {
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.addAction(new RefreshAction());
    actionGroup.addAction(new InstallPluginFromDiskAction());
    return actionGroup;
  }

  @Override
  public boolean isModified() {
    final boolean modified = super.isModified();
    if (modified) return true;
    for (int i = 0; i < myPluginsModel.getRowCount(); i++) {
      final PluginDescriptor pluginDescriptor = myPluginsModel.getObjectAt(i);
      if (pluginDescriptor.isEnabled() != ((InstalledPluginsTableModel)myPluginsModel).isEnabled(pluginDescriptor.getPluginId())) {
        return true;
      }
    }
    for (PluginDescriptor descriptor : myPluginsModel.filtered) {
      if (descriptor.isEnabled() != ((InstalledPluginsTableModel)myPluginsModel).isEnabled(descriptor.getPluginId())) {
        return true;
      }
    }
    final List<String> disabledPlugins = consulo.container.plugin.PluginManager.getDisabledPlugins();
    for (Map.Entry<PluginId, Boolean> entry : ((InstalledPluginsTableModel)myPluginsModel).getEnabledMap().entrySet()) {
      final Boolean enabled = entry.getValue();
      if (enabled != null && !enabled && !disabledPlugins.contains(entry.getKey().toString())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String apply() {
    final String apply = super.apply();
    if (apply != null) return apply;
    for (int i = 0; i < myPluginTable.getRowCount(); i++) {
      final PluginDescriptor pluginDescriptor = myPluginsModel.getObjectAt(i);
      final Boolean enabled = (Boolean)myPluginsModel.getValueAt(i, InstalledPluginsTableModel.getCheckboxColumn());
      pluginDescriptor.setEnabled(enabled != null && enabled);
    }
    for (PluginDescriptor descriptor : myPluginsModel.filtered) {
      descriptor.setEnabled(((InstalledPluginsTableModel)myPluginsModel).isEnabled(descriptor.getPluginId()));
    }

    final ArrayList<String> ids = new ArrayList<>();
    for (Map.Entry<PluginId, Boolean> entry : ((InstalledPluginsTableModel)myPluginsModel).getEnabledMap().entrySet()) {
      final Boolean value = entry.getValue();
      if (value != null && !value) {
        ids.add(entry.getKey().getIdString());
      }
    }

    consulo.container.plugin.PluginManager.replaceDisabledPlugins(ids);

    return null;
  }

  @Override
  protected String canApply() {
    final Map<PluginId, Set<PluginId>> dependentToRequiredListMap = new HashMap<>(((InstalledPluginsTableModel)myPluginsModel).getDependentToRequiredListMap());
    for (Iterator<PluginId> iterator = dependentToRequiredListMap.keySet().iterator(); iterator.hasNext(); ) {
      PluginId item = iterator.next();
      // ignore
      iterator.remove();
    }
    if (!dependentToRequiredListMap.isEmpty()) {
      return "<html><body style=\"padding: 5px;\">Unable to apply changes: plugin" +
             (dependentToRequiredListMap.size() == 1 ? " " : "s ") +
             StringUtil.join(dependentToRequiredListMap.keySet(), pluginId -> {
               final PluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
               return "\"" + (ideaPluginDescriptor != null ? ideaPluginDescriptor.getName() : pluginId.getIdString()) + "\"";
             }, ", ") +
             " won't be able to load.</body></html>";
    }
    return super.canApply();
  }

  private class BrowseRepoListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      PluginsPanel pluginsPanel = DataManager.getInstance().getDataContext(getMainPanel()).getData(PluginsPanel.KEY);

      if (pluginsPanel != null) {
        pluginsPanel.select(getAvailable());
      }
    }
  }
}
