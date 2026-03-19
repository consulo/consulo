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
package consulo.diff.impl.internal.content;

import consulo.application.SaveAndSyncHandler;
import consulo.diff.content.DiffContentBase;
import consulo.diff.content.FileContent;
import consulo.diff.internal.DiffImplUtil;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

/**
 * Allows to compare files
 */
public class FileContentImpl extends DiffContentBase implements FileContent {
  
  private final VirtualFile myFile;
  private final @Nullable Project myProject;
  
  private final FileType myType;
  private final @Nullable VirtualFile myHighlightFile;

  public FileContentImpl(@Nullable Project project, VirtualFile file) {
    this(project, file, getHighlightFile(file));
  }

  public FileContentImpl(@Nullable Project project,
                         VirtualFile file,
                         @Nullable VirtualFile highlightFile) {
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
    return OpenFileDescriptorFactory.getInstance(myProject).newBuilder(myHighlightFile).build();
  }

  private static @Nullable VirtualFile getHighlightFile(VirtualFile file) {
    if (file.isInLocalFileSystem()) return file;
    return null;
  }

  
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  
  @Override
  public FileType getContentType() {
    return myType;
  }

  
  public String getFilePath() {
    return myFile.getPath();
  }

  @Override
  public void onAssigned(boolean isAssigned) {
    if (isAssigned && SaveAndSyncHandler.getInstance().isSyncOnFrameActivation()) DiffImplUtil.markDirtyAndRefresh(true, false, false, myFile);
  }
}
