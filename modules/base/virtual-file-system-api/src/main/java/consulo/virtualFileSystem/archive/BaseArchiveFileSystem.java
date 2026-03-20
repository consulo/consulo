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
package consulo.virtualFileSystem.archive;

import consulo.annotation.DeprecationInfo;
import consulo.platform.Platform;
import consulo.util.dataholder.Key;
import consulo.util.io.BufferExposingByteArrayInputStream;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import consulo.virtualFileSystem.localize.VirtualFileSystemLocalize;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

/**
 * Common implementation of {@link ArchiveFileSystem}
 */
public abstract class BaseArchiveFileSystem extends NewVirtualFileSystem implements ArchiveFileSystem {
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
    public @Nullable VirtualFile getRootByLocal(VirtualFile file) {
        return findFileByPath(composeRootPath(file.getPath()));
    }

    /**
     * Returns a root entry of an archive which hosts a given entry file
     * (i.e.: jar:///path/to/jar.jar!/resource.xml => jar:///path/to/jar.jar!/),
     * or null if the file does not belong to this file system.
     */
    public @Nullable VirtualFile getRootByEntry(VirtualFile entry) {
        return entry.getFileSystem() != this ? null : VirtualFileUtil.getRootFile(entry);
    }

    
    public String getRootPathByLocal(VirtualFile file) {
        return composeRootPath(file.getPath());
    }

