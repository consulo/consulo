/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.ui.editor;

import com.intellij.execution.impl.RunConfigurable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.util.ui.JBUI;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationFileEditor extends UserDataHolderBase implements FileEditor {
  private final Project myProject;
  private RunConfigurable myRunConfigurable;

  public RunConfigurationFileEditor(Project project) {
    myProject = project;
    myRunConfigurable = new RunConfigurable(project);
    myRunConfigurable.setEditorMode();

    project.getApplication().getMessageBus().connect(this).subscribe(ProjectEx.ProjectSaved.TOPIC, targetProject -> {
      if (project == targetProject) {
        save();
      }
    });
  }

  private void save() {
    if (myRunConfigurable.isModified()) {
      try {
        myRunConfigurable.apply();
      }
      catch (ConfigurationException e) {
        if (e.getMessage() != null) {
          Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
        }
      }
    }
  }

  @Nonnull
  @Override
  @RequiredUIAccess
  public JComponent getComponent() {
    JComponent component = myRunConfigurable.createComponent(this);
    Splitter splitter = myRunConfigurable.getSplitter();
    JComponent secondComponent = splitter.getSecondComponent();
    secondComponent.setBorder(JBUI.Borders.empty(DialogWrapper.ourDefaultBorderInsets));
    return component;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRunConfigurable.getPreferredFocusedComponent();
  }

  @Nonnull
  @Override
  public String getName() {
    return "run_configurations";
  }

  @Override
  public boolean isModified() {
    return myRunConfigurable.isModified();
  }

  @Override
  public void selectNotify() {
    save();
  }

  @Override
  public void deselectNotify() {
    save();
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Override
  public void dispose() {
    save();
    
    myRunConfigurable.disposeUIResources();
  }
}
