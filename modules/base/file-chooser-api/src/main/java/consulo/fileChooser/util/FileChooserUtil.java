/*
 * Copyright 2013-2022 consulo.io
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
package consulo.fileChooser.util;

import consulo.platform.Platform;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20/01/2022
 */
public class FileChooserUtil {
  public static boolean isFileHidden(@Nullable VirtualFile file) {
    return file != null && file.isValid() && file.isInLocalFileSystem() && (file.is(VFileProperty.HIDDEN) || Platform.current().os().isUnix() && file.getName().startsWith("."));
  }

  public static boolean isArchive(@Nullable VirtualFile file) {
    if (file == null) return false;
    if (isArchiveFileSystem(file) && file.getParent() == null) return true;
    return !file.isDirectory() && file.getFileType() instanceof ArchiveFileType && !isArchiveFileSystem(file.getParent());
  }

  private static boolean isArchiveFileSystem(VirtualFile file) {
    return file.getFileSystem() instanceof ArchiveFileSystem;
  }
}
