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
package consulo.ide.impl.idea.openapi.vcs.changes.conflicts;

import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.vcs.VcsBundle;
import consulo.vcs.change.ChangeList;
import consulo.vcs.change.ChangeListManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.readOnlyHandler.FileListRenderer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.CollectionListModel;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ChangelistConflictDialog extends DialogWrapper {

  private JPanel myPanel;

  private JRadioButton myShelveChangesRadioButton;
  private JRadioButton myMoveChangesToActiveRadioButton;
  private JRadioButton mySwitchToChangelistRadioButton;
  private JRadioButton myIgnoreRadioButton;
  private JList myFileList;

  private final Project myProject;

  public ChangelistConflictDialog(Project project, List<ChangeList> changeLists, List<VirtualFile> conflicts) {
    super(project);
    myProject = project;

    setTitle("Resolve Changelist Conflict");

    myFileList.setCellRenderer(new FileListRenderer());
    myFileList.setModel(new CollectionListModel(conflicts));

    ChangeListManagerImpl manager = ChangeListManagerImpl.getInstanceImpl(myProject);
    ChangelistConflictResolution resolution = manager.getConflictTracker().getOptions().LAST_RESOLUTION;

    if (changeLists.size() > 1) {
      mySwitchToChangelistRadioButton.setEnabled(false);
      if (resolution == ChangelistConflictResolution.SWITCH) {
        resolution = ChangelistConflictResolution.IGNORE;
      }
    }
    mySwitchToChangelistRadioButton.setText(VcsBundle.message("switch.to.changelist", changeLists.iterator().next().getName()));
    myMoveChangesToActiveRadioButton.setText(VcsBundle.message("move.to.changelist", manager.getDefaultChangeList().getName()));
    
    switch (resolution) {

      case SHELVE:
        myShelveChangesRadioButton.setSelected(true);
        break;
      case MOVE:
        myMoveChangesToActiveRadioButton.setSelected(true);
        break;
      case SWITCH:
        mySwitchToChangelistRadioButton.setSelected(true) ;
        break;
      case IGNORE:
        myIgnoreRadioButton.setSelected(true);
        break;
    }
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  public ChangelistConflictResolution getResolution() {
    if (myShelveChangesRadioButton.isSelected())
      return ChangelistConflictResolution.SHELVE;
    if (myMoveChangesToActiveRadioButton.isSelected())
      return ChangelistConflictResolution.MOVE;
    if (mySwitchToChangelistRadioButton.isSelected())
      return ChangelistConflictResolution.SWITCH;
    return ChangelistConflictResolution.IGNORE;
  }

  @Nonnull
  @Override
  protected Action[] createLeftSideActions() {
    return new Action[] { new AbstractAction("&Configure...") {
      public void actionPerformed(ActionEvent e) {
        ChangeListManagerImpl manager = (ChangeListManagerImpl)ChangeListManager.getInstance(myProject);
        ShowSettingsUtil.getInstance().editConfigurable(myPanel, new ChangelistConflictConfigurable(manager));
      }
    }};
  }

  protected String getHelpId() {
    return "project.propVCSSupport.ChangelistConflict";
  }
}
