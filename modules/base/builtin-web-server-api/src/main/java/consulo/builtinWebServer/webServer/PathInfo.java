// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.webServer;

import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class PathInfo {
    private final @Nullable Path myIoFile;
    private volatile @Nullable VirtualFile myFile;
    private final VirtualFile myRoot;
    private volatile @Nullable String myModuleName;
    private final boolean myLibrary;
    private final boolean myRootNameOptionalInPath;

    private volatile @Nullable String myPath;
    private volatile @Nullable String myRootLessPath;
    private volatile @Nullable String myFilePath;

    public PathInfo(@Nullable Path ioFile, @Nullable VirtualFile file, VirtualFile root) {
        this(ioFile, file, root, null, false, false);
    }

    public PathInfo(@Nullable Path ioFile, @Nullable VirtualFile file, VirtualFile root, @Nullable String moduleName) {
        this(ioFile, file, root, moduleName, false, false);
    }

    public PathInfo(@Nullable Path ioFile, @Nullable VirtualFile file, VirtualFile root, @Nullable String moduleName, boolean isLibrary) {
        this(ioFile, file, root, moduleName, isLibrary, false);
    }

    public PathInfo(@Nullable Path ioFile,
                    @Nullable VirtualFile file,
                    VirtualFile root,
                    @Nullable String moduleName,
                    boolean isLibrary,
                    boolean isRootNameOptionalInPath) {
        myIoFile = ioFile;
        myFile = file;
        myRoot = root;
        myModuleName = moduleName;
        myLibrary = isLibrary;
        myRootNameOptionalInPath = isRootNameOptionalInPath;
    }

    public @Nullable Path getIoFile() {
        return myIoFile;
    }

    public @Nullable VirtualFile getFile() {
        return myFile;
    }

    public VirtualFile getRoot() {
        return myRoot;
    }

    public @Nullable String getModuleName() {
        return myModuleName;
    }

    public void setModuleName(@Nullable String moduleName) {
        myModuleName = moduleName;
    }

    public boolean isLibrary() {
        return myLibrary;
    }

    public boolean isRootNameOptionalInPath() {
        return myRootNameOptionalInPath;
    }

    /**
     * URL path.
     */
    public String getPath() {
        String path = myPath;
        if (path == null) {
            path = buildPath(true);
            myPath = path;
        }
        return path;
    }

    public @Nullable String getRootLessPathIfPossible() {
        if (!myRootNameOptionalInPath) {
            return null;
        }

        String path = myRootLessPath;
        if (path == null) {
            path = buildPath(false);
            myRootLessPath = path;
        }
        return path;
    }

    private String buildPath(boolean useRootName) {
        StringBuilder builder = new StringBuilder();
        String moduleName = myModuleName;
        if (moduleName != null) {
            builder.append(moduleName).append('/');
        }

        if (myLibrary) {
            builder.append(myRoot.getName()).append('/');
        }

        VirtualFile rootParent = myRoot.getParent();
        VirtualFile relativeTo = useRootName || rootParent == null ? myRoot : rootParent;
        VirtualFile file = myFile;
        if (file == null) {
            String ioFilePath = Objects.requireNonNull(myIoFile).toString().replace(File.separatorChar, '/');
            builder.append(FileUtil.getRelativePath(relativeTo.getPath(), ioFilePath, '/'));
        }
        else {
            builder.append(VirtualFileUtil.getRelativePath(file, relativeTo, '/'));
        }
        return builder.toString();
    }

    public @Nullable VirtualFile getOrResolveVirtualFile() {
        VirtualFile file = myFile;
        if (file == null) {
            String ioFilePath = Objects.requireNonNull(myIoFile).toString().replace(File.separatorChar, '/');
            VirtualFile result = LocalFileSystem.getInstance().findFileByPath(ioFilePath);
            myFile = result;
            return result;
        }
        return file;
    }

    /**
     * System-dependent path to file.
     */
    public String getFilePath() {
        String filePath = myFilePath;
        if (filePath == null) {
            filePath = myIoFile != null ? myIoFile.toString() : FileUtil.toSystemDependentName(Objects.requireNonNull(myFile).getPath());
            myFilePath = filePath;
        }
        return filePath;
    }

    public boolean isValid() {
        return myIoFile == null ? Objects.requireNonNull(myFile).isValid() : Files.exists(myIoFile);
    }

    public String getName() {
        if (myIoFile != null) {
            Path fileName = myIoFile.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        }
        return Objects.requireNonNull(myFile).getName();
    }

    public FileType getFileType() {
        if (myIoFile == null) {
            return Objects.requireNonNull(myFile).getFileType();
        }
        return FileTypeRegistry.getInstance().getFileTypeByFileName(getName());
    }

    public boolean isDirectory() {
        return myIoFile != null ? Files.isDirectory(myIoFile) : Objects.requireNonNull(myFile).isDirectory();
    }
}
