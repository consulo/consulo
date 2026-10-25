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
package consulo.virtualFileSystem.archive;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Opens archives as read-only NIO file systems - every archive format with an {@link ArchiveFileType}, without
 * bringing the archive into the virtual file system.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ArchiveNioFileSystemManager {
    static ArchiveNioFileSystemManager getInstance() {
        return Application.get().getInstance(ArchiveNioFileSystemManager.class);
    }

    /**
     * @param archiveFile an archive file on the default file system
     * @return the root of the archive entries, or {@code null} when the file is not a known archive
     */
    @Nullable ArchivePath getRoot(Path archiveFile);
}
