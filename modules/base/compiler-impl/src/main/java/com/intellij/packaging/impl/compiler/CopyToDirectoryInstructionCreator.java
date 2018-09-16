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
package com.intellij.packaging.impl.compiler;

import com.intellij.compiler.impl.packagingCompiler.ExplodedDestinationInfo;
import com.intellij.compiler.impl.packagingCompiler.ArchivePackageInfo;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.packaging.elements.ArchivePackageWriter;
import com.intellij.packaging.elements.IncrementalCompilerInstructionCreator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class CopyToDirectoryInstructionCreator extends IncrementalCompilerInstructionCreatorBase {
  private final String myOutputPath;
  private final @Nullable VirtualFile myOutputFile;

  public CopyToDirectoryInstructionCreator(ArtifactsProcessingItemsBuilderContext context, String outputPath,
                                           @Nullable VirtualFile outputFile) {
    super(context);
    myOutputPath = outputPath;
    myOutputFile = outputFile;
  }

  @Override
  public void addFileCopyInstruction(@Nonnull VirtualFile file, @Nonnull String outputFileName) {
    myContext.addDestination(file, new ExplodedDestinationInfo(myOutputPath + "/" + outputFileName, outputChild(outputFileName)));
  }

  @Override
  public CopyToDirectoryInstructionCreator subFolder(@Nonnull String directoryName) {
    return new CopyToDirectoryInstructionCreator(myContext, myOutputPath + "/" + directoryName, outputChild(directoryName));
  }

  @Nonnull
  @Override
  public IncrementalCompilerInstructionCreator archive(@Nonnull String archiveFileName, @Nonnull ArchivePackageWriter<?> packageWriter) {
    String jarOutputPath = myOutputPath + "/" + archiveFileName;
    final ArchivePackageInfo archivePackageInfo = new ArchivePackageInfo(packageWriter);
    if (!myContext.registerJarFile(archivePackageInfo, jarOutputPath)) {
      return new SkipAllInstructionCreator(myContext);
    }
    VirtualFile outputFile = outputChild(archiveFileName);
    final ExplodedDestinationInfo destination = new ExplodedDestinationInfo(jarOutputPath, outputFile);
    archivePackageInfo.addDestination(destination);
    return new PackIntoArchiveInstructionCreator(myContext, archivePackageInfo, "", destination);
  }

  @Nullable
  private VirtualFile outputChild(String name) {
    return myOutputFile != null ? myOutputFile.findChild(name) : null;
  }
}
