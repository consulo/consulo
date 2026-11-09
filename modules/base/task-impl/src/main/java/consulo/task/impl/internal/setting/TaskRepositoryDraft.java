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
package consulo.task.impl.internal.setting;

import consulo.task.TaskRepositoryOptions;
import consulo.task.TaskRepositoryOptionsUtil;
import consulo.task.TaskRepositoryType;
import consulo.task.impl.internal.TaskRepositoryEntry;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class TaskRepositoryDraft {
    final TaskRepositoryType<?> type;
    final TaskRepositoryOptions options;
    final @Nullable TaskRepositoryOptions original;
    final boolean detected;

    TaskRepositoryDraft(TaskRepositoryType<?> type, TaskRepositoryOptions options, @Nullable TaskRepositoryOptions original, boolean detected) {
        this.type = type;
        this.options = options;
        this.original = original;
        this.detected = detected;
    }

    static TaskRepositoryDraft of(TaskRepositoryEntry entry) {
        return new TaskRepositoryDraft(
            entry.getType(),
            entry.getType().copyOptions(entry.getOptions()),
            entry.getType().copyOptions(entry.getOptions()),
            entry.isDetected()
        );
    }

    boolean isChanged() {
        return original == null || !TaskRepositoryOptionsUtil.isEqual(original, options);
    }

    boolean isConfigured() {
        return !detected || isChanged();
    }

    String getKey() {
        return type.getDetectionKeyUnchecked(original == null ? options : original);
    }

    TaskRepositoryEntry toEntry() {
        return new TaskRepositoryEntry(type, type.copyOptions(options), false);
    }
}
