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
package consulo.externalSystem.service.execution;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.ui.awt.ExternalProjectPathField;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.externalSystem.ui.awt.PaintAwarePanel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;

import static consulo.externalSystem.util.ExternalSystemApiUtil.normalizePath;

/**
 * @author Denis Zhdanov
 * @since 2013-05-23
 */
public class ExternalSystemTaskSettingsControl implements ExternalSystemSettingsControl<ExternalSystemTaskExecutionSettings> {
  private final ProjectSystemId myExternalSystemId;
  
  private final Project myProject;

  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private Label myProjectPathLabel;
  private ExternalProjectPathField myProjectPathField;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private Label myTasksLabel;
  private JBTextField myTasksTextField;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private Label myVmOptionsLabel;
  private RawCommandLineEditor myVmOptionsEditor;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private Label myScriptParametersLabel;
  private RawCommandLineEditor myScriptParametersEditor;

  private @Nullable ExternalSystemTaskExecutionSettings myOriginalSettings;

  public ExternalSystemTaskSettingsControl(Project project, ProjectSystemId externalSystemId) {
    myProject = project;
    myExternalSystemId = externalSystemId;
  }

  public void setOriginalSettings(@Nullable ExternalSystemTaskExecutionSettings originalSettings) {
    myOriginalSettings = originalSettings;
  }

  @Override
  @RequiredUIAccess
  public void fillUi(Disposable uiDisposable, PaintAwarePanel canvas, int indentLevel) {
    myProjectPathLabel = Label.create(ExternalSystemLocalize.runConfigurationSettingsLabelProject(myExternalSystemId.getDisplayName()));
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    FileChooserDescriptor projectPathChooserDescriptor = null;
    if (manager instanceof ExternalSystemUiAware extSysUiAware) {
      projectPathChooserDescriptor = extSysUiAware.getExternalProjectConfigDescriptor();
    }
    if (projectPathChooserDescriptor == null) {
      projectPathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
    }
    String title = ExternalSystemLocalize.settingsLabelSelectProject(myExternalSystemId.getDisplayName().get()).get();
    myProjectPathField = new ExternalProjectPathField(myProject, myExternalSystemId, projectPathChooserDescriptor, title) {
      @Override
      public Dimension getPreferredSize() {
        return myVmOptionsEditor == null ? super.getPreferredSize() : myVmOptionsEditor.getTextField().getPreferredSize();
      }
    };
    canvas.add(TargetAWT.to(myProjectPathLabel), ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myProjectPathField, ExternalSystemUiUtil.getFillLineConstraints(0));

    myTasksLabel = Label.create(ExternalSystemLocalize.runConfigurationSettingsLabelTasks());
    myTasksTextField = new JBTextField(ExternalSystemConstants.TEXT_FIELD_WIDTH_IN_COLUMNS);
    canvas.add(TargetAWT.to(myTasksLabel), ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myTasksTextField, ExternalSystemUiUtil.getFillLineConstraints(0));

    myVmOptionsLabel = Label.create(ExternalSystemLocalize.runConfigurationSettingsLabelVmoptions());
    myVmOptionsEditor = new RawCommandLineEditor();
    myVmOptionsEditor.setDialogCaption(ExternalSystemLocalize.runConfigurationSettingsLabelVmoptions().get());
    canvas.add(TargetAWT.to(myVmOptionsLabel), ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myVmOptionsEditor, ExternalSystemUiUtil.getFillLineConstraints(0));
    myScriptParametersLabel = Label.create(ExternalSystemLocalize.runConfigurationSettingsLabelScriptParameters());
    myScriptParametersEditor = new RawCommandLineEditor();
    myScriptParametersEditor.setDialogCaption(ExternalSystemLocalize.runConfigurationSettingsLabelScriptParameters().get());
    canvas.add(TargetAWT.to(myScriptParametersLabel), ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myScriptParametersEditor, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  @Override
  @RequiredUIAccess
  public void reset() {
    myProjectPathField.setText("");
    myTasksTextField.setText("");
    myVmOptionsEditor.setText("");
    myScriptParametersEditor.setText("");
    showUi(true);

    if (myOriginalSettings == null) {
      return;
    }

    String path = myOriginalSettings.getExternalProjectPath();
    if (StringUtil.isEmpty(path)) {
      path = "";
    }
    myProjectPathField.setText(path);
    myTasksTextField.setText(StringUtil.join(myOriginalSettings.getTaskNames(), " "));
    myVmOptionsEditor.setText(myOriginalSettings.getVmOptions());
    myScriptParametersEditor.setText(myOriginalSettings.getScriptParameters());
  }

  @Override
  public boolean isModified() {
    if (myOriginalSettings == null) {
      return false;
    }

    return !Comparing.equal(normalizePath(myProjectPathField.getText()),
                            normalizePath(myOriginalSettings.getExternalProjectPath()))
           || !Comparing.equal(normalizePath(myTasksTextField.getText()),
                               normalizePath(StringUtil.join(myOriginalSettings.getTaskNames(), " ")))
           || !Comparing.equal(normalizePath(myVmOptionsEditor.getText()),
                               normalizePath(myOriginalSettings.getVmOptions()))
           || !Comparing.equal(normalizePath(myScriptParametersEditor.getText()),
                               normalizePath(myOriginalSettings.getScriptParameters()));
  }

  @Override
  public void apply(ExternalSystemTaskExecutionSettings settings) {
    String projectPath = myProjectPathField.getText();
    settings.setExternalProjectPath(projectPath);
    settings.setTaskNames(StringUtil.split(myTasksTextField.getText(), " "));
    settings.setVmOptions(myVmOptionsEditor.getText());
    settings.setScriptParameters(myScriptParametersEditor.getText());
  }

  @Override
  public boolean validate(ExternalSystemTaskExecutionSettings settings) throws ConfigurationException {
    String projectPath = myProjectPathField.getText();
    if (myOriginalSettings == null) {
      throw new ConfigurationException(String.format(
        "Can't store external task settings into run configuration. Reason: target run configuration is undefined. Tasks: '%s', " +
        "external project: '%s', vm options: '%s', script parameters: '%s'",
        myTasksTextField.getText(), projectPath, myVmOptionsEditor.getText(), myScriptParametersEditor.getText()
      ));
    }
    return true;
  }

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }
}
