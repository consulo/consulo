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
package com.intellij.packaging.elements;

import com.intellij.openapi.vfs.VirtualFile;
import consulo.packaging.elements.ArchivePackageWriter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public interface IncrementalCompilerInstructionCreator {

  void addFileCopyInstruction(@Nonnull VirtualFile file, @Nonnull String outputFileName);

  void addDirectoryCopyInstructions(@Nonnull VirtualFile directory);

  void addDirectoryCopyInstructions(@Nonnull VirtualFile directory, @Nullable PackagingFileFilter filter);

  IncrementalCompilerInstructionCreator subFolder(@Nonnull String directoryName);

  @Nonnull
  IncrementalCompilerInstructionCreator archive(@Nonnull String archiveFileName, @Nonnull ArchivePackageWriter<?> packageWriter);

  IncrementalCompilerInstructionCreator subFolderByRelativePath(@Nonnull String relativeDirectoryPath);
}
