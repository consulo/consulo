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
package consulo.ide.impl.idea.diff.contents;

import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.diff.content.DiffContentBase;
import consulo.diff.content.FileContent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.navigation.Navigatable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows to compare files
 */
public class FileContentImpl extends DiffContentBase implements FileContent {
  @Nonnull
  private final VirtualFile myFile;
  @jakarta.annotation.Nullable
  private final Project myProject;
  @Nonnull
  private final FileType myType;
  @Nullable private final VirtualFile myHighlightFile;

  public FileContentImpl(@jakarta.annotation.Nullable Project project, @Nonnull VirtualFile file) {
    this(project, file, getHighlightFile(file));
  }

  public FileContentImpl(@jakarta.annotation.Nullable Project project,
                         @Nonnull VirtualFile file,
                         @jakarta.annotation.Nullable VirtualFile highlightFile) {
    assert file.isValid() && !file.isDirectory();
    myFile = file;
    myProject = project;
    myType = file.getFileType();
    myHighlightFile = highlightFile;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable() {
    if (myProject == null || myProject.isDefault()) return null;
    if (myHighlightFile == null || !myHighlightFile.isValid()) return null;
    return new OpenFileDescriptorImpl(myProject, myHighlightFile);
  }

  @jakarta.annotation.Nullable
  private static VirtualFile getHighlightFile(@Nonnull VirtualFile file) {
    if (file.isInLocalFileSystem()) return file;
    return null;
  }

  @Nonnull
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public FileType getContentType() {
    return myType;
  }

  @Nonnull
  public String getFilePath() {
    return myFile.getPath();
  }

  @Override
  public void onAssigned(boolean isAssigned) {
    if (isAssigned && GeneralSettings.getInstance().isSyncOnFrameActivation()) DiffUtil.markDirtyAndRefresh(true, false, false, myFile);
  }
}
