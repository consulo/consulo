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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIContent;
import consulo.ai.AIToolCallContent;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Makes the IDE see what an agent did to the files on its own side - it edits and runs commands on the disk, the
 * project view and the editors would show the files as they were.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIChatFileRefresher {
    private AIChatFileRefresher() {
    }

    /**
     * The files the tool call touched - the new ones show up in their directory.
     */
    static void refresh(AIContent.ToolCall toolCall) {
        Set<String> paths = new LinkedHashSet<>(toolCall.locations());
        for (AIToolCallContent content : toolCall.content()) {
            if (content instanceof AIToolCallContent.Diff diff) {
                paths.add(diff.path());
            }
        }
        if (paths.isEmpty()) {
            return;
        }
        executor(() -> {
            for (String path : paths) {
                Path file = toPath(path);
                if (file == null) {
                    continue;
                }
                // a new file is found by its directory, a deleted one by what is left of its path
                Path existing = file;
                while (existing != null && !Files.exists(existing)) {
                    existing = existing.getParent();
                }
                VirtualFile virtualFile = existing == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(existing);
                if (virtualFile != null) {
                    VirtualFileUtil.markDirtyAndRefresh(true, false, true, virtualFile);
                }
            }
        });
    }

    /**
     * Everything under the directory - a command of the agent may have changed any of it.
     */
    static void refreshDirectory(@Nullable Path directory) {
        if (directory == null) {
            return;
        }
        executor(() -> {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
            if (root != null) {
                VirtualFileUtil.markDirtyAndRefresh(true, true, true, root);
            }
        });
    }

    private static void executor(Runnable runnable) {
        Application.get().getInstance(ApplicationConcurrency.class).executor().execute(runnable);
    }

    private static @Nullable Path toPath(String path) {
        try {
            return Path.of(path.startsWith("file://") ? path.substring("file://".length()) : path);
        }
        catch (InvalidPathException e) {
            return null;
        }
    }
}
