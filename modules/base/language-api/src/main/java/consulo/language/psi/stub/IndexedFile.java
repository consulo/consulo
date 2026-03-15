// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;

public interface IndexedFile extends UserDataHolder {
  
  FileType getFileType();

  
  VirtualFile getFile();

  
  String getFileName();

  Project getProject();
}
