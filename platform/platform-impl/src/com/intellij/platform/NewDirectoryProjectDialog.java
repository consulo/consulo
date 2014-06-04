/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.platform;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;

/**
 * @author yole
 */
public class NewDirectoryProjectDialog extends DialogWrapper {
  private JTextField myProjectNameTextField;
  private TextFieldWithBrowseButton myLocationField;
  protected JPanel myRootPane;
  private JLabel myLocationLabel;

  protected NewDirectoryProjectDialog(Project project) {
    super(project, true);
    setTitle("Create New Project");
    init();

    myLocationLabel.setLabelFor(myLocationField.getChildComponent());

    new LocationNameFieldsBinding(project, myLocationField, myProjectNameTextField, ProjectUtil.getBaseDir(),
                                  "Select Location for Project Directory");
  }

  protected void checkValid() {
    String projectName = myProjectNameTextField.getText();
    if (projectName.trim().isEmpty()) {
      setOKActionEnabled(false);
      setErrorText("Project name can't be empty");
      return;
    }
    if (myLocationField.getText().indexOf('$') >= 0) {
      setOKActionEnabled(false);
      setErrorText("Project directory name must not contain the $ character");
      return;
    }

    setOKActionEnabled(true);
    setErrorText(null);
  }

  @Override
  protected JComponent createCenterPanel() {
    return myRootPane;
  }

  public String getNewProjectLocation() {
    return myLocationField.getText();
  }

  public String getNewProjectName() {
    return myProjectNameTextField.getText();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myProjectNameTextField;
  }

  @Override
  protected String getHelpId() {
    return "create_new_project_dialog";
  }
}
