// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;

public interface FilePathHolder extends FileHolder {
  void addFile(@Nonnull FilePath file);

  boolean containsFile(@Nonnull FilePath file, @Nonnull VirtualFile vcsRoot);

  @Nonnull
  Collection<FilePath> values();
}
