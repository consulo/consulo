/*
 * Copyright 2013 Consulo.org
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
package com.intellij.openapi.vfs.util;

import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:39/13.07.13
 */
public class ArchiveVfsUtil {
  /**
   * TODO[VISTALL] rename!
   * @param virtualFile
   * @return
   */
  @Nullable
  public static VirtualFile getJarRootForLocalFile(@Nullable VirtualFile virtualFile) {
    if(virtualFile == null || !virtualFile.isValid()) {
      return null;
    }
    final FileType fileType = virtualFile.getFileType();
    if(fileType instanceof ArchiveFileType) {
      return ((ArchiveFileType)fileType).getFileSystem().findLocalVirtualFileByPath(virtualFile.getPath());
    }
    return null;
  }

  /**
   * TODO[VISTALL] rename!
   * @param virtualFile
   * @return
   */
  @Nullable
  public static VirtualFile getVirtualFileForJar(@Nullable VirtualFile virtualFile) {
    if(virtualFile == null || !virtualFile.isValid()) {
      return null;
    }

    if(virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
      return ((ArchiveFileSystem)virtualFile.getFileSystem()).getVirtualFileForJar(virtualFile);
    }
    return null;
  }
}
