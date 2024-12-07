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
package consulo.ide.impl.execution.editor;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.configurable.ConfigurationException;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.FileEditorWithModifiedIcon;
import consulo.execution.impl.internal.ui.RunConfigurable;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.project.Project;
import consulo.project.internal.ProjectExListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationFileEditor extends UserDataHolderBase implements FileEditor, FileEditorWithModifiedIcon {
  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  private boolean myDisposed;

  private final Project myProject;
  private final VirtualFile myFile;
  private RunConfigurable myRunConfigurable;

  private JComponent myComponent;

  public RunConfigurationFileEditor(Project project, VirtualFile file) {
    myProject = project;
    myFile = file;
    myRunConfigurable = new RunConfigurable(project);
    myRunConfigurable.setEditorMode();

    project.getApplication().getMessageBus().connect(this).subscribe(ProjectExListener.class, targetProject -> {
      if (project == targetProject) {
        project.getApplication().getLastUIAccess().give(this::save);
      }
    });
  }

  @RequiredUIAccess
  private void save() {
    if (myRunConfigurable.isModified()) {
      try {
        myRunConfigurable.apply();

        myRunConfigurable.updateActiveConfigurationFromSelected();
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
    if (myComponent != null) {
      return myComponent;
    }

    JComponent component = myRunConfigurable.createComponent(this);
    assert component != null;

    // fix borders
    Splitter splitter = myRunConfigurable.getSplitter();
    JComponent secondComponent = splitter.getSecondComponent();
    secondComponent.setBorder(JBUI.Borders.empty(DialogWrapper.ourDefaultBorderInsets));

    UiNotifyConnector.doWhenFirstShown(component, () -> {
      final Runnable updateRequest = new Runnable() {
        @Override
        public void run() {
          if (myDisposed) {
            return;
          }

          FileEditorManagerImpl fileEditorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(myProject);
          fileEditorManager.updateFilePresentation(myFile);
          addUpdateRequest(this);
        }
      };

      addUpdateRequest(updateRequest);
    });
    return myComponent = component;
  }

  private void addUpdateRequest(final Runnable updateRequest) {
    myUpdateAlarm.addRequest(updateRequest, 500, IdeaModalityState.any());
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
    myDisposed = true;

    save();

    myRunConfigurable.disposeUIResources();

    myComponent = null;
  }
}
