/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.compiler.impl.internal;

import consulo.application.ReadAction;
import consulo.compiler.CompileContext;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.FileOrDirectoryCopyPackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author nik
 */
public class ArtifactCompilerUtil {
    private static final Logger LOG = Logger.getInstance(ArtifactCompilerUtil.class);

    private ArtifactCompilerUtil() {
    }

    @Nonnull
    public static Pair<InputStream, Long> getArchiveEntryInputStream(VirtualFile sourceFile, CompileContext context) throws IOException {
        String fullPath = sourceFile.getPath();
        int jarEnd = fullPath.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        LOG.assertTrue(jarEnd != -1, fullPath);
        String pathInJar = fullPath.substring(jarEnd + URLUtil.ARCHIVE_SEPARATOR.length());
        String jarPath = fullPath.substring(0, jarEnd);
        final ZipFile jarFile = new ZipFile(new File(FileUtil.toSystemDependentName(jarPath)));
        final ZipEntry entry = jarFile.getEntry(pathInJar);
        if (entry == null) {
            context.addMessage(
                CompilerMessageCategory.ERROR,
                "Cannot extract '" + pathInJar + "' from '" + jarFile.getName() + "': entry not found",
                null,
                -1,
                -1
            );
            return Pair.empty();
        }

        BufferedInputStream bufferedInputStream = new BufferedInputStream(jarFile.getInputStream(entry)) {
            @Override
            public void close() throws IOException {
                super.close();
                jarFile.close();
            }
        };
        return Pair.<InputStream, Long>create(bufferedInputStream, entry.getSize());
    }

    public static File getArchiveFile(VirtualFile file) {
        String fullPath = file.getPath();
        return new File(FileUtil.toSystemDependentName(fullPath.substring(fullPath.indexOf(URLUtil.ARCHIVE_SEPARATOR))));
    }

    @Nonnull
    public static Set<VirtualFile> getArtifactOutputsContainingSourceFiles(@Nonnull Project project) {
        List<VirtualFile> allOutputs = new ArrayList<>();
        for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
            ContainerUtil.addIfNotNull(allOutputs, artifact.getOutputFile());
        }

        Set<VirtualFile> roots = new HashSet<>();
        PackagingElementResolvingContext context = ArtifactManager.getInstance(project).getResolvingContext();
        for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
            Predicate<PackagingElement<?>> processor = element -> {
                if (element instanceof FileOrDirectoryCopyPackagingElement fileOrDirectoryCopyPackagingElement) {
                    VirtualFile file = fileOrDirectoryCopyPackagingElement.findFile();
                    if (file != null) {
                        roots.add(file);
                    }
                }
                return true;
            };
            ArtifactUtil.processRecursivelySkippingIncludedArtifacts(artifact, processor, context);
        }

        Set<VirtualFile> affectedOutputPaths = new HashSet<>();
        for (VirtualFile output : allOutputs) {
            for (VirtualFile root : roots) {
                if (VirtualFileUtil.isAncestor(output, root, false)) {
                    affectedOutputPaths.add(output);
                }
            }
        }
        return affectedOutputPaths;
    }

    public static MultiMap<String, Artifact> createOutputToArtifactMap(Project project) {
        MultiMap<String, Artifact> result = new MultiMap<>(Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY));
        ReadAction.run(() -> {
            for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
                String outputPath = artifact.getOutputFilePath();
                if (!StringUtil.isEmpty(outputPath)) {
                    result.putValue(outputPath, artifact);
                }
            }
        });
        return result;
    }
}
