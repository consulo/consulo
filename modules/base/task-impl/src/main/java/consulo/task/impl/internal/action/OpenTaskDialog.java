/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.task.impl.internal.action;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.task.CustomTaskState;
import consulo.task.Task;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.TaskType;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.localize.TaskLocalize;
import consulo.task.ui.TaskDialogPanel;
import consulo.task.ui.TaskDialogPanelProvider;
import consulo.task.util.TaskUtil;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Dmitry Avdeev
 */
public final class OpenTaskDialog extends DialogDescriptor {
    private static final Logger LOG = Logger.getInstance(OpenTaskDialog.class);
    private static final String UPDATE_STATE_ENABLED = "tasks.open.task.update.state.enabled";

    private final Project myProject;
    private final Task myTask;
    private @Nullable List<TaskDialogPanel> myPanels;
    private @Nullable CheckBox myClearContext;
    private @Nullable TaskStateChooser myStateChooser;

    private OpenTaskDialog(Project project, Task task) {
        super(TaskLocalize.dialogTitleOpenTask());
        myProject = project;
        myTask = task;
    }

    @RequiredUIAccess
    public static CompletableFuture<?> show(Project project, Task task) {
        OpenTaskDialog descriptor = new OpenTaskDialog(project, task);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync();
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TaskManagerImpl taskManager = (TaskManagerImpl) TaskManager.getManager(myProject);

        Label taskLabel = Label.create(LocalizeValue.of(TaskUtil.getTrimmedSummary(myTask)));
        taskLabel.setImage(myTask.getIcon());

        TaskManagerImpl.Config state = taskManager.getState();
        CheckBox clearContext = CheckBox.create(TaskLocalize.openTaskClearContext(), state.clearContext);
        clearContext.addValueListener(event -> state.clearContext = event.getValue());
        myClearContext = clearContext;

        TaskStateChooser stateChooser = new TaskStateChooser(
            myProject,
            myTask,
            UPDATE_STATE_ENABLED,
            TaskLocalize.openTaskUpdateState(),
            repository -> repository.getOptions().preferredOpenTaskState
        );
        myStateChooser = stateChooser;

        FormBuilder builder = FormBuilder.create();
        stateChooser.addTo(builder);
        builder.addBottom(clearContext);

        VerticalLayout layout = VerticalLayout.create();
        layout.add(taskLabel);
        layout.add(builder.build());

        List<TaskDialogPanel> panels = TaskDialogPanelProvider.getOpenTaskPanels(myProject, myTask);
        for (TaskDialogPanel panel : panels) {
            layout.add(panel.getComponent());
        }
        myPanels = panels;
        return layout;
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        List<TaskDialogPanel> panels = myPanels;
        if (panels != null) {
            for (TaskDialogPanel panel : panels) {
                Component component = panel.getPreferredFocusedComponent();
                if (component != null) {
                    return component;
                }
            }
        }
        TaskStateChooser chooser = myStateChooser;
        return chooser == null ? null : chooser.getPreferredFocusedComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (!(action instanceof DialogOkAction)) {
            return true;
        }
        List<TaskDialogPanel> panels = myPanels;
        if (panels != null) {
            for (TaskDialogPanel panel : panels) {
                LocalizeValue error = panel.validate();
                if (error != null) {
                    MessageBoxes.okError(error).showAsync();
                    return false;
                }
            }
        }
        createTask();
        return true;
    }

    @RequiredUIAccess
    private void createTask() {
        TaskManagerImpl taskManager = (TaskManagerImpl) TaskManager.getManager(myProject);
        TaskStateChooser chooser = myStateChooser;
        CustomTaskState taskState = chooser == null ? null : chooser.getSelectedState();
        TaskRepository repository = myTask.getRepository();
        if (repository != null && taskState != null) {
            taskManager.setTaskState(repository, myTask, taskState, true).whenCompleteAsync((result, error) -> {
                if (error != null) {
                    LOG.warn(error);
                    MessageBoxes.okError(error).title(TaskLocalize.dialogTitleCannotSetStateForIssue()).showAsync();
                }
            }, UIAccess.current());
        }

        CheckBox clearContext = myClearContext;
        taskManager.activateTask(myTask, clearContext != null && clearContext.getValueOrError());
        if (myTask.getType() == TaskType.EXCEPTION && AnalyzeTaskStacktraceAction.hasTexts(myTask)) {
            AnalyzeTaskStacktraceAction.analyzeStacktrace(myTask, myProject);
        }
        List<TaskDialogPanel> panels = myPanels;
        if (panels != null) {
            for (TaskDialogPanel panel : panels) {
                panel.commit();
            }
        }
    }
}
