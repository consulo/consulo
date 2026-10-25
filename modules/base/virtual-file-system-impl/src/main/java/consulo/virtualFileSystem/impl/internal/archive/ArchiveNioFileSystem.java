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

import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.virtualFileSystem.archive.ArchiveHandler;
import consulo.virtualFileSystem.impl.internal.ArchiveFileSystemViaArchiveFile;
import consulo.virtualFileSystem.impl.internal.zip.ZipHandler;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Set;

/**
 * A read-only file system over one archive. The entries come from the {@link ArchiveFile} of the archive's virtual
 * file system, through an {@link ArchiveHandler} of its own - the archive is never brought into the virtual file
 * system.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class ArchiveNioFileSystem extends FileSystem {
    private final ArchiveNioFileSystemProvider myProvider;
    private final ArchiveFileSystemViaArchiveFile myArchiveFileSystem;
    private final Path myArchiveFile;
    private final long myArchiveStamp;
    private final long myArchiveLength;
    private final ArchiveHandler myHandler;
    private final ArchivePathImpl myRoot;

    private volatile boolean myOpen = true;

    ArchiveNioFileSystem(ArchiveNioFileSystemProvider provider,
                         ArchiveFileSystemViaArchiveFile archiveFileSystem,
                         Path archiveFile,
                         FileAttributes attributes) {
        myProvider = provider;
        myArchiveFileSystem = archiveFileSystem;
        myArchiveFile = archiveFile;
        myArchiveStamp = attributes.lastModified;
        myArchiveLength = attributes.length;
        myHandler = new ZipHandler(archiveFile.toString()) {
            @Override
            public ArchiveFile createArchiveFile(String path) throws IOException {
                return archiveFileSystem.createArchiveFile(path);
            }
        };
        myRoot = new ArchivePathImpl(this, "/");
    }

    boolean isUpToDate(FileAttributes attributes) {
        return myOpen && attributes.lastModified == myArchiveStamp && attributes.length == myArchiveLength;
    }

    ArchivePathImpl getRoot() {
        return myRoot;
    }

    Path getArchiveFile() {
        return myArchiveFile;
    }

    ArchiveHandler getHandler() {
        return myHandler;
    }

    String getVirtualFileUrl(String entryPath) {
        String localPath = FileUtil.toSystemIndependentName(myArchiveFile.toString());
        return VirtualFileManager.constructUrl(myArchiveFileSystem.getProtocol(), myArchiveFileSystem.composeRootPath(localPath) + entryPath);
    }

    String getProtocol() {
        return myArchiveFileSystem.getProtocol();
    }

    @Override
    public FileSystemProvider provider() {
        return myProvider;
    }

    @Override
    public void close() {
        if (myOpen) {
            myOpen = false;
            myHandler.dispose();
        }
    }

    @Override
    public boolean isOpen() {
        return myOpen;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return List.of(myRoot);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return List.of();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of("basic");
    }

    @Override
    public Path getPath(String first, String... more) {
        StringBuilder builder = new StringBuilder(first);
        for (String segment : more) {
            if (segment.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('/');
            }
            builder.append(segment);
        }
        return new ArchivePathImpl(this, builder.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return myArchiveFile.toString();
    }
}
