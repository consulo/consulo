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

import com.intellij.openapi.vfs.VirtualFile;
import consulo.packaging.elements.ArchivePackageWriter;
import com.intellij.packaging.elements.IncrementalCompilerInstructionCreator;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class SkipAllInstructionCreator extends IncrementalCompilerInstructionCreatorBase {
  public SkipAllInstructionCreator(ArtifactsProcessingItemsBuilderContext context) {
    super(context);
  }

  @Override
  public void addFileCopyInstruction(@Nonnull VirtualFile file, @Nonnull String outputFileName) {
  }

  @Override
  public SkipAllInstructionCreator subFolder(@Nonnull String directoryName) {
    return this;
  }

  @Nonnull
  @Override
  public IncrementalCompilerInstructionCreator archive(@Nonnull String archiveFileName, @Nonnull ArchivePackageWriter<?> packageWriter) {
    return this;
  }
}
