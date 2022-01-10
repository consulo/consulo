/*
 * Copyright 2013-2016 consulo.io
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
package consulo.backgroundTaskByVfsChange.ui;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import consulo.backgroundTaskByVfsChange.BackgroundTaskByVfsParameters;
import consulo.backgroundTaskByVfsChange.BackgroundTaskByVfsParametersImpl;
import javax.annotation.Nonnull;

import javax.swing.*;

public class BackgroundTaskByVfsChangePanel extends JPanel {
  private JPanel contentPane;
  private TextFieldWithBrowseButton myExePath;
  private CommonProgramParametersPanel myProgramParametersPanel;
  private TextFieldWithBrowseButton myOutPath;
  private JCheckBox myShowConsoleCheckBox;

  public BackgroundTaskByVfsChangePanel(Project project) {
    myOutPath.addBrowseFolderListener("Select Output Path", null, project, new FileChooserDescriptor(false, true, false, false, false, false));
    myExePath.addBrowseFolderListener("Select Executable", null, project, new FileChooserDescriptor(true, false, false, false, false, false));
  }

  public void reset(@Nonnull BackgroundTaskByVfsParameters parameters) {
    myProgramParametersPanel.reset(parameters);
    myExePath.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getExePath())));
    myOutPath.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getOutPath())));
    myShowConsoleCheckBox.setSelected(parameters.isShowConsole());
    UIUtil.setEnabled(this, parameters != BackgroundTaskByVfsParametersImpl.EMPTY, true);
  }

  protected void applyTo(@Nonnull BackgroundTaskByVfsParameters parameters) {
    parameters.setExePath(FileUtil.toSystemIndependentName(myExePath.getText()));
    parameters.setOutPath(FileUtil.toSystemIndependentName(myOutPath.getText()));
    myProgramParametersPanel.applyTo(parameters);
    parameters.setShowConsole(myShowConsoleCheckBox.isSelected());
  }

  private void createUIComponents() {
    contentPane = this;
  }
}
