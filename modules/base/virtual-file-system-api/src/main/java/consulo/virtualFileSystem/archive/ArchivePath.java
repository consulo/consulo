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

import java.nio.file.Path;

/**
 * A read-only path inside an archive, on a file system served by {@link ArchiveNioFileSystemManager}.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public interface ArchivePath extends Path {
    /**
     * @return the archive file on the default file system
     */
    Path getArchiveFile();

    /**
     * @return the path of the entry inside the archive, without a leading slash - empty for the archive root
     */
    String getEntryPath();

    /**
     * @return the url of the related virtual file, e.g. {@code jar:///libs/a.jar!/org/a}
     */
    String getVirtualFileUrl();
}
