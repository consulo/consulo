/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.virtualFileSystem.util;

import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileProvider;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class VirtualFilePathUtil {
  private VirtualFilePathUtil() {
  }

  @Nullable
  public static String getLocalPath(@Nullable VirtualFile file) {
    if (file == null || !file.isValid()) {
      return null;
    }
    if (file.getFileSystem().getProtocol().equals(ArchiveFileSystem.ARCHIVE_SEPARATOR) && file.getParent() != null) {
      return null;
    }
    return getLocalPath(file.getPath());
  }

  @Nonnull
  public static String getLocalPath(@Nonnull String path) {
    return FileUtil.toSystemDependentName(StringUtil.trimEnd(path, ArchiveFileSystem.ARCHIVE_SEPARATOR));
  }

  @Nonnull
  public static VirtualFile getLocalFile(@Nonnull VirtualFile file) {
    if (!file.isValid()) {
      return file;
    }
    if (file.getFileSystem() instanceof LocalFileProvider) {
      final VirtualFile localFile = ((LocalFileProvider)file.getFileSystem()).getLocalVirtualFileFor(file);
      if (localFile != null) {
        return localFile;
      }
    }
    return file;
  }

  @Nonnull
  public static String toPresentableUrl(@Nonnull String url) {
    return getLocalPath(VirtualFileManager.extractPath(url));
  }
}
