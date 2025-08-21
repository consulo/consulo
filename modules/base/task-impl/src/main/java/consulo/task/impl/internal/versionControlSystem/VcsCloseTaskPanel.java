/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.task.impl.internal.versionControlSystem;

import consulo.project.Project;
import consulo.versionControlSystem.VcsType;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.task.ChangeListInfo;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.ui.TaskDialogPanel;
import consulo.ui.ex.awt.JBCheckBox;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;

/**
 * @author Dmitry Avdeev
 */
public class VcsCloseTaskPanel extends TaskDialogPanel {

  private JPanel myPanel;
  private JPanel myVcsPanel;
  private JCheckBox myCommitChanges;
  private JBCheckBox myMergeBranches;

  private final Project myProject;
  private final LocalTask myTask;
  private final TaskManagerImpl myTaskManager;

  public VcsCloseTaskPanel(Project project, LocalTask task) {
    myProject = project;
    myTask = task;

    myTaskManager = (TaskManagerImpl)TaskManager.getManager(project);
    
    boolean hasChanges = !task.getChangeLists().isEmpty();
    myCommitChanges.setEnabled(hasChanges);
    myCommitChanges.setSelected(hasChanges && myTaskManager.getState().commitChanges);
    if (myTaskManager.getActiveVcs().getType() == VcsType.distributed) {
      boolean enabled = !task.getBranches(true).isEmpty() && !task.getBranches(false).isEmpty();
      myMergeBranches.setEnabled(enabled);
      myMergeBranches.setSelected(enabled && myTaskManager.getState().mergeBranch);
    }
    else {
      myMergeBranches.setVisible(false);
    }
    
  }

  @Nonnull
  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  boolean isCommitChanges() {
    return myCommitChanges.isSelected();
  }

  boolean isMergeBranch() {
    return myMergeBranches.isSelected();
  }
  
  @Override
  public void commit() {

    if (myCommitChanges.isEnabled()) {
      myTaskManager.getState().commitChanges = isCommitChanges();
    }
    if (myMergeBranches.isEnabled()) {
      myTaskManager.getState().mergeBranch = isMergeBranch();
    }
    
    if (isCommitChanges()) {
      ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
      for (ChangeListInfo info : myTask.getChangeLists()) {
        LocalChangeList list = changeListManager.getChangeList(info.id);
        if (list != null) {
          changeListManager.commitChanges(list, new ArrayList<>(list.getChanges()));
        }
      }
    }
    if (isMergeBranch()) {
      myTaskManager.mergeBranch(myTask);
    }

  }
}
