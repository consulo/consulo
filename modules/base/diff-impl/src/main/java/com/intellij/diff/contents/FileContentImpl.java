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
package com.intellij.diff.contents;

import com.intellij.diff.util.DiffUtil;
import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows to compare files
 */
public class FileContentImpl extends DiffContentBase implements FileContent {
  @Nonnull
  private final VirtualFile myFile;
  @javax.annotation.Nullable
  private final Project myProject;
  @Nonnull
  private final FileType myType;
  @Nullable private final VirtualFile myHighlightFile;

  public FileContentImpl(@javax.annotation.Nullable Project project, @Nonnull VirtualFile file) {
    this(project, file, getHighlightFile(file));
  }

  public FileContentImpl(@javax.annotation.Nullable Project project,
                         @Nonnull VirtualFile file,
                         @javax.annotation.Nullable VirtualFile highlightFile) {
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
    return new OpenFileDescriptor(myProject, myHighlightFile);
  }

  @javax.annotation.Nullable
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
