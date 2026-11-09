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
import consulo.task.ChangeListInfo;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.localize.TaskLocalize;
import consulo.task.ui.TaskDialogPanel;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.VcsType;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;

import java.util.ArrayList;

/**
 * @author Dmitry Avdeev
 */
public class VcsCloseTaskPanel extends TaskDialogPanel {
    private final Project myProject;
    private final LocalTask myTask;
    private final TaskManagerImpl myTaskManager;
    private final CheckBox myCommitChanges;
    private final CheckBox myMergeBranches;
    private final VerticalLayout myComponent;

    @RequiredUIAccess
    public VcsCloseTaskPanel(Project project, LocalTask task) {
        myProject = project;
        myTask = task;
        myTaskManager = (TaskManagerImpl) TaskManager.getManager(project);

        boolean hasChanges = !task.getChangeLists().isEmpty();
        myCommitChanges = CheckBox.create(TaskLocalize.vcsTaskCommitChanges(), hasChanges && myTaskManager.getState().commitChanges);
        myCommitChanges.setEnabled(hasChanges);

        myMergeBranches = CheckBox.create(TaskLocalize.vcsTaskMergeBranch());
        myComponent = VerticalLayout.create();
        myComponent.add(myCommitChanges);
        if (myTaskManager.getActiveVcs() != null && myTaskManager.getActiveVcs().getType() == VcsType.distributed) {
            boolean enabled = !task.getBranches(true).isEmpty() && !task.getBranches(false).isEmpty();
            myMergeBranches.setEnabled(enabled);
            myMergeBranches.setValue(enabled && myTaskManager.getState().mergeBranch);
            myComponent.add(myMergeBranches);
        }
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    @Override
    public void commit() {
        boolean commitChanges = myCommitChanges.getValueOrError();
        boolean mergeBranch = myMergeBranches.getValueOrError();
        if (myCommitChanges.isEnabled()) {
            myTaskManager.getState().commitChanges = commitChanges;
        }
        if (myMergeBranches.isEnabled()) {
            myTaskManager.getState().mergeBranch = mergeBranch;
        }

        if (commitChanges) {
            ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
            for (ChangeListInfo info : myTask.getChangeLists()) {
                LocalChangeList list = changeListManager.getChangeList(info.id);
                if (list != null) {
                    changeListManager.commitChanges(list, new ArrayList<>(list.getChanges()));
                }
            }
        }
        if (mergeBranch) {
            myTaskManager.mergeBranch(myTask);
        }
    }
}
