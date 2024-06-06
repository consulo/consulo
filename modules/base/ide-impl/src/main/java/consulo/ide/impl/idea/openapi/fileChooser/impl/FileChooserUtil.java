/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileChooser.impl;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.PathChooserDialog;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public final class FileChooserUtil {
  private static final String LAST_OPENED_FILE_PATH = "last_opened_file_path";

  @Nullable
  public static VirtualFile getLastOpenedFile(@Nullable final Project project) {
    if (project != null) {
      final String path = PropertiesComponent.getInstance(project).getValue(LAST_OPENED_FILE_PATH);
      if (path != null) {
        return LocalFileSystem.getInstance().findFileByPath(path);
      }
    }
    return null;
  }

  public static void setLastOpenedFile(@Nullable final Project project, @Nullable final VirtualFile file) {
    if (project != null && !project.isDisposed() && file != null) {
      PropertiesComponent.getInstance(project).setValue(LAST_OPENED_FILE_PATH, file.getPath());
    }
  }

  @Nullable
  public static VirtualFile getFileToSelect(@Nonnull FileChooserDescriptor descriptor, @Nullable Project project,
                                            @Nullable VirtualFile toSelect, @Nullable VirtualFile lastPath) {
    boolean chooseDir = descriptor instanceof FileSaverDescriptor;
    VirtualFile result;

    if (toSelect == null && lastPath == null) {
      result = project == null? null : project.getBaseDir();
    }
    else if (toSelect != null && lastPath != null) {
      if (Boolean.TRUE.equals(descriptor.getUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT))) {
        result = lastPath;
      }
      else {
        result = toSelect;
      }
    }
    else if (toSelect == null) {
      result = lastPath;
    }
    else {
      result = toSelect;
    }

    if (result != null) {
      if (chooseDir && !result.isDirectory()) {
        result = result.getParent();
      }
    }
    else if (Platform.current().os().isUnix()) {
      result = VfsUtil.getUserHomeDir();
    }

    return result;
  }

  @Nonnull
  public static List<VirtualFile> getChosenFiles(@Nonnull final FileChooserDescriptor descriptor,
                                                 @Nonnull final Collection<VirtualFile> selectedFiles) {
    return ContainerUtil.mapNotNull(selectedFiles, file -> file != null && file.isValid() ? descriptor.getFileToSelect(file) : null);
  }
}
