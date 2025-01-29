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
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.ExternalSystemManager;
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
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBTextField;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

import static consulo.externalSystem.util.ExternalSystemApiUtil.normalizePath;

/**
 * @author Denis Zhdanov
 * @since 23.05.13 18:46
 */
public class ExternalSystemTaskSettingsControl implements ExternalSystemSettingsControl<ExternalSystemTaskExecutionSettings> {

  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nonnull
  private final Project myProject;

  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myProjectPathLabel;
  private ExternalProjectPathField myProjectPathField;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myTasksLabel;
  private JBTextField myTasksTextField;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myVmOptionsLabel;
  private RawCommandLineEditor myVmOptionsEditor;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myScriptParametersLabel;
  private RawCommandLineEditor myScriptParametersEditor;

  @Nullable
  private ExternalSystemTaskExecutionSettings myOriginalSettings;

  public ExternalSystemTaskSettingsControl(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    myProject = project;
    myExternalSystemId = externalSystemId;
  }

  public void setOriginalSettings(@Nullable ExternalSystemTaskExecutionSettings originalSettings) {
    myOriginalSettings = originalSettings;
  }

  @Override
  public void fillUi(@Nonnull Disposable uiDisposable, @Nonnull final PaintAwarePanel canvas, int indentLevel) {
    myProjectPathLabel = new JBLabel(ExternalSystemBundle.message(
      "run.configuration.settings.label.project", myExternalSystemId.getDisplayName().get()));
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    FileChooserDescriptor projectPathChooserDescriptor = null;
    if (manager instanceof ExternalSystemUiAware) {
      projectPathChooserDescriptor = ((ExternalSystemUiAware)manager).getExternalProjectConfigDescriptor();
    }
    if (projectPathChooserDescriptor == null) {
      projectPathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
    }
    String title = ExternalSystemBundle.message("settings.label.select.project", myExternalSystemId.getDisplayName().get());
    myProjectPathField = new ExternalProjectPathField(myProject, myExternalSystemId, projectPathChooserDescriptor, title) {
      @Override
      public Dimension getPreferredSize() {
        return myVmOptionsEditor == null ? super.getPreferredSize() : myVmOptionsEditor.getTextField().getPreferredSize();
      }
    };
    canvas.add(myProjectPathLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myProjectPathField, ExternalSystemUiUtil.getFillLineConstraints(0));

    myTasksLabel = new JBLabel(ExternalSystemBundle.message("run.configuration.settings.label.tasks"));
    myTasksTextField = new JBTextField(ExternalSystemConstants.TEXT_FIELD_WIDTH_IN_COLUMNS);
    canvas.add(myTasksLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myTasksTextField, ExternalSystemUiUtil.getFillLineConstraints(0));

    myVmOptionsLabel = new JBLabel(ExternalSystemBundle.message("run.configuration.settings.label.vmoptions"));
    myVmOptionsEditor = new RawCommandLineEditor();
    myVmOptionsEditor.setDialogCaption(ExternalSystemBundle.message("run.configuration.settings.label.vmoptions"));
    canvas.add(myVmOptionsLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myVmOptionsEditor, ExternalSystemUiUtil.getFillLineConstraints(0));
    myScriptParametersLabel = new JBLabel(ExternalSystemBundle.message("run.configuration.settings.label.script.parameters"));
    myScriptParametersEditor = new RawCommandLineEditor();
    myScriptParametersEditor.setDialogCaption(ExternalSystemBundle.message("run.configuration.settings.label.script.parameters"));
    canvas.add(myScriptParametersLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myScriptParametersEditor, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  @Override
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
  public void apply(@Nonnull ExternalSystemTaskExecutionSettings settings) {
    String projectPath = myProjectPathField.getText();
    settings.setExternalProjectPath(projectPath);
    settings.setTaskNames(StringUtil.split(myTasksTextField.getText(), " "));
    settings.setVmOptions(myVmOptionsEditor.getText());
    settings.setScriptParameters(myScriptParametersEditor.getText());
  }

  @Override
  public boolean validate(@Nonnull ExternalSystemTaskExecutionSettings settings) throws ConfigurationException {
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
