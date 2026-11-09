/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.tasks;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.task.TaskRepository;
import consulo.task.TaskRepositoryType;
import consulo.task.TaskState;
import consulo.task.ui.BaseRepositoryEditor;
import consulo.task.ui.TaskRepositoryEditor;
import consulo.ui.CheckBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;

import java.util.EnumSet;
import java.util.function.Consumer;

@ExtensionImpl
public class SandRepositoryType extends TaskRepositoryType<SandRepositoryOptions> {
    @Override
    public String getId() {
        return "Sand";
    }

    @Override
    public LocalizeValue getPresentableName() {
        return LocalizeValue.of("Sand");
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolbarUnknown();
    }

    @Override
    public String getAccountTypeId() {
        return "sandTracker";
    }

    @Override
    public SandRepositoryOptions createOptions() {
        return new SandRepositoryOptions();
    }

    @Override
    public TaskRepository createRepository(Project project, SandRepositoryOptions options) {
        return new SandTaskRepository(this, project, options);
    }

    @Override
    public EnumSet<TaskState> getPossibleTaskStates() {
        return EnumSet.of(TaskState.OPEN, TaskState.IN_PROGRESS, TaskState.RESOLVED);
    }

    @Override
    public TaskRepositoryEditor createEditor(SandRepositoryOptions options, Project project, Consumer<SandRepositoryOptions> changeListener) {
        return new BaseRepositoryEditor<>(project, this, options, changeListener) {
            @RequiredUIAccess
            @Override
            protected void addCustomRows(FormBuilder builder) {
                TextBox projectKeyBox = TextBox.create(myOptions.projectKey);
                projectKeyBox.addValueListener(event -> {
                    myOptions.projectKey = StringUtil.notNullize(event.getValue());
                    fireChanged();
                });
                builder.addLabeled(LocalizeValue.of("Project key:"), projectKeyBox);

                CheckBox assignedBox = CheckBox.create(LocalizeValue.of("Assigned to me only"), myOptions.assignedToMeOnly);
                assignedBox.addValueListener(event -> {
                    myOptions.assignedToMeOnly = event.getValue();
                    fireChanged();
                });
                builder.addLabeled(LocalizeValue.of("Filter:"), assignedBox);
            }
        };
    }
}
