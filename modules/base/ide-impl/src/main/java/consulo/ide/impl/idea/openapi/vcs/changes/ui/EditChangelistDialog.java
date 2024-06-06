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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author max
 */
public class EditChangelistDialog extends DialogWrapper {
  private final NewEditChangelistPanel myPanel;
  private final Project myProject;
  private final LocalChangeList myList;

  public EditChangelistDialog(Project project, @Nonnull LocalChangeList list) {
    super(project, true);
    myProject = project;
    myList = list;
    myPanel = new NewEditChangelistPanel(project) {
      @Override
      protected void nameChanged(String errorMessage) {
        setOKActionEnabled(errorMessage == null);
        setErrorText(errorMessage);
      }
    };
    myPanel.setChangeListName(list.getName());
    myPanel.setDescription(list.getComment());
    myPanel.init(list);
    myPanel.getMakeActiveCheckBox().setSelected(myList.isDefault());
    myPanel.getMakeActiveCheckBox().setEnabled(!myList.isDefault());
    setTitle(VcsLocalize.changesDialogEditchangelistTitle().get());
    init();
  }

  protected JComponent createCenterPanel() {
    return myPanel.getContent();
  }

  protected void doOKAction() {
    String oldName = myList.getName();
    String oldComment = myList.getComment();

    if (!Comparing.equal(oldName, myPanel.getChangeListName()) && ChangeListManager.getInstance(myProject).findChangeList(myPanel.getChangeListName()) != null) {
      Messages.showErrorDialog(
        myPanel.getContent(),
        VcsLocalize.changesDialogEditchangelistErrorAlreadyExists(myPanel.getChangeListName()).get(),
        VcsLocalize.changesDialogEditchangelistTitle().get()
      );
      return;
    }

    if (!Comparing.equal(oldName, myPanel.getChangeListName(), true) || !Comparing.equal(oldComment, myPanel.getDescription(), true)) {
      final ChangeListManager clManager = ChangeListManager.getInstance(myProject);

      final String newName = myPanel.getChangeListName();
      if (! myList.getName().equals(newName)) {
        clManager.editName(myList.getName(), newName);
      }
      final String newDescription = myPanel.getDescription();
      if (! myList.getComment().equals(newDescription)) {
        clManager.editComment(myList.getName(), newDescription);
      }
    }
    if (!myList.isDefault() && myPanel.getMakeActiveCheckBox().isSelected()) {
      ChangeListManager.getInstance(myProject).setDefaultChangeList(myList);  
    }
    myPanel.changelistCreatedOrChanged(myList);
    super.doOKAction();
  }

  public JComponent getPreferredFocusedComponent() {
    return myPanel.getPreferredFocusedComponent();
  }

  protected String getDimensionServiceKey() {
    return "VCS.EditChangelistDialog";
  }
}
