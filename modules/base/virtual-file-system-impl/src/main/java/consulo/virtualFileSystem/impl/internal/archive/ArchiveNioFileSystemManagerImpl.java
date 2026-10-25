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
package consulo.virtualFileSystem.impl.internal.archive;

import consulo.annotation.component.ServiceImpl;
import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.internal.FileSystemUtil;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.archive.ArchiveNioFileSystemManager;
import consulo.virtualFileSystem.archive.ArchivePath;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.impl.internal.ArchiveFileSystemViaArchiveFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps one file system per archive, and replaces it when the archive changes on disk.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@Singleton
@ServiceImpl
public class ArchiveNioFileSystemManagerImpl implements ArchiveNioFileSystemManager {
    private final FileTypeRegistry myFileTypeRegistry;
    private final ArchiveNioFileSystemProvider myProvider = new ArchiveNioFileSystemProvider();
    private final ConcurrentMap<Path, ArchiveNioFileSystem> myFileSystems = new ConcurrentHashMap<>();

    @Inject
    public ArchiveNioFileSystemManagerImpl(FileTypeRegistry fileTypeRegistry) {
        myFileTypeRegistry = fileTypeRegistry;
    }

    @Override
    public @Nullable ArchivePath getRoot(Path archiveFile) {
        if (archiveFile.getFileSystem() != FileSystems.getDefault()) {
            return null;
        }

        Path fileName = archiveFile.getFileName();
        if (fileName == null) {
            return null;
        }

        FileType fileType = myFileTypeRegistry.getFileTypeByFileName(fileName.toString());
        if (!(fileType instanceof ArchiveFileType archiveFileType)) {
            return null;
        }

        ArchiveFileSystem fileSystem = archiveFileType.getFileSystem();
        if (!(fileSystem instanceof ArchiveFileSystemViaArchiveFile viaArchiveFile)) {
            return null;
        }

        Path absolute = archiveFile.toAbsolutePath().normalize();
        FileAttributes attributes = FileSystemUtil.getAttributes(absolute.toString());
        if (attributes == null || attributes.isDirectory()) {
            return null;
        }

        ArchiveNioFileSystem result = myFileSystems.compute(absolute, (path, current) -> {
            if (current != null && current.isUpToDate(attributes)) {
                return current;
            }
            if (current != null) {
                current.close();
            }
            return new ArchiveNioFileSystem(myProvider, viaArchiveFile, path, attributes);
        });
        return result.getRoot();
    }
}
