/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.compiler.scope;

import consulo.compiler.util.ExportableUserDataHolderBase;
import consulo.module.Module;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * @author 2003-01-20
 */
public class FileSetCompileScope extends ExportableUserDataHolderBase implements CompileScope {
    private final Set<Path> myRootFiles = new HashSet<>();
    private final Set<String> myDirectoryPaths = new HashSet<>();
    private Set<String> myPaths = null;
    private final Module[] myAffectedModules;
    private final boolean myIncludeTestScope;

    public FileSetCompileScope(Collection<Path> files, Module[] modules) {
        this(files, modules, true);
    }

    public FileSetCompileScope(Collection<Path> files, Module[] modules, boolean includeTestScope) {
        myAffectedModules = modules;
        myIncludeTestScope = includeTestScope;
        for (Path file : files) {
            addFile(file);
        }
    }

    @Override
    public boolean includeTestScope() {
        return myIncludeTestScope;
    }

    @Override
    public Module[] getAffectedModules() {
        return myAffectedModules;
    }

    @Override
    public Collection<Path> getFiles(FileType fileType) {
        List<Path> files = new ArrayList<>();
        for (Path file : myRootFiles) {
            if (Files.isDirectory(file)) {
                addRecursively(files, file, fileType);
            }
            else if (Files.exists(file) && matches(file, fileType)) {
                files.add(file);
            }
        }
        return files;
    }

    @Override
    public boolean belongs(Path path) {
        String normalizedPath = FileUtil.toSystemIndependentName(path.toString());
        if (getPaths().contains(normalizedPath)) {
            return true;
        }
        for (String directoryPath : myDirectoryPaths) {
            if (FileUtil.startsWith(normalizedPath, directoryPath)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getPaths() {
        if (myPaths == null) {
            myPaths = new HashSet<>();
            for (Path file : myRootFiles) {
                myPaths.add(FileUtil.toSystemIndependentName(file.toString()));
            }
        }
        return myPaths;
    }

    private void addFile(Path file) {
        if (Files.isDirectory(file)) {
            myDirectoryPaths.add(FileUtil.toSystemIndependentName(file.toString()) + "/");
        }
        myRootFiles.add(file);
        myPaths = null;
    }

    private static boolean matches(Path file, FileType fileType) {
        return fileType == null
            || fileType.equals(FileTypeRegistry.getInstance().getFileTypeByFileName(file.getFileName().toString()));
    }

    private static void addRecursively(Collection<Path> container, Path fromDirectory, FileType fileType) {
        try {
            Files.walkFileTree(fromDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matches(file, fileType)) {
                        container.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException ignored) {
        }
    }
}
