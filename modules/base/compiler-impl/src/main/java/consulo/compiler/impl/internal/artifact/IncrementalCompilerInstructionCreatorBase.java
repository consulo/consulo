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
package consulo.compiler.impl.internal.artifact;

import consulo.compiler.artifact.element.IncrementalCompilerInstructionCreator;
import consulo.compiler.artifact.element.PackagingFileFilter;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author nik
 */
public abstract class IncrementalCompilerInstructionCreatorBase implements IncrementalCompilerInstructionCreator {
    private static final Logger LOG = Logger.getInstance(IncrementalCompilerInstructionCreatorBase.class);

    protected final ArtifactsProcessingItemsBuilderContext myContext;

    public IncrementalCompilerInstructionCreatorBase(ArtifactsProcessingItemsBuilderContext context) {
        myContext = context;
    }

    @Override
    public void addFileCopyInstruction(Path file, String outputFileName) {
        addFileCopyInstruction(FileUtil.toSystemIndependentName(file.toString()), outputFileName);
    }

    protected abstract void addFileCopyInstruction(String sourcePath, String outputFileName);

    @Override
    public void addDirectoryCopyInstructions(Path directory) {
        addDirectoryCopyInstructions(directory, null);
    }

    @Override
    public void addDirectoryCopyInstructions(Path directory, @Nullable PackagingFileFilter filter) {
        Set<String> excludedPaths = myContext.getExcludedPaths();
        boolean copyExcluded = isUnderExcluded(directory, excludedPaths);
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        Deque<IncrementalCompilerInstructionCreatorBase> creators = new ArrayDeque<>();
        creators.push(this);
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(directory)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!accept(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    creators.push(creators.peek().subFolder(dir.getFileName().toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (accept(file)) {
                        creators.peek().addFileCopyInstruction(file, file.getFileName().toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (!dir.equals(directory)) {
                        creators.pop();
                    }
                    return FileVisitResult.CONTINUE;
                }

                private boolean accept(Path child) {
                    if (copyExcluded) {
                        if (fileTypeManager.isFileIgnored(child.getFileName().toString())) {
                            return false;
                        }
                    }
                    else if (excludedPaths.contains(FileUtil.toSystemIndependentName(child.toString()))) {
                        return false;
                    }
                    return filter == null || filter.accept(child, myContext.getCompileContext());
                }
            });
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    private static boolean isUnderExcluded(Path directory, Set<String> excludedPaths) {
        String path = FileUtil.toSystemIndependentName(directory.toString());
        for (String excludedPath : excludedPaths) {
            if (FileUtil.startsWith(path, excludedPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addExtractDirectoryInstruction(Path jarFile, String pathInJar) {
        String jarPath = FileUtil.toSystemIndependentName(jarFile.toString());
        String prefix = StringUtil.trimStart(pathInJar, "/");
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        try (ZipFile zipFile = new ZipFile(jarFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (!entryName.startsWith(prefix)) {
                    continue;
                }
                String relativePath = entryName.substring(prefix.length());
                int nameIndex = relativePath.lastIndexOf('/');
                IncrementalCompilerInstructionCreatorBase creator = this;
                if (nameIndex != -1) {
                    creator = (IncrementalCompilerInstructionCreatorBase) subFolderByRelativePath(relativePath.substring(0, nameIndex));
                }
                creator.addFileCopyInstruction(jarPath + URLUtil.ARCHIVE_SEPARATOR + entryName, relativePath.substring(nameIndex + 1));
            }
        }
        catch (IOException e) {
            LOG.warn(e);
            myContext.getCompileContext()
                .newError(LocalizeValue.localizeTODO("Cannot read '" + jarPath + "': " + e.getMessage()))
                .add();
        }
    }

    @Override
    public abstract IncrementalCompilerInstructionCreatorBase subFolder(String directoryName);

    @Override
    public IncrementalCompilerInstructionCreator subFolderByRelativePath(String relativeDirectoryPath) {
        List<String> folders = StringUtil.split(relativeDirectoryPath, "/");
        IncrementalCompilerInstructionCreator current = this;
        for (String folder : folders) {
            current = current.subFolder(folder);
        }
        return current;
    }
}
