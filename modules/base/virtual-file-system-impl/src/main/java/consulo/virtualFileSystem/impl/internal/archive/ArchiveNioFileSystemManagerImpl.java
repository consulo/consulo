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
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keeps one file system per archive, and replaces it when the archive changes on disk.
 *
 * <p>The archive of another machine - a remote platform's - is read through a copy on this machine, kept under the
 * system directory and fetched again when the archive changes; its root still goes up to the archive's own folder.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@Singleton
@ServiceImpl
public class ArchiveNioFileSystemManagerImpl implements ArchiveNioFileSystemManager {
    private static final Logger LOG = Logger.getInstance(ArchiveNioFileSystemManagerImpl.class);
    private static final String REMOTE_ARCHIVES_DIRECTORY = "remote-archives";

    private final FileTypeRegistry myFileTypeRegistry;
    private final ArchiveNioFileSystemProvider myProvider = new ArchiveNioFileSystemProvider();
    private final ConcurrentMap<Path, ArchiveNioFileSystem> myFileSystems = new ConcurrentHashMap<>();

    @Inject
    public ArchiveNioFileSystemManagerImpl(FileTypeRegistry fileTypeRegistry) {
        myFileTypeRegistry = fileTypeRegistry;
    }

    @Override
    public @Nullable ArchivePath getRoot(Path archiveFile) {
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

        if (archiveFile.getFileSystem() != FileSystems.getDefault()) {
            return getRemoteRoot(archiveFile, viaArchiveFile);
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

    /** Another machine's archive, read through a local copy - blocking: the archive is fetched when it changed. */
    private @Nullable ArchivePath getRemoteRoot(Path archiveFile, ArchiveFileSystemViaArchiveFile viaArchiveFile) {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(archiveFile, BasicFileAttributes.class);
        }
        catch (IOException | RuntimeException e) {
            LOG.debug("Cannot read the attributes of " + archiveFile, e);
            return null;
        }
        if (attributes.isDirectory()) {
            return null;
        }

        long stamp = attributes.lastModifiedTime().toMillis();
        long length = attributes.size();
        String url = archiveFile.toUri().toString();
        try {
            ArchiveNioFileSystem result = myFileSystems.compute(archiveFile, (path, current) -> {
                if (current != null && current.isUpToDate(stamp, length)) {
                    return current;
                }
                if (current != null) {
                    current.close();
                }
                Path copy = fetchRemoteArchive(archiveFile, url);
                return new ArchiveNioFileSystem(myProvider, viaArchiveFile, archiveFile, copy, stamp, length);
            });
            return result.getRoot();
        }
        catch (UncheckedIOException e) {
            LOG.warn("Cannot fetch " + archiveFile, e.getCause());
            return null;
        }
    }

    /** The local copy of another machine's archive - one folder per archive, so its name stays the archive's. */
    private static Path fetchRemoteArchive(Path archiveFile, String url) {
        Path fileName = archiveFile.getFileName();
        Path directory = Path.of(ContainerPathManager.get().getSystemPath(), REMOTE_ARCHIVES_DIRECTORY,
            Integer.toHexString(url.hashCode()));
        Path copy = directory.resolve(fileName != null ? fileName.toString() : "archive");
        try {
            Files.createDirectories(directory);
            Files.copy(archiveFile, copy, StandardCopyOption.REPLACE_EXISTING);
            return copy;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
