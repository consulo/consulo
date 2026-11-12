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
package consulo.virtualFileSystem.impl.internal.zip;

import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.virtualFileSystem.impl.internal.ArchiveFileSystemViaArchiveFile;
import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * An archive of another platform, read where it is - through the {@link Path} of its machine.
 * <p>
 * The archive is not checked for changes on every access - that is a round trip to the machine; a change of the host file
 * comes as a file event, which disposes the handler.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public class PlatformArchiveHandler extends ZipHandler {
    private final String myHostPath;
    private final VirtualFileSystem myHostFileSystem;
    private final ArchiveFileSystemViaArchiveFile myArchiveFileSystem;

    /**
     * @param hostPath the {@link VirtualFile#getPath() path} of the host file in {@code hostFileSystem}
     */
    public PlatformArchiveHandler(String hostPath, VirtualFileSystem hostFileSystem, ArchiveFileSystemViaArchiveFile archiveFileSystem) {
        super(hostPath);
        myHostPath = hostPath;
        myHostFileSystem = hostFileSystem;
        myArchiveFileSystem = archiveFileSystem;
    }

    private Path getHostNioPath() throws IOException {
        VirtualFile host = myHostFileSystem.findFileByPath(myHostPath);
        Path path = host == null ? null : myHostFileSystem.getNioPath(host);
        if (path == null) {
            throw new FileNotFoundException(myHostPath);
        }
        return path;
    }

    @Override
    protected ArchiveFile openArchiveFile() throws IOException {
        Path path = getHostNioPath();
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        setFileStamp(attributes.lastModifiedTime().toMillis(), attributes.size());
        return createArchiveFile(path);
    }

    @Override
    protected boolean isChangedSinceOpened() {
        return false;
    }

    @Override
    public ArchiveFile createArchiveFile(Path path) throws IOException {
        return myArchiveFileSystem.createArchiveFile(path);
    }

    @Override
    public @Nullable FileAttributes getAttributes(String relativePath) {
        if (!relativePath.isEmpty()) {
            return super.getAttributes(relativePath);
        }
        // the root - the archive exists while its host file does
        VirtualFile host = myHostFileSystem.findFileByPath(myHostPath);
        return host == null || !host.exists()
            ? null
            : new FileAttributes(true, false, false, false, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, false);
    }
}
