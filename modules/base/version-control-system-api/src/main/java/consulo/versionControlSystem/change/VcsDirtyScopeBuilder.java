// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public interface VcsDirtyScopeBuilder {
  boolean belongsTo(@Nonnull FilePath path);

  void addDirtyPathFast(@Nonnull VirtualFile vcsRoot, @Nonnull FilePath filePath, boolean recursively);

  void markEverythingDirty();

  @Nonnull
  VcsModifiableDirtyScope pack();
}
