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
package consulo.ide.impl.idea.packaging.impl.compiler;

import consulo.compiler.CompileContext;
import consulo.compiler.CompilerMessageCategory;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ArchiveFileSystem;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.ide.impl.idea.packaging.impl.artifacts.ArtifactUtil;
import consulo.ide.impl.idea.packaging.impl.elements.FileOrDirectoryCopyPackagingElement;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.application.AccessRule;
import consulo.logging.Logger;
import consulo.util.collection.Maps;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
  public static Pair<InputStream, Long> getArchiveEntryInputStream(VirtualFile sourceFile, final CompileContext context) throws IOException {
    final String fullPath = sourceFile.getPath();
    final int jarEnd = fullPath.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR);
    LOG.assertTrue(jarEnd != -1, fullPath);
    String pathInJar = fullPath.substring(jarEnd + ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
    String jarPath = fullPath.substring(0, jarEnd);
    final ZipFile jarFile = new ZipFile(new File(FileUtil.toSystemDependentName(jarPath)));
    final ZipEntry entry = jarFile.getEntry(pathInJar);
    if (entry == null) {
      context.addMessage(CompilerMessageCategory.ERROR, "Cannot extract '" + pathInJar + "' from '" + jarFile.getName() + "': entry not found", null, -1, -1);
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
    return new File(FileUtil.toSystemDependentName(fullPath.substring(fullPath.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR))));
  }

  @Nonnull
  public static Set<VirtualFile> getArtifactOutputsContainingSourceFiles(final @Nonnull Project project) {
    final List<VirtualFile> allOutputs = new ArrayList<VirtualFile>();
    for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
      ContainerUtil.addIfNotNull(artifact.getOutputFile(), allOutputs);
    }

    final Set<VirtualFile> roots = new HashSet<VirtualFile>();
    final PackagingElementResolvingContext context = ArtifactManager.getInstance(project).getResolvingContext();
    for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
      Processor<PackagingElement<?>> processor = new Processor<PackagingElement<?>>() {
        @Override
        public boolean process(@Nonnull PackagingElement<?> element) {
          if (element instanceof FileOrDirectoryCopyPackagingElement<?>) {
            final VirtualFile file = ((FileOrDirectoryCopyPackagingElement)element).findFile();
            if (file != null) {
              roots.add(file);
            }
          }
          return true;
        }
      };
      ArtifactUtil.processRecursivelySkippingIncludedArtifacts(artifact, processor, context);
    }

    final Set<VirtualFile> affectedOutputPaths = new HashSet<VirtualFile>();
    for (VirtualFile output : allOutputs) {
      for (VirtualFile root : roots) {
        if (VfsUtilCore.isAncestor(output, root, false)) {
          affectedOutputPaths.add(output);
        }
      }
    }
    return affectedOutputPaths;
  }

  public static MultiMap<String, Artifact> createOutputToArtifactMap(final Project project) {
    final MultiMap<String, Artifact> result = new MultiMap<String, Artifact>() {
      @Nonnull
      @Override
      protected Map<String, Collection<Artifact>> createMap() {
        return Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
      }
    };
    AccessRule.read(() -> {
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