    /**
     * Returns a local file of an archive which hosts a given entry file
     * (i.e.: jar:///path/to/jar.jar!/resource.xml => file:///path/to/jar.jar),
     * or null if the file does not belong to this file system.
     */
    public @Nullable VirtualFile getLocalByEntry(VirtualFile entry) {
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
    
    public String extractLocalPath(String rootPath) {
        return StringUtil.trimEnd(rootPath, URLUtil.ARCHIVE_SEPARATOR);
    }

    /**
     * A reverse to {@link #extractLocalPath(String)} - i.e. dresses a local file path to make it a suitable root path for this filesystem.
     */
    
    public String composeRootPath(String localPath) {
        return localPath + URLUtil.ARCHIVE_SEPARATOR;
    }

    @Override
    public @Nullable VirtualFile getLocalVirtualFileFor(@Nullable VirtualFile entryVFile) {
        return getVirtualFileForJar(entryVFile);
    }

    @Override
    public @Nullable VirtualFile findLocalVirtualFileByPath(String path) {
        if (!path.contains(URLUtil.ARCHIVE_SEPARATOR)) {
            path += URLUtil.ARCHIVE_SEPARATOR;
        }
        return findFileByPath(path);
    }

    public @Nullable VirtualFile getVirtualFileForJar(@Nullable VirtualFile entryFile) {
        return entryFile == null ? null : getLocalByEntry(entryFile);
    }

    public @Nullable VirtualFile getJarRootForLocalFile(VirtualFile file) {
        return getRootByLocal(file);
    }

    
    @Override
    public String extractPresentableUrl(String path) {
        return super.extractPresentableUrl(StringUtil.trimEnd(path, URLUtil.ARCHIVE_SEPARATOR));
    }

    @Override
    public String normalize(String path) {
        int jarSeparatorIndex = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        if (jarSeparatorIndex > 0) {
            String root = path.substring(0, jarSeparatorIndex);
            return FileUtil.normalize(root, Platform.current().os().isWindows()) + path.substring(jarSeparatorIndex);
        }
        return super.normalize(path);
    }

    
    @Override
    public String extractRootPath(String path) {
        int jarSeparatorIndex = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        assert jarSeparatorIndex >= 0 : "Path passed to ArchiveFileSystem must have archive separator '!/': " + path;
        return path.substring(0, jarSeparatorIndex + URLUtil.ARCHIVE_SEPARATOR.length());
    }

    
    public abstract ArchiveHandler getHandler(VirtualFile entryFile);

    // standard implementations

    @Override
    public int getRank() {
        return LocalFileSystem.getInstance().getRank() + 1;
    }

    
    @Override
    public VirtualFile copyFile(
            Object requestor,
            VirtualFile file,
            VirtualFile newParent,
            String copyName
    ) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    
    @Override
    public VirtualFile createChildDirectory(Object requestor, VirtualFile parent, String dir) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(parent.getUrl()).get());
    }

    
    @Override
    public VirtualFile createChildFile(Object requestor, VirtualFile parent, String file) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(parent.getUrl()).get());
    }

    @Override
    public void deleteFile(Object requestor, VirtualFile file) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Override
    public void moveFile(Object requestor, VirtualFile file, VirtualFile newParent) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Override
    public void renameFile(Object requestor, VirtualFile file, String newName) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    
    protected String getRelativePath(VirtualFile file) {
        String path = file.getPath();
        String relativePath = path.substring(extractRootPath(path).length());
        return StringUtil.startsWithChar(relativePath, '/') ? relativePath.substring(1) : relativePath;
    }

    @Override
    public @Nullable FileAttributes getAttributes(VirtualFile file) {
        return myAttrGetter.apply(file);
    }

    
    @Override
    public String[] list(VirtualFile file) {
        return myChildrenGetter.apply(file);
    }

    @Override
    public boolean exists(VirtualFile file) {
        if (file.getParent() == null) {
            return getLocalByEntry(file) != null;
        } else {
            return getAttributes(file) != null;
        }
    }

    @Override
    public boolean isDirectory(VirtualFile file) {
        if (file.getParent() == null) {
            return true;
        }
        FileAttributes attributes = getAttributes(file);
        return attributes == null || attributes.isDirectory();
    }

    @Override
    public boolean isWritable(VirtualFile file) {
        return false;
    }

    @Override
    public long getTimeStamp(VirtualFile file) {
        if (file.getParent() == null) {
            VirtualFile host = getLocalByEntry(file);
            if (host != null) {
                return host.getTimeStamp();
            }
        } else {
            FileAttributes attributes = getAttributes(file);
            if (attributes != null) {
                return attributes.lastModified;
            }
        }
        return ArchiveHandler.DEFAULT_TIMESTAMP;
    }

    @Override
    public long getLength(VirtualFile file) {
        if (file.getParent() == null) {
            VirtualFile host = getLocalByEntry(file);
            if (host != null) {
                return host.getLength();
            }
        } else {
            FileAttributes attributes = getAttributes(file);
            if (attributes != null) {
                return attributes.length;
            }
        }
        return ArchiveHandler.DEFAULT_LENGTH;
    }

    
    @Override
    public byte[] contentsToByteArray(VirtualFile file) throws IOException {
        return getHandler(file).contentsToByteArray(getRelativePath(file));
    }

    
    @Override
    public InputStream getInputStream(VirtualFile file) throws IOException {
        return new BufferExposingByteArrayInputStream(contentsToByteArray(file));
    }

    @Override
    public void setTimeStamp(VirtualFile file, long timeStamp) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    @Override
    public void setWritable(VirtualFile file, boolean writableFlag) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    
    @Override
    public OutputStream getOutputStream(VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
        throw new IOException(VirtualFileSystemLocalize.jarModificationNotSupportedError(file.getUrl()).get());
    }

    /**
     * Returns a local file of an archive which hosts a root with the given path
     * (i.e.: "jar:///path/to/jar.jar!/" => file:///path/to/jar.jar),
     * or {@code null} if the local file is of incorrect type.
     */
    public @Nullable VirtualFile findLocalByRootPath(String rootPath) {
        String localPath = extractLocalPath(rootPath);
        VirtualFile local = StandardFileSystems.local().findFileByPath(localPath);
        return local != null && isCorrectFileType(local) ? local : null;
    }

    /**
     * Implementations should return {@code false} if the given file may not host this file system.
     */
    protected boolean isCorrectFileType(VirtualFile local) {
        FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(local.getNameSequence());
        return fileType instanceof ArchiveFileType archiveFileType && archiveFileType.getFileSystem() == this;
    }

    @Override
    public VirtualFile findFileByPath(String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    @Override
    public VirtualFile findFileByPathIfCached(String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Override
    public VirtualFile refreshAndFindFileByPath(String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    @Override
    public void refresh(boolean asynchronous) {
        VfsImplUtil.refresh(this, asynchronous);
    }
}