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

import com.intellij.ide.plugins.sorters.SortByDownloadsAction;
import com.intellij.ide.plugins.sorters.SortByRatingAction;
import com.intellij.ide.plugins.sorters.SortByUpdatedAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.container.plugin.PluginDescriptor;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.TreeSet;

/**
 * User: anna
 */
public class AvailablePluginsManagerMain extends PluginManagerMain {
  public static final String MANAGE_REPOSITORIES = "Manage repositories...";
  public static final String N_A = "N/A";

  private PluginManagerMain installed;

  public AvailablePluginsManagerMain(PluginManagerMain installed, PluginManagerUISettings uiSettings) {
    super(uiSettings);
    this.installed = installed;
    init();
    myActionsPanel.setVisible(false);
    /*final JButton manageRepositoriesBtn = new JButton(MANAGE_REPOSITORIES);
    if (myVendorFilter == null) {
      manageRepositoriesBtn.setMnemonic('m');
      manageRepositoriesBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (ShowSettingsUtil.getInstance().editConfigurable(myActionsPanel, new PluginHostsConfigurable())) {
            final List<String> pluginHosts = UpdateSettings.getInstance().getStoredPluginHosts();
            if (!pluginHosts.contains(((AvailablePluginsTableModel)pluginsModel).getRepository())) {
              ((AvailablePluginsTableModel)pluginsModel).setRepository(AvailablePluginsTableModel.ALL, myFilter.getFilter().toLowerCase());
            }
            loadAvailablePlugins();
          }
        }
      });
      myActionsPanel.add(manageRepositoriesBtn, BorderLayout.EAST);
    } */

   /* final JButton httpProxySettingsButton = new JButton(IdeBundle.message("button.http.proxy.settings"));
    httpProxySettingsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (HttpConfigurable.editConfigurable(getMainPanel())) {
          loadAvailablePlugins();
        }
      }
    });
    myActionsPanel.add(httpProxySettingsButton, BorderLayout.WEST);    */
    myPanelDescription.setVisible(false);
  }

  @Override
  protected JScrollPane createTable() {
    myPluginsModel = new AvailablePluginsTableModel();
    myPluginTable = new PluginTable(myPluginsModel);
    myPluginTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    //pluginTable.setColumnWidth(PluginManagerColumnInfo.COLUMN_DOWNLOADS, 70);
    //pluginTable.setColumnWidth(PluginManagerColumnInfo.COLUMN_DATE, 80);
    //pluginTable.setColumnWidth(PluginManagerColumnInfo.COLUMN_RATE, 80);

    return ScrollPaneFactory.createScrollPane(myPluginTable);
  }

  @Override
  protected DefaultActionGroup createSortersGroup() {
    final DefaultActionGroup group = super.createSortersGroup();
    group.addAction(new SortByDownloadsAction(myPluginTable, myPluginsModel));
    group.addAction(new SortByRatingAction(myPluginTable, myPluginsModel));
    group.addAction(new SortByUpdatedAction(myPluginTable, myPluginsModel));
    return group;
  }

  @Override
  public void reset() {
    UiNotifyConnector.doWhenFirstShown(getPluginTable(), this::loadAvailablePlugins);
    super.reset();
  }

  @Override
  protected PluginManagerMain getAvailable() {
    return this;
  }

  @Override
  protected PluginManagerMain getInstalled() {
    return installed;
  }

  @Override
  protected ActionGroup getActionGroup(boolean inToolbar) {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new RefreshAction());

    if (inToolbar) {
      actionGroup.add(new MyFilterCategoryAction());
    }
    else {
      actionGroup.add(createSortersGroup());
      actionGroup.add(AnSeparator.getInstance());
      actionGroup.add(new InstallPluginAction(getAvailable(), getInstalled()));
    }
    return actionGroup;
  }

  @Override
  protected void propagateUpdates(List<PluginDescriptor> list) {
    installed.modifyPluginsList(list);
  }

  private class MyFilterCategoryAction extends ComboBoxAction implements DumbAware {
    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      String category = ((AvailablePluginsTableModel)myPluginsModel).getCategory();
      if (category == null) {
        category = N_A;
      }
      e.getPresentation().setText("Category: " + category);
    }

    @Nonnull
    @Override
    public DefaultActionGroup createPopupActionGroup(JComponent component) {
      final TreeSet<String> availableCategories = ((AvailablePluginsTableModel)myPluginsModel).getAvailableCategories();
      final DefaultActionGroup gr = new DefaultActionGroup();
      gr.add(createFilterByCategoryAction(AvailablePluginsTableModel.ALL));
      final boolean noCategory = availableCategories.remove(N_A);
      for (final String availableCategory : availableCategories) {
        gr.add(createFilterByCategoryAction(availableCategory));
      }
      if (noCategory) {
        gr.add(createFilterByCategoryAction(N_A));
      }
      return gr;
    }

    private AnAction createFilterByCategoryAction(final String availableCategory) {
      return new AnAction(availableCategory) {
        @Override
        public void actionPerformed(AnActionEvent e) {
          final String filter = myFilter.getFilter().toLowerCase();
          ((AvailablePluginsTableModel)myPluginsModel).setCategory(availableCategory, filter);
        }
      };
    }
  }
}
