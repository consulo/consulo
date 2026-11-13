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
package consulo.virtualFileSystem.impl.internal.watcher;

import consulo.virtualFileSystem.NewVirtualFileSystem;

import java.nio.file.Path;

/**
 * A virtual file system whose files are watched by the {@link FileWatchServiceImpl} - it says which watched paths are its files.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public interface WatchedFileSystem {
    /**
     * True for a path of a java file system whose files are files of this one.
     */
    boolean isWatchedPath(Path path);

    /**
     * The path of the file, as {@link consulo.virtualFileSystem.VirtualFile#getPath()} of this file system answers it.
     */
    String toVfsPath(Path path);

    /**
     * Changes of files of this file system were marked dirty - nothing else refreshes them, when this file system is not refreshed
     * on its own (on activation of the frame, as the local one is).
     */
    default void dirtyFilesMarked() {
    }

    default NewVirtualFileSystem asFileSystem() {
        return (NewVirtualFileSystem) this;
    }
}
