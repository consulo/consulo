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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Marks the state of the files of a project before an agent works on them, so what the agent did can be undone.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface AIChatCheckpointManager {
    static AIChatCheckpointManager getInstance(Project project) {
        return project.getInstance(AIChatCheckpointManager.class);
    }

    /**
     * The files under the directory as they are now. Changes made outside the IDE are taken in first - a file the
     * IDE did not know of yet would count as made after the checkpoint, and going back would delete it.
     *
     * @param name what the checkpoint is shown as in the history of the files
     */
    CompletableFuture<AIChatCheckpoint> create(LocalizeValue name, Path directory);
}
