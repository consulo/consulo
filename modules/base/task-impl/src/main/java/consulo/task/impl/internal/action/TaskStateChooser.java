/*
 * Copyright 2013-2026 consulo.io
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

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.task.CustomTaskState;
import consulo.task.Task;
import consulo.task.TaskRepository;
import consulo.task.issue.IssueStateSupport;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class TaskStateChooser {
    private final @Nullable IssueStateSupport mySupport;
    private final CheckBox myUpdateState;
    private final MutableFlatDataModel<CustomTaskState> myStates = FlatDataModel.of(List.of());
    private final ComboBox<CustomTaskState> myStateBox;
    private boolean myLoaded;

    @RequiredUIAccess
    TaskStateChooser(
        Project project,
        Task task,
        String enabledKey,
        LocalizeValue label,
        Function<TaskRepository, @Nullable CustomTaskState> preferred
    ) {
        TaskRepository repository = task.isIssue() ? task.getRepository() : null;
        mySupport = repository == null ? null : repository.getIssueTracker().getSupport(IssueStateSupport.class);
        myUpdateState = CheckBox.create(label, ProjectPropertiesComponent.getInstance(project).getBoolean(enabledKey, false));
        myStateBox = ComboBox.create(myStates);
        myStateBox.setTextRenderer(state -> state == null ? LocalizeValue.empty() : LocalizeValue.of(state.getPresentableName()));
        myStateBox.setEnabled(myUpdateState.getValueOrError());
        myUpdateState.addValueListener(event -> {
            ProjectPropertiesComponent.getInstance(project).setValue(enabledKey, event.getValue(), false);
            myStateBox.setEnabled(event.getValue());
            if (event.getValue()) {
                load(task, repository, preferred);
            }
        });
        if (myUpdateState.getValueOrError()) {
            load(task, repository, preferred);
        }
    }

    boolean isAvailable() {
        return mySupport != null;
    }

    @RequiredUIAccess
    void addTo(FormBuilder builder) {
        if (isAvailable()) {
            builder.addLabeled(myUpdateState, myStateBox);
        }
    }

    @RequiredUIAccess
    private void load(Task task, @Nullable TaskRepository repository, Function<TaskRepository, @Nullable CustomTaskState> preferred) {
        IssueStateSupport support = mySupport;
        if (myLoaded || support == null || repository == null) {
            return;
        }
        myLoaded = true;
        support.getAvailableStates(task).whenCompleteAsync((states, error) -> {
            if (states == null) {
                return;
            }
            myStates.replaceAll(List.copyOf(states));
            CustomTaskState state = preferred.apply(repository);
            if (state != null && states.contains(state)) {
                myStateBox.setValue(state);
            }
            else if (!states.isEmpty()) {
                myStateBox.setValue(states.iterator().next());
            }
        }, UIAccess.current());
    }

    @RequiredUIAccess
    @Nullable CustomTaskState getSelectedState() {
        return isAvailable() && myUpdateState.getValueOrError() ? myStateBox.getValue() : null;
    }

    @Nullable ComboBox<CustomTaskState> getPreferredFocusedComponent() {
        return isAvailable() && myUpdateState.getValueOrError() ? myStateBox : null;
    }
}
