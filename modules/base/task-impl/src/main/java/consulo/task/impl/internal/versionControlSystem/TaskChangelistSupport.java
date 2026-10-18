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

package consulo.task.impl.internal.versionControlSystem;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionContributor;
import consulo.project.Project;
import consulo.task.ChangeListInfo;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.task.impl.internal.action.TaskAutoCompletionListProvider;
import consulo.task.localize.TaskLocalize;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.versionControlSystem.change.EditChangelistSupport;
import consulo.versionControlSystem.change.LocalChangeList;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TaskChangelistSupport implements EditChangelistSupport {
    private final Project myProject;
    private final TaskManager myTaskManager;

    @Inject
    public TaskChangelistSupport(Project project, TaskManager taskManager) {
        myProject = project;
        myTaskManager = taskManager;
    }

    @Override
    @RequiredUIAccess
    public void installSearch(EditorBox name, EditorBox comment) {
        TaskAutoCompletionListProvider completionProvider = new TaskAutoCompletionListProvider(myProject);

        TextFieldWithAutoCompletionContributor.installCompletion(name.getDocument(), myProject, completionProvider, false);
    }

    @Override
    @RequiredUIAccess
    public Consumer<LocalChangeList> addControls(HorizontalLayout bottomPanel, @Nullable LocalChangeList initial) {
        CheckBox checkBox = CheckBox.create(TaskLocalize.changelistTrackContextCheckbox());
        checkBox.setToolTipText(TaskLocalize.changelistTrackContextTooltip());
        checkBox.setValue(initial == null ? myTaskManager.isTrackContextForNewChangelist() : myTaskManager.getAssociatedTask(initial) != null);
        bottomPanel.add(checkBox);
        return changeList -> {
            boolean selected = checkBox.getValueOrError();
            if (initial == null) {
                myTaskManager.setTrackContextForNewChangelist(selected);
                if (selected) {
                    myTaskManager.trackContext(changeList);
                }
                else {
                    myTaskManager.getActiveTask().addChangelist(new ChangeListInfo(changeList));
                }
            }
            else {
                LocalTask associatedTask = myTaskManager.getAssociatedTask(changeList);
                if (selected) {
                    if (associatedTask == null) {
                        myTaskManager.trackContext(changeList);
                    }
                }
                else {
                    if (associatedTask != null) {
                        myTaskManager.removeTask(associatedTask);
                    }
                }
            }
        };
    }
}
