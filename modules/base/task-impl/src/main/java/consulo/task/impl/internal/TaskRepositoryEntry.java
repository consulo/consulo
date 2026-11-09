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
import consulo.task.TaskRepository;
import consulo.task.TaskRepositoryOptions;
import consulo.task.TaskRepositoryType;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class TaskRepositoryEntry {
    private final TaskRepositoryType<?> myType;
    private final TaskRepositoryOptions myOptions;
    private final boolean myDetected;

    public TaskRepositoryEntry(TaskRepositoryType<?> type, TaskRepositoryOptions options, boolean detected) {
        myType = type;
        myOptions = options;
        myDetected = detected;
    }

    public TaskRepositoryType<?> getType() {
        return myType;
    }

    public TaskRepositoryOptions getOptions() {
        return myOptions;
    }

    public boolean isDetected() {
        return myDetected;
    }

    public String getKey() {
        return myType.getDetectionKeyUnchecked(myOptions);
    }

    public TaskRepositoryEntry copy(boolean detected) {
        return new TaskRepositoryEntry(myType, myType.copyOptions(myOptions), detected);
    }

    public TaskRepository createRepository(Project project) {
        return myType.createRepositoryUnchecked(project, myOptions);
    }
}
