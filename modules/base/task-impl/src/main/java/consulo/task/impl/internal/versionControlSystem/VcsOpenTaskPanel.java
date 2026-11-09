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

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.localize.TaskLocalize;
import consulo.task.ui.TaskDialogPanel;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsTaskHandler;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class VcsOpenTaskPanel extends TaskDialogPanel {
    private static final String START_FROM_BRANCH = "start.from.branch";

    private final TaskManagerImpl myTaskManager;
    private final LocalTask myPreviousTask;
    private final @Nullable VcsTaskHandler myVcsTaskHandler;
    private final List<VcsTaskHandler.TaskInfo> myBranches;
    private final CheckBox myCreateBranch;
    private final TextBox myBranchName;
    private final ComboBox<VcsTaskHandler.TaskInfo> myBranchFrom;
    private final CheckBox myUseBranch;
    private final ComboBox<VcsTaskHandler.TaskInfo> myUseBranchCombo;
    private final CheckBox myCreateChangelist;
    private final TextBox myChangelistName;
    private final Component myComponent;

    @RequiredUIAccess
    public VcsOpenTaskPanel(Project project, Task task) {
        myTaskManager = (TaskManagerImpl) TaskManager.getManager(project);
        myPreviousTask = myTaskManager.getActiveTask();

        VcsTaskHandler handler = null;
        List<VcsTaskHandler.TaskInfo> branches = List.of();
        VcsTaskHandler[] handlers = VcsTaskHandler.getAllHandlers(project);
        for (VcsTaskHandler candidate : handlers) {
            VcsTaskHandler.TaskInfo[] tasks = candidate.getAllExistingTasks();
            if (tasks.length > 0) {
                handler = candidate;
                Arrays.sort(tasks);
                branches = List.of(tasks);
                break;
            }
        }
        myVcsTaskHandler = handler;
        myBranches = branches;

        myCreateBranch = CheckBox.create(TaskLocalize.vcsTaskCreateBranch());
        myBranchName = TextBox.create(handler != null
            ? handler.cleanUpBranchName(myTaskManager.constructDefaultBranchName(task))
            : myTaskManager.suggestBranchName(task));
        myBranchFrom = ComboBox.create(branches);
        myBranchFrom.setTextRenderer(info -> info == null ? LocalizeValue.empty() : LocalizeValue.of(info.getName()));
        myUseBranch = CheckBox.create(TaskLocalize.vcsTaskUseBranch());
        myUseBranchCombo = ComboBox.create(branches);
        myUseBranchCombo.setTextRenderer(info -> info == null ? LocalizeValue.empty() : LocalizeValue.of(info.getName()));
        myCreateChangelist = CheckBox.create(TaskLocalize.vcsTaskCreateChangelist(), myTaskManager.getState().createChangelist);
        myChangelistName = TextBox.create(myTaskManager.getChangelistName(task));

        if (handler != null && !branches.isEmpty()) {
            String startFrom = ProjectPropertiesComponent.getInstance(project).getValue(START_FROM_BRANCH);
            VcsTaskHandler.TaskInfo info = null;
            for (VcsTaskHandler.TaskInfo branch : branches) {
                if (branch.getName().equals(startFrom)) {
                    info = branch;
                }
            }
            if (info == null) {
                VcsTaskHandler.TaskInfo[] current = handler.getCurrentTasks();
                info = current.length > 0 ? current[0] : branches.get(0);
            }
            myBranchFrom.setValue(info);
            myUseBranchCombo.setValue(branches.get(0));
            myBranchFrom.addValueListener(event -> {
                VcsTaskHandler.TaskInfo value = event.getValue();
                if (value != null) {
                    ProjectPropertiesComponent.getInstance(project).setValue(START_FROM_BRANCH, value.getName());
                }
            });
        }

        myCreateBranch.setValue(myTaskManager.getState().createBranch && !branches.isEmpty());
        myUseBranch.setValue(myTaskManager.getState().useBranch && !branches.isEmpty());

        myCreateBranch.addValueListener(event -> {
            if (event.getValue()) {
                if (myBranches.isEmpty()) {
                    MessageBoxes.okWarning(TaskLocalize.vcsTaskNoCommit()).showAsync();
                    myCreateBranch.setValue(false);
                    return;
                }
                myUseBranch.setValue(false);
            }
            updateFields();
        });
        myUseBranch.addValueListener(event -> {
            if (event.getValue()) {
                myCreateBranch.setValue(false);
            }
            updateFields();
        });
        myCreateChangelist.addValueListener(event -> updateFields());

        FormBuilder builder = FormBuilder.create();
        if (handlers.length > 0) {
            HorizontalLayout createBranchRow = HorizontalLayout.create(Space.SMALL);
            createBranchRow.add(myBranchName);
            createBranchRow.add(Label.create(TaskLocalize.vcsTaskFrom()));
            createBranchRow.add(myBranchFrom);
            builder.addLabeled(myCreateBranch, createBranchRow);
            builder.addLabeled(myUseBranch, myUseBranchCombo);
        }
        builder.addLabeled(myCreateChangelist, myChangelistName);
        myComponent = builder.build();

        updateFields();
    }

    @RequiredUIAccess
    private void updateFields() {
        myBranchName.setEnabled(myCreateBranch.getValueOrError());
        myBranchFrom.setEnabled(myCreateBranch.getValueOrError());
        myUseBranchCombo.setEnabled(myUseBranch.getValueOrError());
        myChangelistName.setEnabled(myCreateChangelist.getValueOrError());
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myComponent;
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        if (myCreateBranch.getValueOrError()) {
            return myBranchName;
        }
        if (myUseBranch.getValueOrError()) {
            return myUseBranchCombo;
        }
        if (myCreateChangelist.getValueOrError()) {
            return myChangelistName;
        }
        return null;
    }

    @RequiredUIAccess
    @Override
    public @Nullable LocalizeValue validate() {
        if (myCreateBranch.getValueOrError()) {
            String branchName = StringUtil.notNullize(myBranchName.getValue()).trim();
            if (branchName.isEmpty()) {
                return TaskLocalize.vcsTaskBranchEmpty();
            }
            if (myVcsTaskHandler != null) {
                return myVcsTaskHandler.isBranchNameValid(branchName) ? null : TaskLocalize.vcsTaskBranchInvalid();
            }
            if (branchName.contains(" ")) {
                return TaskLocalize.vcsTaskBranchSpaces();
            }
        }
        if (myCreateChangelist.getValueOrError() && StringUtil.isEmptyOrSpaces(myChangelistName.getValue())) {
            return TaskLocalize.vcsTaskChangelistEmpty();
        }
        return null;
    }

    @RequiredUIAccess
    @Override
    public void commit() {
        boolean createBranch = myCreateBranch.getValueOrError();
        boolean useBranch = myUseBranch.getValueOrError();
        boolean createChangelist = myCreateChangelist.getValueOrError();
        myTaskManager.getState().createChangelist = createChangelist;
        myTaskManager.getState().createBranch = createBranch;
        myTaskManager.getState().useBranch = useBranch;

        LocalTask localTask = myTaskManager.getActiveTask();
        if (createChangelist) {
            myTaskManager.createChangeList(localTask, StringUtil.notNullize(myChangelistName.getValue()));
        }
        VcsTaskHandler handler = myVcsTaskHandler;
        if (createBranch && handler != null) {
            VcsTaskHandler.TaskInfo branchFrom = myBranchFrom.getValue();
            String branchName = StringUtil.notNullize(myBranchName.getValue());
            Runnable doCreate = () -> myTaskManager.createBranch(localTask, myPreviousTask, branchName, branchFrom);
            VcsTaskHandler.TaskInfo[] current = handler.getCurrentTasks();
            if (branchFrom != null && (current.length == 0 || !current[0].equals(branchFrom))) {
                handler.switchToTask(branchFrom, doCreate);
            }
            else {
                doCreate.run();
            }
        }
        if (useBranch && handler != null) {
            VcsTaskHandler.TaskInfo branch = myUseBranchCombo.getValue();
            if (branch != null) {
                VcsTaskHandler.TaskInfo[] tasks = handler.getCurrentTasks();
                myTaskManager.addBranches(myPreviousTask, tasks, true);
                handler.switchToTask(branch, () -> myTaskManager.addBranches(localTask, new VcsTaskHandler.TaskInfo[]{branch}, false));
            }
        }
    }
}
