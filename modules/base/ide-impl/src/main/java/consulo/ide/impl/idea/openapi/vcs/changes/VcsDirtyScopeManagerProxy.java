/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * to do not allow user to perform unknown activity under lock, pass mock manager
 * use it later to call real manager under lock
 */
class VcsDirtyScopeManagerProxy extends VcsDirtyScopeManager {
  private boolean myEverythingDirty;
  // no grouping etc., let real manager do stuff
  private final Set<VirtualFile> myVFiles;
  private final Set<VirtualFile> myVDirs;
  private final Set<FilePath> myFiles;
  private final Set<FilePath> myDirs;

  VcsDirtyScopeManagerProxy() {
    myEverythingDirty = false;

    myVFiles = new HashSet<>();
    myVDirs = new HashSet<>();
    myFiles = new HashSet<>();
    myDirs = new HashSet<>();
  }

  @Override
  public void markEverythingDirty() {
    myEverythingDirty = true;

    myVDirs.clear();
    myVFiles.clear();
    myDirs.clear();
    myFiles.clear();
  }

  @Override
  public void fileDirty(@Nonnull final VirtualFile file) {
    myVFiles.add(file);
  }

  @Override
  public void fileDirty(@Nonnull final FilePath file) {
    myFiles.add(file);
  }

  @Override
  public void dirDirtyRecursively(final VirtualFile dir) {
    myVDirs.add(dir);
  }

  @Override
  public void dirDirtyRecursively(final FilePath path) {
    myDirs.add(path);
  }

  @Nonnull
  @Override
  public Collection<FilePath> whatFilesDirty(@Nonnull Collection<? extends FilePath> files) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void filePathsDirty(@Nullable final Collection<? extends FilePath> filesDirty, @Nullable final Collection<? extends FilePath> dirsRecursivelyDirty) {
    if (filesDirty != null) {
      myFiles.addAll(filesDirty);
    }
    if (dirsRecursivelyDirty != null) {
      myDirs.addAll(dirsRecursivelyDirty);
    }
  }

  @Override
  public void filesDirty(@Nullable final Collection<? extends VirtualFile> filesDirty, @Nullable final Collection<? extends VirtualFile> dirsRecursivelyDirty) {
    if (filesDirty != null) {
      myVFiles.addAll(filesDirty);
    }
    if (dirsRecursivelyDirty != null) {
      myVDirs.addAll(dirsRecursivelyDirty);
    }
  }

  public void callRealManager(final VcsDirtyScopeManager manager) {
    if (myEverythingDirty) {
      manager.markEverythingDirty();
      return;
    }

    for (FilePath file : myFiles) {
      manager.fileDirty(file);
    }
    for (VirtualFile file : myVFiles) {
      manager.fileDirty(file);
    }
    for (FilePath dir : myDirs) {
      manager.dirDirtyRecursively(dir);
    }
    for (VirtualFile dir : myVDirs) {
      manager.dirDirtyRecursively(dir);
    }
  }
}
