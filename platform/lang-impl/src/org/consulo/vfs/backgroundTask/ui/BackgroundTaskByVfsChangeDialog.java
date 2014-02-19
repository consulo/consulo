/*
 * Copyright 2013 must-be.org
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
package org.consulo.vfs.backgroundTask.ui;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.consulo.vfs.backgroundTask.BackgroundTaskByVfsParameters;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class BackgroundTaskByVfsChangeDialog extends DialogWrapper {
  private final BackgroundTaskByVfsParameters myParameters;
  private JPanel contentPane;
  private TextFieldWithBrowseButton myExePath;
  private CommonProgramParametersPanel myProgramParametersPanel;
  private TextFieldWithBrowseButton myOutPath;

  public BackgroundTaskByVfsChangeDialog(Project project, BackgroundTaskByVfsParameters parameters) {
    super(project);
    myParameters = parameters;

    myOutPath.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getOutPath())));
    myOutPath.addBrowseFolderListener("Select Output Path", null, project, new FileChooserDescriptor(false, true, false, false, false, false));
    myExePath.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getExePath())));
    myExePath.addBrowseFolderListener("Select Executable", null, project, new FileChooserDescriptor(true, false, false, false, false, false));
    myProgramParametersPanel.reset(parameters);

    setTitle("Configure Background Task");
    init();
  }

  @Override
  protected void doOKAction() {
    myParameters.setExePath(FileUtil.toSystemIndependentName(myExePath.getText()));
    myParameters.setOutPath(FileUtil.toSystemIndependentName(myOutPath.getText()));
    myProgramParametersPanel.applyTo(myParameters);

    super.doOKAction();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(450, 200);
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    return "#BackgroundTaskByVfsChangeDialog";
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }
}
