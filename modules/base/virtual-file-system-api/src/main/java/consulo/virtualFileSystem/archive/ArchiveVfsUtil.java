/*
 * Copyright 2013-2016 consulo.io
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
package consulo.virtualFileSystem.archive;

import consulo.annotation.DeprecationInfo;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 19:39/13.07.13
 */
public class ArchiveVfsUtil {

  /**
   * Return mirror virtual file from archive file system if virtual file is archive
   */
  @Nullable
  public static VirtualFile getArchiveRootForLocalFile(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }
    final FileType fileType = virtualFile.getFileType();
    if (fileType instanceof ArchiveFileType) {
      return ((ArchiveFileType)fileType).getFileSystem().findLocalVirtualFileByPath(virtualFile.getPath());
    }
    return null;
  }

  /**
   * Return original file from local system, when file is mirror in archive file system
   */
  @Nullable
  public static VirtualFile getVirtualFileForArchive(@Nullable VirtualFile virtualFile) {
    if (virtualFile == null || !virtualFile.isValid()) {
      return null;
    }

    if (virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
      return ((ArchiveFileSystem)virtualFile.getFileSystem()).getLocalVirtualFileFor(virtualFile);
    }
    return null;
  }

  @Deprecated(forRemoval = true)
  @DeprecationInfo(value = "Use #getArchiveRootForLocalFile()")
  @Nullable
  public static VirtualFile getJarRootForLocalFile(@Nullable VirtualFile virtualFile) {
    return getArchiveRootForLocalFile(virtualFile);
  }

  @Deprecated(forRemoval = true)
  @DeprecationInfo(value = "Use #getVirtualFileForArchive()")
  @Nullable
  public static VirtualFile getVirtualFileForJar(@Nullable VirtualFile virtualFile) {
    return getVirtualFileForArchive(virtualFile);
  }
}
