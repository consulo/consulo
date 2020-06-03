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
package com.intellij.remoteServer.impl.configuration.localServer;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Comparing;
import consulo.disposer.Disposer;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SortedComboBoxModel;
import com.intellij.util.ui.FormBuilder;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
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
