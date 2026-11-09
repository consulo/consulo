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
package consulo.task.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.localize.TaskLocalize;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "tasks.close")
public class CloseTaskAction extends BaseTaskAction {
    public CloseTaskAction() {
        super(TaskLocalize.actionTasksCloseText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        TaskManagerImpl taskManager = (TaskManagerImpl) TaskManager.getManager(project);
        LocalTask task = taskManager.getActiveTask();
        UIAccess uiAccess = UIAccess.current();
        CloseTaskDialog.show(project, task).whenCompleteAsync((taskState, dialogError) -> {
            TaskRepository repository = task.getRepository();
            if (dialogError != null || taskState == null || repository == null) {
                return;
            }
            taskManager.setTaskState(repository, task, taskState, false).whenCompleteAsync((result, error) -> {
                if (error != null) {
                    MessageBoxes.okError(error).title(TaskLocalize.dialogTitleCannotSetStateForIssue()).showAsync();
                }
            }, uiAccess);
        }, uiAccess);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        boolean enabled = project != null && !TaskManager.getManager(project).getActiveTask().isDefault();
        e.getPresentation().setEnabled(enabled);
    }
}