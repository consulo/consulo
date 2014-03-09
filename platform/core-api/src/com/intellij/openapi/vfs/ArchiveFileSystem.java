/*
 * Copyright 2013 must-be.org
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
package com.intellij.openapi.vfs;

import com.intellij.openapi.vfs.newvfs.CachingVirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.FileSystemInterface;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 18:59/13.07.13
 */
public interface ArchiveFileSystem
  extends FileSystemInterface, CachingVirtualFileSystem, ArchiveCopyingFileSystem, LocalFileProvider, IVirtualFileSystem {
  @NonNls String ARCHIVE_SEPARATOR = "!/";

  @Nullable
  VirtualFile getVirtualFileForArchive(@Nullable VirtualFile entryVFile);

  /**
   * FIXME [VISTALL] it looks like findLocalVirtualFileByPath
   * @param entryVFile
   * @return
   */
  @Nullable
  @Deprecated
  VirtualFile findByPathWithSeparator(@Nullable VirtualFile entryVFile);

  @Nullable
  ArchiveFile getArchiveWrapperFile(@NotNull VirtualFile entryVFile) throws IOException;
}
