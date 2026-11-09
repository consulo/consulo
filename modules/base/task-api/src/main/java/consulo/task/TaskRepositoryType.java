/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.task;

import consulo.accountManagement.Account;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.ui.BaseRepositoryEditor;
import consulo.task.ui.TaskRepositoryEditor;
import consulo.ui.image.Image;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * The main extension point for issue tracking integration.
 *
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TaskRepositoryType<O extends TaskRepositoryOptions> {
    public abstract String getId();

    public abstract LocalizeValue getPresentableName();

    public abstract Image getIcon();

    public @Nullable String getAdvertiser() {
        return null;
    }

    public abstract String getAccountTypeId();

    public abstract O createOptions();

    public abstract TaskRepository createRepository(Project project, O options);

    public final O copyOptions(TaskRepositoryOptions options) {
        return TaskRepositoryOptionsUtil.deserializeInto(TaskRepositoryOptionsUtil.serialize(options), createOptions());
    }

    public final O readOptions(Element element) {
        return TaskRepositoryOptionsUtil.deserializeInto(element, createOptions());
    }

    public List<O> detect(Project project, Account account) {
        return List.of(createOptions());
    }

    public String getDetectionKey(O options) {
        return getId() + ":" + options.accountId + ":" + options.serverUrl;
    }

    public TaskRepositoryEditor createEditor(O options, Project project, Consumer<O> changeListener) {
        return new BaseRepositoryEditor<>(project, this, options, changeListener);
    }

    @SuppressWarnings("unchecked")
    public final TaskRepository createRepositoryUnchecked(Project project, TaskRepositoryOptions options) {
        return createRepository(project, (O) options);
    }

    @SuppressWarnings("unchecked")
    public final String getDetectionKeyUnchecked(TaskRepositoryOptions options) {
        return getDetectionKey((O) options);
    }

    @SuppressWarnings("unchecked")
    public final TaskRepositoryEditor createEditorUnchecked(TaskRepositoryOptions options, Project project, Consumer<TaskRepositoryOptions> changeListener) {
        return createEditor((O) options, project, (Consumer<O>) changeListener);
    }

    public EnumSet<TaskState> getPossibleTaskStates() {
        return EnumSet.noneOf(TaskState.class);
    }
}
