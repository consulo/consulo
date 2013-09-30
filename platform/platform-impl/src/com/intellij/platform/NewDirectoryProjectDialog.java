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
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author yole
 */
public class NewDirectoryProjectDialog extends DialogWrapper {
  private JTextField myProjectNameTextField;
  private TextFieldWithBrowseButton myLocationField;
  protected JPanel myRootPane;
  protected JComboBox myProjectTypeComboBox;
  private JPanel myProjectTypePanel;
  private JLabel myLocationLabel;

  protected JPanel getPlaceHolder() {
    return myPlaceHolder;
  }

  private JPanel myPlaceHolder;

  protected NewDirectoryProjectDialog(Project project) {
    super(project, true);
    setTitle("Create New Project");
    init();

    myLocationLabel.setLabelFor(myLocationField.getChildComponent());

    new LocationNameFieldsBinding(project, myLocationField, myProjectNameTextField, ProjectUtil.getBaseDir(),
                                  "Select Location for Project Directory");

    final DirectoryProjectGenerator[] generators = DirectoryProjectGenerator.EP_NAME.getExtensions();
    assert generators.length != 0;

    DefaultComboBoxModel model = new DefaultComboBoxModel();

    for (DirectoryProjectGenerator generator : generators) {
      if (generator instanceof HideableProjectGenerator) {
        if (((HideableProjectGenerator)generator).isHidden()) {
          continue;
        }
      }
      model.addElement(generator);
    }

    myProjectTypeComboBox.setModel(model);
    myProjectTypeComboBox.setRenderer(createProjectTypeListCellRenderer(myProjectTypeComboBox.getRenderer()));

  }

  @NotNull
  private ListCellRenderer createProjectTypeListCellRenderer(@NotNull final ListCellRenderer originalRenderer) {

    return new ListCellRendererWrapper() {
      @Override
      public void customize(final JList list, final Object value, final int index, final boolean selected, final boolean cellHasFocus) {
        if (value == null) return;
        setText(((DirectoryProjectGenerator)value).getName());
      }
    };
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
    DirectoryProjectGenerator generator = getProjectGenerator();
    if (generator != null) {
      String baseDirPath = myLocationField.getTextField().getText();
      String validationResult = generator.validate(baseDirPath);
      if (validationResult != null) {
        setOKActionEnabled(false);
        setErrorText(validationResult);
        return;
      }
    }
    setOKActionEnabled(true);
    setErrorText(null);
  }


  protected JComponent createCenterPanel() {
    return myRootPane;
  }

  public String getNewProjectLocation() {
    return myLocationField.getText();
  }

  public String getNewProjectName() {
    return myProjectNameTextField.getText();
  }

  @Nullable
  public DirectoryProjectGenerator getProjectGenerator() {
    final Object selItem = myProjectTypeComboBox.getSelectedItem();
    return (DirectoryProjectGenerator)selItem;
  }

  public JComponent getPreferredFocusedComponent() {
    return myProjectNameTextField;
  }

  @Override
  protected String getHelpId() {
    return "create_new_project_dialog";
  }
}
