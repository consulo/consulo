/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.CustomTaskState;
import consulo.task.LocalTask;
import consulo.task.localize.TaskLocalize;
import consulo.task.ui.TaskDialogPanel;
import consulo.task.ui.TaskDialogPanelProvider;
import consulo.task.util.TaskUtil;
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
public final class CloseTaskDialog extends DialogDescriptor {
    private static final String UPDATE_STATE_ENABLED = "tasks.close.task.update.state.enabled";

    private final Project myProject;
    private final LocalTask myTask;
    private @Nullable List<TaskDialogPanel> myPanels;
    private @Nullable TaskStateChooser myStateChooser;
    private @Nullable CustomTaskState myCloseState;

    private CloseTaskDialog(Project project, LocalTask task) {
        super(TaskLocalize.dialogTitleCloseTask());
        myProject = project;
        myTask = task;
    }

    /**
     * Completes with the state the issue should be moved to, or {@code null} when the state is kept; fails when cancelled.
     */
    @RequiredUIAccess
    public static CompletableFuture<@Nullable CustomTaskState> show(Project project, LocalTask task) {
        CloseTaskDialog descriptor = new CloseTaskDialog(project, task);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().thenApply(value -> descriptor.myCloseState);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        Label taskLabel = Label.create(LocalizeValue.of(TaskUtil.getTrimmedSummary(myTask)));
        taskLabel.setImage(myTask.getIcon());

        TaskStateChooser stateChooser = new TaskStateChooser(
            myProject,
            myTask,
            UPDATE_STATE_ENABLED,
            TaskLocalize.closeTaskUpdateState(),
            repository -> repository.getOptions().preferredCloseTaskState
        );
        myStateChooser = stateChooser;

        FormBuilder builder = FormBuilder.create();
        stateChooser.addTo(builder);

        VerticalLayout layout = VerticalLayout.create();
        layout.add(taskLabel);
        layout.add(builder.build());

        List<TaskDialogPanel> panels = TaskDialogPanelProvider.getCloseTaskPanels(myProject, myTask);
        for (TaskDialogPanel panel : panels) {
            layout.add(panel.getComponent());
        }
        myPanels = panels;
        return layout;
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
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
            for (TaskDialogPanel panel : panels) {
                panel.commit();
            }
        }
        TaskStateChooser chooser = myStateChooser;
        myCloseState = chooser == null ? null : chooser.getSelectedState();
        return true;
    }
}
