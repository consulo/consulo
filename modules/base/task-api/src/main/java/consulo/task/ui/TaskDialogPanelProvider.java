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
package consulo.task.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TaskDialogPanelProvider {
    @RequiredUIAccess
    public static List<TaskDialogPanel> getOpenTaskPanels(Project project, Task task) {
        List<TaskDialogPanel> panels = new ArrayList<>();
        project.getApplication().getExtensionPoint(TaskDialogPanelProvider.class).forEach(provider -> {
            TaskDialogPanel panel = provider.getOpenTaskPanel(project, task);
            if (panel != null) {
                panels.add(panel);
            }
        });
        return panels;
    }

    @RequiredUIAccess
    public static List<TaskDialogPanel> getCloseTaskPanels(Project project, LocalTask task) {
        List<TaskDialogPanel> panels = new ArrayList<>();
        project.getApplication().getExtensionPoint(TaskDialogPanelProvider.class).forEach(provider -> {
            TaskDialogPanel panel = provider.getCloseTaskPanel(project, task);
            if (panel != null) {
                panels.add(panel);
            }
        });
        return panels;
    }

    @RequiredUIAccess
    public abstract @Nullable TaskDialogPanel getOpenTaskPanel(Project project, Task task);

    @RequiredUIAccess
    public abstract @Nullable TaskDialogPanel getCloseTaskPanel(Project project, LocalTask task);
}
