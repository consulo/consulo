/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.project;

import consulo.platform.Platform;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author dmitrylomov
 */
public class ProjectCoreUtil {
  public static boolean isProjectOrWorkspaceFile(@Nonnull VirtualFile file) {
    return isProjectOrWorkspaceFile(file, file.getFileType());
  }

  public static boolean isProjectOrWorkspaceFile(@Nonnull VirtualFile file, @Nullable FileType fileType) {
    VirtualFile parent = file.isDirectory() ? file : file.getParent();
    while (parent != null) {
      if (StringUtil.equal(parent.getNameSequence(), Project.DIRECTORY_STORE_FOLDER, Platform.current().fs().isCaseSensitive())) return true;
      parent = parent.getParent();
    }
    return false;
  }

  public static boolean isDirectoryBased(@Nonnull Project project) {
    if (project.isDefault()) {
      return false;
    }
    return true;
  }

  @Nullable
  public static VirtualFile getDirectoryStoreFile(Project project) {
    VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) {
      return null;
    }
    return baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
  }
}