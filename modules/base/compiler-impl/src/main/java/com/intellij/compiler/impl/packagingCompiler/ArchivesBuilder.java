/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.compiler.impl.packagingCompiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.compiler.ArtifactCompilerUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.DFSTBuilder;
import com.intellij.util.graph.GraphGenerator;
import consulo.compiler.impl.packagingCompiler.ArchivePackageWriterEx;
import consulo.logging.Logger;
import consulo.packaging.elements.ArchivePackageWriter;
import consulo.packaging.impl.util.DeploymentUtilImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

/**
 * @author nik
 */
public class ArchivesBuilder {
  public static final Logger LOGGER = Logger.getInstance(ArchivesBuilder.class);

  private final Set<ArchivePackageInfo> myArchivesToBuild;
  private final FileFilter myFileFilter;
  private final CompileContext myContext;
  private Map<ArchivePackageInfo, File> myBuiltArchives;

  public ArchivesBuilder(@Nonnull Set<ArchivePackageInfo> archivesToBuild, @Nonnull FileFilter fileFilter, @Nonnull CompileContext context) {
    DependentArchivesEvaluator evaluator = new DependentArchivesEvaluator();
    for (ArchivePackageInfo archivePackageInfo : archivesToBuild) {
      evaluator.addArchiveWithDependencies(archivePackageInfo);
    }
    myArchivesToBuild = evaluator.getArchivePackageInfos();
    myFileFilter = fileFilter;
    myContext = context;
  }

  public boolean buildArchives(Set<String> writtenPaths) throws IOException {
    myContext.getProgressIndicator().setText(CompilerBundle.message("packaging.compiler.message.building.archives"));

    final ArchivePackageInfo[] sortedArchives = sortArchives();
    if (sortedArchives == null) {
      return false;
    }

    myBuiltArchives = new HashMap<>();
    try {
      for (ArchivePackageInfo archivePackageInfo : sortedArchives) {
        myContext.getProgressIndicator().checkCanceled();
        buildArchive(archivePackageInfo);
      }

      myContext.getProgressIndicator().setText(CompilerBundle.message("packaging.compiler.message.copying.archives"));
      copyJars(writtenPaths);
    }
    finally {
      deleteTemporaryJars();
    }


    return true;
  }

  private void deleteTemporaryJars() {
    for (File file : myBuiltArchives.values()) {
      FileUtil.delete(file);
    }
  }

  private void copyJars(final Set<String> writtenPaths) throws IOException {
    for (Map.Entry<ArchivePackageInfo, File> entry : myBuiltArchives.entrySet()) {
      File fromFile = entry.getValue();
      boolean first = true;
      for (DestinationInfo destination : entry.getKey().getAllDestinations()) {
        if (destination instanceof ExplodedDestinationInfo) {
          File toFile = new File(FileUtil.toSystemDependentName(destination.getOutputPath()));

          if (first) {
            first = false;
            renameFile(fromFile, toFile, writtenPaths);
            fromFile = toFile;
          }
          else {
            DeploymentUtilImpl.copyFile(fromFile, toFile, myContext, writtenPaths, myFileFilter);
          }

        }
      }
    }
  }

  private static void renameFile(final File fromFile, final File toFile, final Set<String> writtenPaths) throws IOException {
    FileUtil.rename(fromFile, toFile);
    writtenPaths.add(toFile.getPath());
  }

  @Nullable
  private ArchivePackageInfo[] sortArchives() {
    final DFSTBuilder<ArchivePackageInfo> builder = new DFSTBuilder<>(GraphGenerator.create(CachingSemiGraph.create(new ArchivesGraph())));
    if (!builder.isAcyclic()) {
      final Pair<ArchivePackageInfo, ArchivePackageInfo> dependency = builder.getCircularDependency();
      String message = CompilerBundle
              .message("packaging.compiler.error.cannot.build.circular.dependency.found.between.0.and.1", dependency.getFirst().getPresentableDestination(),
                       dependency.getSecond().getPresentableDestination());
      myContext.addMessage(CompilerMessageCategory.ERROR, message, null, -1, -1);
      return null;
    }

    ArchivePackageInfo[] archives = myArchivesToBuild.toArray(new ArchivePackageInfo[myArchivesToBuild.size()]);
    Arrays.sort(archives, builder.comparator());
    archives = ArrayUtil.reverseArray(archives);
    return archives;
  }

  public Set<ArchivePackageInfo> getArchivesToBuild() {
    return myArchivesToBuild;
  }

