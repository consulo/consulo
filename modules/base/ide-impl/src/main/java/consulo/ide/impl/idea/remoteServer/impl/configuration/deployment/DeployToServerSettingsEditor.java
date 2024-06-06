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
package consulo.ide.impl.idea.remoteServer.impl.configuration.deployment;

import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.util.lang.Comparing;
import consulo.disposer.Disposer;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.ide.impl.idea.remoteServer.impl.configuration.RemoteServerListConfigurable;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboboxWithBrowseButton;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SortedComboBoxModel;
import consulo.ui.ex.awt.FormBuilder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;

/**
 * @author nik
 */
public class DeployToServerSettingsEditor<S extends ServerConfiguration, D extends DeploymentConfiguration> extends SettingsEditor<DeployToServerRunConfiguration<S, D>> {
  private final ServerType<S> myServerType;
  private final DeploymentConfigurator<D> myDeploymentConfigurator;
  private final Project myProject;
  private final ComboboxWithBrowseButton myServerComboBox;
  private final ComboBox mySourceComboBox;
  private final SortedComboBoxModel<String> myServerListModel;
  private final SortedComboBoxModel<DeploymentSource> mySourceListModel;
  private final JPanel myDeploymentSettingsComponent;
  private SettingsEditor<D> myDeploymentSettingsEditor;
  private DeploymentSource myLastSelection;

  public DeployToServerSettingsEditor(final ServerType<S> type, DeploymentConfigurator<D> deploymentConfigurator, Project project) {
    myServerType = type;
    myDeploymentConfigurator = deploymentConfigurator;
    myProject = project;

    myServerListModel = new SortedComboBoxModel<String>(String.CASE_INSENSITIVE_ORDER);
    myServerComboBox = new ComboboxWithBrowseButton(new ComboBox(myServerListModel));
    fillApplicationServersList(null);
    myServerComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        RemoteServerListConfigurable configurable = RemoteServerListConfigurable.createConfigurable(type);
        ShowSettingsUtil.getInstance().editConfigurable(myServerComboBox, configurable).doWhenDone(() -> {
          fillApplicationServersList(configurable.getLastSelectedServer());
        });
      }
    });
    myServerComboBox.getComboBox().setRenderer(new ColoredListCellRenderer<String>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<? extends String> list, String value, int index, boolean selected, boolean hasFocus) {
        if (value == null) return;
        RemoteServer<S> server = RemoteServersManager.getInstance().findByName(value, type);
        SimpleTextAttributes attributes = server == null ? SimpleTextAttributes.ERROR_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        setIcon(server != null ? server.getType().getIcon() : null);
        append(value, attributes);
      }
    });

    mySourceListModel = new SortedComboBoxModel<DeploymentSource>(new Comparator<DeploymentSource>() {
      @Override
      public int compare(DeploymentSource o1, DeploymentSource o2) {
        return o1.getPresentableName().compareToIgnoreCase(o2.getPresentableName());
      }
    });
    mySourceListModel.addAll(deploymentConfigurator.getAvailableDeploymentSources());
    mySourceComboBox = new ComboBox(mySourceListModel);
    mySourceComboBox.setRenderer(new ColoredListCellRenderer<DeploymentSource>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<? extends DeploymentSource> list, DeploymentSource value, int index, boolean selected, boolean hasFocus) {
        if (value == null) return;
        setIcon(value.getIcon());
        append(value.getPresentableName());
      }
    });

    myDeploymentSettingsComponent = new JPanel(new BorderLayout());
    mySourceComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onDeploymentSourceChanged(null);
      }
    });
  }

  private void fillApplicationServersList(@Nullable RemoteServer<?> newSelection) {
    String oldSelection = myServerListModel.getSelectedItem();
    myServerListModel.clear();
    for (RemoteServer<?> server : RemoteServersManager.getInstance().getServers(myServerType)) {
      myServerListModel.add(server.getName());
    }
    myServerComboBox.getComboBox().setSelectedItem(newSelection != null ? newSelection.getName() : oldSelection);
  }

  private void onDeploymentSourceChanged(@Nullable D configuration) {
    DeploymentSource selected = mySourceListModel.getSelectedItem();
    if (Comparing.equal(selected, myLastSelection)) {
      if (configuration != null && myDeploymentSettingsEditor != null) {
        myDeploymentSettingsEditor.resetFrom(configuration);
      }
      return;
    }

    updateBeforeRunOptions(myLastSelection, false);
    updateBeforeRunOptions(selected, true);
    myDeploymentSettingsComponent.removeAll();
    myDeploymentSettingsEditor = myDeploymentConfigurator.createEditor(selected);
    if (myDeploymentSettingsEditor != null) {
      Disposer.register(this, myDeploymentSettingsEditor);
      myDeploymentSettingsComponent.add(BorderLayout.CENTER, myDeploymentSettingsEditor.getComponent());
      if (configuration != null) {
        myDeploymentSettingsEditor.resetFrom(configuration);
      }
    }
    myLastSelection = selected;
  }

  private void updateBeforeRunOptions(@Nullable DeploymentSource source, boolean selected) {
    if (source != null) {
      DeploymentSourceType type = source.getType();
      DataContext dataContext = DataManager.getInstance().getDataContext(myServerComboBox);
      type.updateBuildBeforeRunOption(dataContext, myProject, source, selected);
    }
  }

  @Override
  protected void resetEditorFrom(DeployToServerRunConfiguration<S,D> configuration) {
    String serverName = configuration.getServerName();
    if (serverName != null && !myServerListModel.getItems().contains(serverName)) {
      myServerListModel.add(serverName);
    }
    myServerComboBox.getComboBox().setSelectedItem(serverName);
    mySourceComboBox.setSelectedItem(configuration.getDeploymentSource());
    onDeploymentSourceChanged(configuration.getDeploymentConfiguration());
  }

  @Override
  protected void applyEditorTo(DeployToServerRunConfiguration<S,D> configuration) throws ConfigurationException {
    configuration.setServerName(myServerListModel.getSelectedItem());
    DeploymentSource deploymentSource = mySourceListModel.getSelectedItem();
    configuration.setDeploymentSource(deploymentSource);

    if (deploymentSource != null) {
      D deployment = configuration.getDeploymentConfiguration();
      if (deployment == null) {
        deployment = myDeploymentConfigurator.createDefaultConfiguration(deploymentSource);
        configuration.setDeploymentConfiguration(deployment);
      }
      if (myDeploymentSettingsEditor != null) {
        myDeploymentSettingsEditor.applyTo(deployment);
      }
    }
    else {
      configuration.setDeploymentConfiguration(null);
    }
  }

  @Nonnull
  @Override
  protected JComponent createEditor() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent("Server:", myServerComboBox)
      .addLabeledComponent("Deployment:", mySourceComboBox)
      .addComponent(myDeploymentSettingsComponent)
      .getPanel();
  }
}
