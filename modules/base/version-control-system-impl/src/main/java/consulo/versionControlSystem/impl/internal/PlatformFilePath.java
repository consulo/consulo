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
package consulo.versionControlSystem.impl.internal;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.internal.PlatformPathVirtualFileSystem;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * A path of a file of another machine - a remote project's. It is kept as {@link VirtualFile#getPath()} answers it
 * ({@code <platform id><path>}), so it compares, hashes and nests with the virtual files of that platform, and resolves through
 * the file system of the platform.
 * <p>
 * It has no {@link File} - such a file would be of this machine.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public class PlatformFilePath implements FilePath {
    private final String myPath;
    private final boolean myIsDirectory;
    private int myHash;

    public PlatformFilePath(String vfsPath, boolean isDirectory) {
        myPath = FileUtil.toCanonicalPath(vfsPath);
        myIsDirectory = isDirectory;
    }

    public static @Nullable PlatformPathVirtualFileSystem getFileSystem() {
        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.PLATFORM_PROTOCOL);
        return fileSystem instanceof PlatformPathVirtualFileSystem platformFileSystem ? platformFileSystem : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlatformFilePath path = (PlatformFilePath) o;
        return myIsDirectory == path.myIsDirectory && myPath.equals(path.myPath);
    }

    @Override
    public int hashCode() {
        int result = myHash;
        if (result == 0) {
            result = myPath.hashCode();
            result = 31 * result + (myIsDirectory ? 1 : 0);
            myHash = result;
        }
        return result;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void hardRefresh() {
        VirtualFileSystem fileSystem = (VirtualFileSystem) getFileSystem();
        if (fileSystem != null) {
            fileSystem.refreshAndFindFileByPath(myPath);
        }
    }

    @Override
    public String getPath() {
        return myPath;
    }

    @Override
    public boolean isDirectory() {
        return myIsDirectory;
    }

    @Override
    public boolean isUnder(FilePath parent, boolean strict) {
        // paths of a platform are case-sensitive here, as its virtual files are
        return FileUtil.isAncestor(parent.getPath(), getPath(), strict);
    }

    @Override
    public @Nullable FilePath getParentPath() {
        String parent = PathUtil.getParentPath(myPath);
        return parent.isEmpty() ? null : new PlatformFilePath(parent, true);
    }

    @Override
    public @Nullable VirtualFile getVirtualFile() {
        VirtualFileSystem fileSystem = (VirtualFileSystem) getFileSystem();
        return fileSystem != null ? fileSystem.findFileByPath(myPath) : null;
    }

    @Override
    public @Nullable VirtualFile getVirtualFileParent() {
        FilePath parent = getParentPath();
        return parent != null ? parent.getVirtualFile() : null;
    }

    @Override
    public File getIOFile() {
        throw new UnsupportedOperationException("A file of another machine has no java.io.File, use getNioPath(): " + myPath);
    }

    @Override
    public @Nullable Path getNioPath() {
        PlatformPathVirtualFileSystem fileSystem = getFileSystem();
        return fileSystem != null ? fileSystem.toNioPath(myPath) : null;
    }

    @Override
    public String getName() {
        return PathUtil.getFileName(myPath);
    }

    @Override
    public String getPresentableUrl() {
        PlatformPathVirtualFileSystem fileSystem = getFileSystem();
        return fileSystem != null ? fileSystem.extractPresentableUrl(myPath) : myPath;
    }

    @Override
    public @Nullable Document getDocument() {
        VirtualFile file = getVirtualFile();
        if (file == null || file.getFileType().isBinary()) {
            return null;
        }
        return FileDocumentManager.getInstance().getDocument(file);
    }

    @Override
    public Charset getCharset() {
        return getCharset(null);
    }

    @Override
    public Charset getCharset(@Nullable Project project) {
        VirtualFileSystem fileSystem = (VirtualFileSystem) getFileSystem();
        VirtualFile file = getVirtualFile();
        String path = myPath;
        while ((file == null || !file.isValid()) && !path.isEmpty() && fileSystem != null) {
            path = PathUtil.getParentPath(path);
            file = fileSystem.findFileByPath(path);
        }
        if (file != null) {
            return file.getCharset();
        }
        EncodingManager e = project == null ? EncodingManager.getInstance() : EncodingProjectManager.getInstance(project);
        return e.getDefaultCharset();
    }

    @Override
    public FileType getFileType() {
        VirtualFile file = getVirtualFile();
        FileTypeRegistry manager = FileTypeRegistry.getInstance();
        return file != null ? manager.getFileTypeByFile(file) : manager.getFileTypeByFileName(getName());
    }

    @Override
    public String toString() {
        return myPath + (myIsDirectory ? "/" : "");
    }

    @Override
    public boolean isNonLocal() {
        return false;
    }
}
