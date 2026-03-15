// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi.stub;

import consulo.language.internal.SubstitutedFileType;
import consulo.language.psi.stub.IndexedFile;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import org.jspecify.annotations.Nullable;

public class IndexedFileImpl extends UserDataHolderBase implements IndexedFile {
  protected final VirtualFile myFile;
  protected final String myFileName;
  protected final FileType myFileType;

  protected Project myProject;

  public IndexedFileImpl(VirtualFile file, FileType type) {
    myFile = file;
    myFileName = file.getName();
    myFileType = type;
  }

  
  @Override
  public FileType getFileType() {
    return SubstitutedFileType.substituteFileType(myFile, myFileType, getProject());
  }

  
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  
  @Override
  public String getFileName() {
    return myFileName;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  public void setProject(@Nullable Project project) {
    myProject = project;
  }
}
