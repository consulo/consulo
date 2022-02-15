// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;

public interface IndexedFile extends UserDataHolder {
  @Nonnull
  FileType getFileType();

  @Nonnull
  VirtualFile getFile();

  @Nonnull
  String getFileName();

  Project getProject();
}
