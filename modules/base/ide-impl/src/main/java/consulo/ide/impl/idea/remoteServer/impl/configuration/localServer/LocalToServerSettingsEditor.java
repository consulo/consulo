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
package consulo.ide.impl.idea.remoteServer.impl.configuration.localServer;

import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.util.lang.Comparing;
import consulo.disposer.Disposer;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.ui.ex.awt.ColoredListCellRenderer;
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
public class LocalToServerSettingsEditor<S extends ServerConfiguration, D extends DeploymentConfiguration> extends SettingsEditor<LocalServerRunConfiguration<S, D>> {
  private final ServerType<S> myServerType;
  private final DeploymentConfigurator<D> myDeploymentConfigurator;
  private final Project myProject;
  private final ComboBox mySourceComboBox;
  private final SortedComboBoxModel<DeploymentSource> mySourceListModel;
  private final JPanel myDeploymentSettingsComponent;
  private SettingsEditor<D> myDeploymentSettingsEditor;
  private DeploymentSource myLastSelection;

  public LocalToServerSettingsEditor(final ServerType<S> type, DeploymentConfigurator<D> deploymentConfigurator, Project project) {
    myServerType = type;
    myDeploymentConfigurator = deploymentConfigurator;
    myProject = project;

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
      protected void customizeCellRenderer(@Nonnull JList list, DeploymentSource value, int index, boolean selected, boolean hasFocus) {
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


  private void onDeploymentSourceChanged(@Nullable D configuration) {
    DeploymentSource selected = mySourceListModel.getSelectedItem();
    if (Comparing.equal(selected, myLastSelection)) {
      if (configuration != null && myDeploymentSettingsEditor != null) {
        myDeploymentSettingsEditor.resetFrom(configuration);
      }
      return;
    }

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


  @Override
  protected void resetEditorFrom(LocalServerRunConfiguration<S,D> configuration) {
    mySourceComboBox.setSelectedItem(configuration.getDeploymentSource());
    onDeploymentSourceChanged(configuration.getDeploymentConfiguration());
  }

  @Override
  protected void applyEditorTo(LocalServerRunConfiguration<S,D> configuration) throws ConfigurationException {
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
      .addLabeledComponent("Deployment:", mySourceComboBox)
      .addComponent(myDeploymentSettingsComponent)
      .getPanel();
  }
}
