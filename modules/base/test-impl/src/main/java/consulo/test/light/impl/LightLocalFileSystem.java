/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.io.NioFiles;
import consulo.virtualFileSystem.LocalFileOperationsHandler;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.FileSystemUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Read-only {@link LocalFileSystem} which reads the disk directly, without persistent caches, refresh or file watching.
 *
 * @author VISTALL
 * @since 2023-11-05
 */
@ExtensionImpl
public class LightLocalFileSystem extends LocalFileSystem {
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        Path file = NioFiles.toPath(path);
        if (file == null) {
            return null;
        }

        Path absoluteFile = file.toAbsolutePath().normalize();
        return Files.exists(absoluteFile) ? new LightLocalVirtualFile(this, absoluteFile) : null;
    }

    @Override
    public @Nullable VirtualFile findFileByPathIfCached(String path) {
        return findFileByPath(path);
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
        return findFileByPath(path);
    }

    @Override
    public void refresh(boolean asynchronous) {
    }

    @Override
    public void refreshIoFiles(Iterable<? extends File> files) {
    }

    @Override
    public void refreshIoFiles(Iterable<? extends File> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
        if (onFinish != null) {
            onFinish.run();
        }
    }

    @Override
    public void refreshFiles(Iterable<? extends VirtualFile> files) {
    }

    @Override
    public void refreshFiles(Iterable<? extends VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
        if (onFinish != null) {
            onFinish.run();
        }
    }

    @Override
    public Set<WatchRequest> replaceWatchedRoots(
        Collection<WatchRequest> watchRequests,
        @Nullable Collection<String> recursiveRoots,
        @Nullable Collection<String> flatRoots
    ) {
        return Collections.emptySet();
    }

    @Override
    public void registerAuxiliaryFileOperationsHandler(LocalFileOperationsHandler handler) {
    }

    @Override
    public void unregisterAuxiliaryFileOperationsHandler(LocalFileOperationsHandler handler) {
    }

    @Override
    public String extractRootPath(String path) {
        Path file = NioFiles.toPath(path);
        Path root = file == null ? null : file.getRoot();
        return root == null ? "" : FileUtil.toSystemIndependentName(root.toString());
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public @Nullable Path getNioPath(VirtualFile file) {
        return file instanceof LightLocalVirtualFile lightFile ? lightFile.toNioPath() : null;
    }

    @Override
    public @Nullable FileAttributes getAttributes(VirtualFile file) {
        Path path = getNioPath(file);
        return path == null ? null : FileSystemUtil.getAttributes(path.toString());
    }

    @Override
    public boolean exists(VirtualFile file) {
        return getAttributes(file) != null;
    }

    @Override
    public String[] list(VirtualFile file) {
        Path path = getNioPath(file);
        if (path == null) {
            return ArrayUtil.EMPTY_STRING_ARRAY;
        }

        List<Path> children = NioFiles.list(path);
        String[] names = new String[children.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = NioFiles.getFileName(children.get(i));
        }
        return names;
    }

    @Override
    public boolean isDirectory(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isDirectory();
    }

    @Override
    public boolean isSymLink(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isSymLink();
    }

    @Override
    public @Nullable String resolveSymLink(VirtualFile file) {
        return FileSystemUtil.resolveSymLink(file.getPath());
    }

    @Override
    public boolean isWritable(VirtualFile file) {
        return false;
    }

    @Override
    public void setWritable(VirtualFile file, boolean writableFlag) throws IOException {
        throw new UnsupportedOperationException("setWritable() not supported");
    }

    @Override
    public long getTimeStamp(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP;
    }

    @Override
    public void setTimeStamp(VirtualFile file, long timeStamp) throws IOException {
        throw new UnsupportedOperationException("setTimeStamp() not supported");
    }

    @Override
    public long getLength(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.length : DEFAULT_LENGTH;
    }

    @Override
    public byte[] contentsToByteArray(VirtualFile file) throws IOException {
        return Files.readAllBytes(getRequiredNioPath(file));
    }

    @Override
    public InputStream getInputStream(VirtualFile file) throws IOException {
        return Files.newInputStream(getRequiredNioPath(file));
    }

    @Override
    public OutputStream getOutputStream(VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
        throw new UnsupportedOperationException("getOutputStream() not supported");
    }

    @Override
    @RequiredWriteAction
    public VirtualFile createChildDirectory(@Nullable Object requestor, VirtualFile parent, String dir) throws IOException {
        throw new UnsupportedOperationException("createChildDirectory() not supported");
    }

    @Override
    @RequiredWriteAction
    public VirtualFile createChildFile(@Nullable Object requestor, VirtualFile parent, String file) throws IOException {
        throw new UnsupportedOperationException("createChildFile() not supported");
    }

    @Override
    @RequiredWriteAction
    public void deleteFile(@Nullable Object requestor, VirtualFile file) throws IOException {
        throw new UnsupportedOperationException("deleteFile() not supported");
    }

    @Override
    @RequiredWriteAction
    public void moveFile(@Nullable Object requestor, VirtualFile file, VirtualFile newParent) throws IOException {
        throw new UnsupportedOperationException("moveFile() not supported");
    }

    @Override
    @RequiredWriteAction
    public void renameFile(@Nullable Object requestor, VirtualFile file, String newName) throws IOException {
        throw new UnsupportedOperationException("renameFile() not supported");
    }

    @Override
    @RequiredWriteAction
    public VirtualFile copyFile(@Nullable Object requestor, VirtualFile file, VirtualFile newParent, String copyName) throws IOException {
        throw new UnsupportedOperationException("copyFile() not supported");
    }

    private Path getRequiredNioPath(VirtualFile file) throws IOException {
        Path path = getNioPath(file);
        if (path == null) {
            throw new IOException("Not a file of " + this + ": " + file);
        }
        return path;
    }
}
