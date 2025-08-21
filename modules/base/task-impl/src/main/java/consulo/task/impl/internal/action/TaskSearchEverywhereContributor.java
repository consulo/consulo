/*
 * Copyright 2013-2025 consulo.io
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

import consulo.application.progress.ProgressIndicator;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.searchEverywhere.CheckBoxSearchEverywhereToggleAction;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.impl.internal.language.TaskPsiElement;
import consulo.task.internal.CreateNewTaskAction;
import consulo.task.localize.TaskLocalize;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-08-21
 */
public class TaskSearchEverywhereContributor implements SearchEverywhereContributor<Object> {
    public static final String ID = "TaskSearchEverywhereContributor";
    private final Project myProject;

    private boolean myShowEverywhere;

    private CreateNewTaskAction myNewTaskAction = new CreateNewTaskAction();

    public TaskSearchEverywhereContributor(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public String getSearchProviderId() {
        return ID;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true;
    }

    @Nonnull
    @Override
    public String getGroupName() {
        return "Tasks";
    }

    @Override
    public int getSortWeight() {
        return 1000;
    }

    @Override
    public boolean showInFindResults() {
        return false;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions(@Nonnull Runnable onChanged) {
        return Collections.singletonList(new CheckBoxSearchEverywhereToggleAction(TaskLocalize.labelIncludeClosedTasks()) {
            @Override
            public boolean isEverywhere() {
                return myShowEverywhere;
            }

            @Override
            public void setEverywhere(boolean state) {
                myShowEverywhere = state;
                onChanged.run();
            }
        });
    }


    @Override
    public void fetchElements(@Nonnull String pattern, @Nonnull ProgressIndicator progressIndicator, @Nonnull Predicate<? super Object> predicate) {
        myNewTaskAction.setTaskName(pattern);

        predicate.test(myNewTaskAction);
        
        List<Task> cachedAndLocalTasks =
            TaskSearchSupport.getLocalAndCachedTasks(TaskManager.getManager(myProject), pattern, myShowEverywhere);

        PsiManager psiManager = PsiManager.getInstance(myProject);

        if (!processTasks(cachedAndLocalTasks, predicate, progressIndicator, psiManager)) {
            return;
        }

        List<Task> tasks = TaskSearchSupport.getRepositoriesTasks(
            TaskManager.getManager(myProject),
            pattern,
            Integer.MAX_VALUE,
            0,
            true,
            myShowEverywhere,
            progressIndicator
        );
        tasks.removeAll(cachedAndLocalTasks);

        processTasks(tasks, predicate, progressIndicator, psiManager);
    }

    private static boolean processTasks(List<Task> tasks, Predicate<Object> consumer, ProgressIndicator progressIndicator, PsiManager psiManager) {
        for (Task task : tasks) {
            progressIndicator.checkCanceled();
            if (!consumer.test(new TaskPsiElement(psiManager, task))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean processSelectedItem(@Nonnull Object element, int modifiers, @Nonnull String searchText) {
        TaskManager taskManager = TaskManager.getManager(myProject);
        if (element instanceof TaskPsiElement taskPsiElem) {
            Task task = taskPsiElem.getTask();
            LocalTask localTask = taskManager.findTask(task.getId());
            if (localTask != null) {
                taskManager.activateTask(localTask, false);
            }
            else {
                showOpenTaskDialog(myProject, task);
            }
        }
        else if (element instanceof CreateNewTaskAction taskAction) {
            LocalTask localTask = taskManager.createLocalTask(taskAction.getTaskName());

            showOpenTaskDialog(myProject, localTask);
        }

        return true;
    }

    private static void showOpenTaskDialog(@Nonnull Project project, Task task) {
        IdeEventQueueProxy.getInstance().closeAllPopups();

        project.getApplication().invokeLater(() -> new OpenTaskDialog(project, task).show());
    }

    @Nonnull
    @Override
    public ListCellRenderer<? super Object> getElementsRenderer() {
        return new TaskCellRenderer(myProject);
    }

    @Nullable
    @Override
    public Object getDataForItem(@Nonnull Object element, @Nonnull Key dataId) {
        return null;
    }
}
