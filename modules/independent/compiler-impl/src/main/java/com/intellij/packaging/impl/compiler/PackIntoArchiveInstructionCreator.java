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

import com.intellij.compiler.impl.packagingCompiler.DestinationInfo;
import com.intellij.compiler.impl.packagingCompiler.ArchiveDestinationInfo;
import com.intellij.compiler.impl.packagingCompiler.ArchivePackageInfo;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.packaging.elements.ArchivePackageWriter;
import com.intellij.packaging.elements.IncrementalCompilerInstructionCreator;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class PackIntoArchiveInstructionCreator extends IncrementalCompilerInstructionCreatorBase {
  private final DestinationInfo myDestinationInfo;
  private final ArchivePackageInfo myArchivePackageInfo;
  private final String myPathInJar;

  public PackIntoArchiveInstructionCreator(ArtifactsProcessingItemsBuilderContext context, ArchivePackageInfo archivePackageInfo,
                                           String pathInJar, DestinationInfo destinationInfo) {
    super(context);
    myArchivePackageInfo = archivePackageInfo;
    myPathInJar = pathInJar;
    myDestinationInfo = destinationInfo;
  }

  @Override
  public void addFileCopyInstruction(@Nonnull VirtualFile file, @Nonnull String outputFileName) {
    final String pathInJar = childPathInJar(outputFileName);
    if (myContext.addDestination(file, new ArchiveDestinationInfo(pathInJar, myArchivePackageInfo, myDestinationInfo))) {
      myArchivePackageInfo.addContent(pathInJar, file);
    }
  }

  private String childPathInJar(String fileName) {
    return myPathInJar.length() == 0 ? fileName : myPathInJar + "/" + fileName;
  }

  @Override
  public PackIntoArchiveInstructionCreator subFolder(@Nonnull String directoryName) {
    return new PackIntoArchiveInstructionCreator(myContext, myArchivePackageInfo, childPathInJar(directoryName), myDestinationInfo);
  }

  @Nonnull
  @Override
  public IncrementalCompilerInstructionCreator archive(@Nonnull String archiveFileName, @Nonnull ArchivePackageWriter<?> packageWriter) {
    final ArchivePackageInfo archivePackageInfo = new ArchivePackageInfo(packageWriter);
    final String outputPath = myDestinationInfo.getOutputPath() + "/" + archiveFileName;
    if (!myContext.registerJarFile(archivePackageInfo, outputPath)) {
      return new SkipAllInstructionCreator(myContext);
    }
    final ArchiveDestinationInfo destination = new ArchiveDestinationInfo(childPathInJar(archiveFileName), myArchivePackageInfo, myDestinationInfo);
    archivePackageInfo.addDestination(destination);
    return new PackIntoArchiveInstructionCreator(myContext, archivePackageInfo, "", destination);
  }
}
