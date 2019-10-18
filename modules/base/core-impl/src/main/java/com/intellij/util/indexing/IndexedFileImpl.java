// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

public class IndexedFileImpl extends UserDataHolderBase implements IndexedFile {
  protected final VirtualFile myFile;
  protected final String myFileName;
  protected final FileType myFileType;

  public IndexedFileImpl(@Nonnull VirtualFile file, @Nonnull FileType type) {
    myFile = file;
    myFileName = file.getName();
    myFileType = type;
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return SubstitutedFileType.substituteFileType(myFile, myFileType, getProject());
  }

  @Nonnull
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public String getFileName() {
    return myFileName;
  }

  @Override
  public Project getProject() {
    return getUserData(IndexingDataKeys.PROJECT);
  }
}
