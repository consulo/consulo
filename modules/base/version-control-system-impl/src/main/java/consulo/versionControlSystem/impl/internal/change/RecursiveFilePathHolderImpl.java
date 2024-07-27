// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.FileHolder;
import consulo.versionControlSystem.change.IgnoredFilesHolder;
import consulo.versionControlSystem.change.VcsDirtyScope;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.*;

public class RecursiveFilePathHolderImpl implements IgnoredFilesHolder {

  private final Project myProject;
  private final HolderType myHolderType;
  private final Set<FilePath> myMap;

  public RecursiveFilePathHolderImpl(final Project project, final FileHolder.HolderType holderType) {
    myProject = project;
    myHolderType = holderType;
    myMap = new HashSet<>();
  }

  @Override
  public void cleanAll() {
    myMap.clear();
  }

  @Override
  public FileHolder.HolderType getType() {
    return myHolderType;
  }

  @Override
  public void addFile(@Nonnull FilePath file) {
    if (!containsFile(file)) {
      myMap.add(file);
    }
  }

  @Override
  public boolean containsFile(VirtualFile file) {
    return containsFile(VcsUtil.getFilePath(file));
  }

  @Override
  public RecursiveFilePathHolderImpl copy() {
    final RecursiveFilePathHolderImpl copyHolder = new RecursiveFilePathHolderImpl(myProject, myHolderType);
    copyHolder.myMap.addAll(myMap);
    return copyHolder;
  }

  @Override
  public boolean containsFile(@Nonnull FilePath file, @Nonnull VirtualFile vcsRoot) {
    return containsFile(file);
  }

  private boolean containsFile(@Nonnull FilePath file) {
    if (myMap.isEmpty()) return false;
    FilePath parent = file;
    while (parent != null) {
      if (myMap.contains(parent)) return true;
      parent = parent.getParentPath();
    }
    return false;
  }

  @Override
  public Collection<VirtualFile> values() {
    return myMap.stream().map(FilePath::getVirtualFile).filter(Objects::nonNull).toList();
  }

  @Override
  public void cleanAndAdjustScope(final VcsModifiableDirtyScope scope) {
    if (myProject.isDisposed()) return;
    final Iterator<FilePath> iterator = myMap.iterator();
    while (iterator.hasNext()) {
      final FilePath file = iterator.next();
      if (isFileDirty(scope, file)) {
        iterator.remove();
      }
    }
  }

  private static boolean isFileDirty(@Nonnull VcsDirtyScope scope, @Nonnull FilePath filePath) {
    return scope.belongsTo(filePath);
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final RecursiveFilePathHolderImpl that = (RecursiveFilePathHolderImpl)o;
    return myMap.equals(that.myMap);
  }

  public int hashCode() {
    return myMap.hashCode();
  }
}
