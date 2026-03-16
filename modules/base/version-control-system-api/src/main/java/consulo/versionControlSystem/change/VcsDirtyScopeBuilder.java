// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;

public interface VcsDirtyScopeBuilder {
  boolean belongsTo(FilePath path);

  void addDirtyPathFast(VirtualFile vcsRoot, FilePath filePath, boolean recursively);

  void markEverythingDirty();

  
  VcsModifiableDirtyScope pack();
}
