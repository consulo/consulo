/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl;

import consulo.annotation.DeprecationInfo;
import consulo.util.dataholder.Key;
import consulo.util.io.BufferExposingByteArrayInputStream;
import consulo.util.io.FileAttributes;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.impl.internal.VfsImplUtil;
import consulo.virtualFileSystem.localize.VirtualFileSystemLocalize;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

/**
 * Common interface of archive-based file systems (jar://, phar:// etc).
 *
 * @since 138.100
 * <p>
 * Only for internal use. For plugin implementation please use {@link ArchiveFileSystemBase}
 */
@Deprecated
public abstract class RawArchiveFileSystem extends NewVirtualFileSystem {
    @Deprecated
    @DeprecationInfo("Use URLUtil#ARCHIVE_SEPARATOR")
    public static final String ARCHIVE_SEPARATOR = URLUtil.ARCHIVE_SEPARATOR;

    private static final Key<VirtualFile> LOCAL_FILE = Key.create("vfs.archive.local.file");

    private final Function<VirtualFile, FileAttributes> myAttrGetter =
        ManagingFS.getInstance().accessDiskWithCheckCanceled(file -> getHandler(file).getAttributes(getRelativePath(file)));
    private final Function<VirtualFile, String[]> myChildrenGetter =
        ManagingFS.getInstance().accessDiskWithCheckCanceled(file -> getHandler(file).list(getRelativePath(file)));

    /**
     * Returns a root entry of an archive hosted by a given local file
     * (i.e.: file:///path/to/jar.jar => jar:///path/to/jar.jar!/),
     * or null if the file does not host this file system.
     */
    @Nullable
    public VirtualFile getRootByLocal(@Nonnull VirtualFile file) {
        return findFileByPath(composeRootPath(file.getPath()));
    }

    /**
     * Returns a root entry of an archive which hosts a given entry file
     * (i.e.: jar:///path/to/jar.jar!/resource.xml => jar:///path/to/jar.jar!/),
     * or null if the file does not belong to this file system.
     */
    @Nullable
    public VirtualFile getRootByEntry(@Nonnull VirtualFile entry) {
        return entry.getFileSystem() != this ? null : VirtualFileUtil.getRootFile(entry);
    }

    @Nonnull
    public String getRootPathByLocal(@Nonnull VirtualFile file) {
        return composeRootPath(file.getPath());
    }

    /**
     * Returns a local file of an archive which hosts a given entry file
     * (i.e.: jar:///path/to/jar.jar!/resource.xml => file:///path/to/jar.jar),
     * or null if the file does not belong to this file system.
     */
    @Nullable
    public VirtualFile getLocalByEntry(@Nonnull VirtualFile entry) {
        if (entry.getFileSystem() != this) {
            return null;
        }

        VirtualFile root = getRootByEntry(entry);
        assert root != null : entry;

        VirtualFile local = LOCAL_FILE.get(root);
        if (local == null) {
            String localPath = extractLocalPath(root.getPath());
            local = StandardFileSystems.local().findFileByPath(localPath);
            if (local != null) {
                LOCAL_FILE.set(root, local);
            }
        }
        return local;
    }

    /**
     * Strips any separator chars from a root path (obtained via {@link #extractRootPath(String)}) to obtain a path to a local file.
     */
    @Nonnull
    public abstract String extractLocalPath(@Nonnull String rootPath);

    /**
     * A reverse to {@link #extractLocalPath(String)} - i.e. dresses a local file path to make it a suitable root path for this filesystem.
     */
    @Nonnull
    public abstract String composeRootPath(@Nonnull String localPath);

    @Nonnull
    public abstract ArchiveHandler getHandler(@Nonnull VirtualFile entryFile);

    // standard implementations

    @Override
    public int getRank() {
        return LocalFileSystem.getInstance().getRank() + 1;
    }

