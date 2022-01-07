/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.plugins;

import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.AvailablePluginsManagerMain;
import com.intellij.ide.plugins.InstalledPluginsManagerMain;
import com.intellij.ide.plugins.PluginInstallUtil;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-06-26
 */
public class PluginsPanel implements Disposable {
  public static final int FROM_REPOSITORY = 0;
  public static final int INSTALLED = 1;

  public static final Key<PluginsPanel> KEY = Key.create("PluginsPanel");

  private JBEditorTabs myTabs;

  private InstalledPluginsManagerMain myInstalledPluginsPanel;

  private AvailablePluginsManagerMain myAvailablePluginsManagerMain;

  public PluginsPanel() {
    myTabs = new JBEditorTabs(null, ActionManager.getInstance(), IdeFocusManager.getGlobalInstance(), this);

    Wrapper fromRepository = new Wrapper();
    TabInfo repositoryTab = new TabInfo(fromRepository);
    repositoryTab.setText("From Repository");
    myTabs.addTab(repositoryTab);

    Wrapper installedWrapper = new Wrapper();
    TabInfo installedTab = new TabInfo(installedWrapper);
    installedTab.setText("Installed");
    myTabs.addTab(installedTab);

    myInstalledPluginsPanel = new InstalledPluginsManagerMain();
    Disposer.register(this, myInstalledPluginsPanel);
    installedWrapper.setContent(myInstalledPluginsPanel.getMainPanel());

    myAvailablePluginsManagerMain = new AvailablePluginsManagerMain();
    Disposer.register(this, myAvailablePluginsManagerMain);
    fromRepository.setContent(myAvailablePluginsManagerMain.getMainPanel());

    myInstalledPluginsPanel.setInstalledTab(myInstalledPluginsPanel);
    myInstalledPluginsPanel.setAvailableTab(myAvailablePluginsManagerMain);

    myAvailablePluginsManagerMain.setInstalledTab(myInstalledPluginsPanel);
    myAvailablePluginsManagerMain.setAvailableTab(myAvailablePluginsManagerMain);

    repositoryTab.setActions(myAvailablePluginsManagerMain.getActionGroup(), "FromRepositoryGroup");
    installedTab.setActions(myInstalledPluginsPanel.getActionGroup(), "InstallGroup");

    int pluginsCount = PluginManager.getPluginsCount();
    // set default Repository tab if no plugins installed
    select(pluginsCount == 0 ? myAvailablePluginsManagerMain : myInstalledPluginsPanel);

    DataManager.registerDataProvider(myTabs.getComponent(), dataId -> {
      if (dataId == KEY) {
        return this;
      }

      return null;
    });
  }

  public void filter(String option) {
    select(myInstalledPluginsPanel);

    myInstalledPluginsPanel.filter(option);
  }

  public void selectInstalled(PluginId pluginId) {
    myInstalledPluginsPanel.select(pluginId);
  }

  public void selectAvailable(PluginId pluginId) {
    myAvailablePluginsManagerMain.select(pluginId);
  }

  public void select(PluginManagerMain main) {
    int index;
    if (main == myInstalledPluginsPanel) {
      index = INSTALLED;
    }
    else if (main == myAvailablePluginsManagerMain) {
      index = FROM_REPOSITORY;
    }
    else {
      throw new UnsupportedOperationException();
    }

    TabInfo tabAt = myTabs.getTabAt(index);
    myTabs.select(tabAt, false);
  }

  public int getSelectedIndex() {
    TabInfo selectedInfo = myTabs.getSelectedInfo();
    if (selectedInfo == null) {
      return INSTALLED;
    }

    return myTabs.getIndexOf(selectedInfo);
  }

  @Nonnull
  public PluginManagerMain getSelected() {
    int index = getSelectedIndex();

    if(index == FROM_REPOSITORY) {
      return myAvailablePluginsManagerMain;
    }

    return myInstalledPluginsPanel;
  }

  public JComponent getComponent() {
    return myTabs.getComponent();
  }

  @Override
  public void dispose() {
  }

  public void reset() {
    myInstalledPluginsPanel.reset();
    myAvailablePluginsManagerMain.reset();
  }

  public boolean isModified() {
    return myInstalledPluginsPanel.isModified() || myAvailablePluginsManagerMain.isModified();
  }

  public void apply() throws ConfigurationException {
    String applyMessage = myInstalledPluginsPanel.apply();
    myAvailablePluginsManagerMain.apply();

    if (applyMessage != null) {
      throw new ConfigurationException(applyMessage);
    }

    if (myInstalledPluginsPanel.isRequireShutdown()) {
      final Application app = Application.get();

      int response = app.isRestartCapable() ? PluginInstallUtil.showRestartIDEADialog() : PluginInstallUtil.showShutDownIDEADialog();
      if (response == Messages.YES) {
        app.restart(true);
      }
      else {
        myInstalledPluginsPanel.ignoreChanges();
      }
    }
  }
}
