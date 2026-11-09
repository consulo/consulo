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
package consulo.task.impl.internal;

import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.TaskRepositoryType;
import consulo.task.icon.TaskIconGroup;
import consulo.task.impl.internal.timeTracking.TimeTrackingManager;
import consulo.task.localize.TaskLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class TasksToolWindowState {
    private TasksToolWindowState() {
    }

    public static boolean isAvailable(Project project) {
        return TimeTrackingManager.getInstance(project).isTimeTrackingToolWindowAvailable()
            || TaskManager.getManager(project).getAllRepositories().length > 0;
    }

    @RequiredUIAccess
    public static void update(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.TASKS);
        if (toolWindow == null) {
            return;
        }

        Set<TaskRepositoryType<?>> types = new LinkedHashSet<>();
        for (TaskRepository repository : TaskManager.getManager(project).getAllRepositories()) {
            types.add(repository.getRepositoryType());
        }

        if (types.size() == 1) {
            TaskRepositoryType<?> type = types.iterator().next();
            toolWindow.setIcon(type.getIcon());
            toolWindow.setDisplayName(TaskLocalize.toolwindowIssuesTitle(type.getPresentableName().get()));
        }
        else {
            toolWindow.setIcon(TaskIconGroup.clock());
            toolWindow.setDisplayName(TaskLocalize.toolwindowTasksTitle());
        }
        toolWindow.setAvailable(isAvailable(project));
    }
}