    @Nonnull
    @Override
    public VirtualFile copyFile(
        Object requestor,
        @Nonnull VirtualFile file,
        @Nonnull VirtualFile newParent,
        @Nonnull String copyName
    ) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Nonnull
    @Override
    public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(parent.getUrl()).get());
    }

    @Nonnull
    @Override
    public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(parent.getUrl()).get());
    }

    @Override
    public void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Override
    public void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Override
    public void renameFile(Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Nonnull
    protected String getRelativePath(@Nonnull VirtualFile file) {
        String path = file.getPath();
        String relativePath = path.substring(extractRootPath(path).length());
        return StringUtil.startsWithChar(relativePath, '/') ? relativePath.substring(1) : relativePath;
    }

    @Nullable
    @Override
    public FileAttributes getAttributes(@Nonnull VirtualFile file) {
        return myAttrGetter.apply(file);
    }

    @Nonnull
    @Override
    public String[] list(@Nonnull VirtualFile file) {
        return myChildrenGetter.apply(file);
    }

    @Override
    public boolean exists(@Nonnull VirtualFile file) {
        if (file.getParent() == null) {
            return getLocalByEntry(file) != null;
        }
        else {
            return getAttributes(file) != null;
        }
    }

    @Override
    public boolean isDirectory(@Nonnull VirtualFile file) {
        if (file.getParent() == null) {
            return true;
        }
        FileAttributes attributes = getAttributes(file);
        return attributes == null || attributes.isDirectory();
    }

    @Override
    public boolean isWritable(@Nonnull VirtualFile file) {
        return false;
    }

    @Override
    public long getTimeStamp(@Nonnull VirtualFile file) {
        if (file.getParent() == null) {
            VirtualFile host = getLocalByEntry(file);
            if (host != null) {
                return host.getTimeStamp();
            }
        }
        else {
            FileAttributes attributes = getAttributes(file);
            if (attributes != null) {
                return attributes.lastModified;
            }
        }
        return ArchiveHandler.DEFAULT_TIMESTAMP;
    }

    @Override
    public long getLength(@Nonnull VirtualFile file) {
        if (file.getParent() == null) {
            VirtualFile host = getLocalByEntry(file);
            if (host != null) {
                return host.getLength();
            }
        }
        else {
            FileAttributes attributes = getAttributes(file);
            if (attributes != null) {
                return attributes.length;
            }
        }
        return ArchiveHandler.DEFAULT_LENGTH;
    }

    @Nonnull
    @Override
    public byte[] contentsToByteArray(@Nonnull VirtualFile file) throws IOException {
        return getHandler(file).contentsToByteArray(getRelativePath(file));
    }

    @Nonnull
    @Override
    public InputStream getInputStream(@Nonnull VirtualFile file) throws IOException {
        return new BufferExposingByteArrayInputStream(contentsToByteArray(file));
    }

    @Override
    public void setTimeStamp(@Nonnull VirtualFile file, long timeStamp) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Override
    public void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Nonnull
    @Override
    public OutputStream getOutputStream(@Nonnull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    /**
     * Returns a local file of an archive which hosts a root with the given path
     * (i.e.: "jar:///path/to/jar.jar!/" => file:///path/to/jar.jar),
     * or {@code null} if the local file is of incorrect type.
     */
    @Nullable
    public VirtualFile findLocalByRootPath(@Nonnull String rootPath) {
        String localPath = extractLocalPath(rootPath);
        VirtualFile local = StandardFileSystems.local().findFileByPath(localPath);
        return local != null && isCorrectFileType(local) ? local : null;
    }

    /**
     * Implementations should return {@code false} if the given file may not host this file system.
     */
    protected boolean isCorrectFileType(@Nonnull VirtualFile local) {
        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(local.getNameSequence());
        return fileType instanceof ArchiveFileType archiveFileType && archiveFileType.getFileSystem() == this;
    }

    @Override
    public VirtualFile findFileByPath(@Nonnull String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    @Override
    public VirtualFile findFileByPathIfCached(@Nonnull String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Override
    public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    @Override
    public void refresh(boolean asynchronous) {
        VfsImplUtil.refresh(this, asynchronous);
    }
}