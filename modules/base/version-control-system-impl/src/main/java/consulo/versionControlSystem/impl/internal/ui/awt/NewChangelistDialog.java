/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.impl.internal.change.ui.awt.NewEditChangelistPanel;
import consulo.versionControlSystem.localize.VcsLocalize;

import javax.swing.*;

/**
 * @author max
 */
public class NewChangelistDialog extends DialogWrapper {

  private NewEditChangelistPanel myPanel;
  private JPanel myTopPanel;
  private JLabel myErrorLabel;
  private final Project myProject;

  public NewChangelistDialog(Project project) {
    super(project, true);
    myProject = project;
    setTitle(VcsLocalize.changesDialogNewchangelistTitle());

    myPanel.init(null);

    init();
  }

  @Override
  protected void doOKAction() {
    super.doOKAction();
    VcsConfiguration.getInstance(myProject).MAKE_NEW_CHANGELIST_ACTIVE = isNewChangelistActive();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myTopPanel;
  }

  public String getName() {
    return myPanel.getChangeListName();
  }

  public String getDescription() {
    return myPanel.getDescription();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPanel.getPreferredFocusedComponent();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "VCS.EditChangelistDialog";
  }

  public boolean isNewChangelistActive() {
    return myPanel.getMakeActiveCheckBox().isSelected();
  }

  public NewEditChangelistPanel getPanel() {
    return myPanel;
  }

  private void createUIComponents() {
    myPanel = new NewEditChangelistPanel(myProject) {
      @Override
      protected void nameChanged(String errorMessage) {
        setOKActionEnabled(errorMessage == null);
        myErrorLabel.setText(errorMessage == null ? " " : errorMessage);        
      }
    };
  }

  @Override
  protected String getHelpId() {
    return "new_changelist_dialog";
  }
}
