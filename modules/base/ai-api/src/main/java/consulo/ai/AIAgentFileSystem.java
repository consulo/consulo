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
import consulo.application.Application;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * The files an agent reads and writes through the IDE instead of the disk: it reads what the editor shows, unsaved
 * changes included, and what it writes lands in the editor at once - undoable, and kept in the local history.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AIAgentFileSystem {
    static AIAgentFileSystem getInstance() {
        return Application.get().getInstance(AIAgentFileSystem.class);
    }

    CompletableFuture<String> readText(Path path);

    /**
     * Replaces the text of the file, creating it when it does not exist, and saves it.
     */
    CompletableFuture<?> writeText(Path path, String text);
}