  @SuppressWarnings("unchecked")
  private <T> void buildArchive(final ArchivePackageInfo archive) throws IOException {
    if (archive.getPackedFiles().isEmpty() && archive.getPackedArchives().isEmpty()) {
      myContext.addMessage(CompilerMessageCategory.WARNING, "Archive '" + archive.getPresentableDestination() + "' has no files so it won't be created", null,
                           -1, -1);
      return;
    }

    myContext.getProgressIndicator().setText(CompilerBundle.message("packaging.compiler.message.building.0", archive.getPresentableDestination()));
    File tempFile = File.createTempFile("artifactCompiler", "tmp");

    myBuiltArchives.put(archive, tempFile);

    FileUtil.createParentDirs(tempFile);

    ArchivePackageWriter<T> packageWriter = (ArchivePackageWriter<T>)archive.getPackageWriter();

    T archiveFile;

    if (packageWriter instanceof ArchivePackageWriterEx) {
      archiveFile = ((ArchivePackageWriterEx<T>)packageWriter).createArchiveObject(tempFile, archive);
    }
    else {
      archiveFile = packageWriter.createArchiveObject(tempFile);
    }

    try {
      final Set<String> writtenPaths = new HashSet<>();
      for (Pair<String, VirtualFile> pair : archive.getPackedFiles()) {
        final VirtualFile sourceFile = pair.getSecond();
        if (sourceFile.isInLocalFileSystem()) {
          File file = VfsUtil.virtualToIoFile(sourceFile);
          addFileToArchive(archiveFile, packageWriter, file, pair.getFirst(), writtenPaths);
        }
        else {
          extractFileAndAddToArchive(archiveFile, packageWriter, sourceFile, pair.getFirst(), writtenPaths);
        }
      }

      for (Pair<String, ArchivePackageInfo> nestedArchive : archive.getPackedArchives()) {
        File nestedArchiveFile = myBuiltArchives.get(nestedArchive.getSecond());
        if (nestedArchiveFile != null) {
          addFileToArchive(archiveFile, packageWriter, nestedArchiveFile, nestedArchive.getFirst(), writtenPaths);
        }
        else {
          LOGGER.debug("nested archive file " + nestedArchive.getFirst() + " for " + archive.getPresentableDestination() + " not found");
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      packageWriter.close(archiveFile);
    }
  }

  private <T> void extractFileAndAddToArchive(@Nonnull T archiveObject,
                                              @Nonnull ArchivePackageWriter<T> writer,
                                              VirtualFile sourceFile,
                                              String relativePath,
                                              Set<String> writtenPaths) throws IOException {
    relativePath = addParentDirectories(archiveObject, writer, writtenPaths, relativePath);
    myContext.getProgressIndicator().setText2(relativePath);
    if (!writtenPaths.add(relativePath)) return;

    Pair<InputStream, Long> streamLongPair = ArtifactCompilerUtil.getArchiveEntryInputStream(sourceFile, myContext);
    final InputStream input = streamLongPair.getFirst();
    if (input == null) {
      return;
    }

    try {
      writer.addFile(archiveObject, input, relativePath, streamLongPair.getSecond(), ArtifactCompilerUtil.getArchiveFile(sourceFile).lastModified());
    }
    finally {
      input.close();
    }
  }

  private <T> void addFileToArchive(@Nonnull T archiveObject,
                                    @Nonnull ArchivePackageWriter<T> writer,
                                    @Nonnull File file,
                                    @Nonnull String relativePath,
                                    @Nonnull Set<String> writtenPaths) throws IOException {
    if (!file.exists()) {
      return;
    }

    if (!FileUtil.isFilePathAcceptable(file, myFileFilter)) {
      return;
    }

    if (!writtenPaths.add(relativePath)) {
      return;
    }

    relativePath = addParentDirectories(archiveObject, writer, writtenPaths, relativePath);

    myContext.getProgressIndicator().setText2(relativePath);

    try (FileInputStream fileOutputStream = new FileInputStream(file)) {
      writer.addFile(archiveObject, fileOutputStream, relativePath, file.length(), file.lastModified());
    }
  }

  private static <T> String addParentDirectories(@Nonnull T archiveObject,
                                                 @Nonnull ArchivePackageWriter<T> writer,
                                                 Set<String> writtenPaths,
                                                 String relativePath) throws IOException {
    while (StringUtil.startsWithChar(relativePath, '/')) {
      relativePath = relativePath.substring(1);
    }
    int i = relativePath.indexOf('/');
    while (i != -1) {
      String prefix = relativePath.substring(0, i + 1);
      if (!writtenPaths.contains(prefix) && prefix.length() > 1) {

        writer.addDirectory(archiveObject, prefix);

        writtenPaths.add(prefix);
      }
      i = relativePath.indexOf('/', i + 1);
    }
    return relativePath;
  }

  private class ArchivesGraph implements GraphGenerator.SemiGraph<ArchivePackageInfo> {
    @Override
    public Collection<ArchivePackageInfo> getNodes() {
      return myArchivesToBuild;
    }

    @Override
    public Iterator<ArchivePackageInfo> getIn(final ArchivePackageInfo n) {
      Set<ArchivePackageInfo> ins = new HashSet<>();
      for (ArchiveDestinationInfo destination : n.getArchiveDestinations()) {
        ins.add(destination.getArchivePackageInfo());
      }
      return ins.iterator();
    }
  }
}
