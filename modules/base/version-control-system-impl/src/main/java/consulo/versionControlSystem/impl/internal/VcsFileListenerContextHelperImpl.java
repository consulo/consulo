// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.SystemInfo;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.impl.internal.util.RecursiveFilePathSet;
import consulo.versionControlSystem.internal.VcsFileListenerContextHelper;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Singleton
@ServiceImpl
public class VcsFileListenerContextHelperImpl implements VcsFileListenerContextHelper {
  private final Object LOCK = new Object();
  private final Set<FilePath> myIgnoredDeleted = new HashSet<>();
  private final Set<FilePath> myIgnoredAdded = new HashSet<>();
  private final RecursiveFilePathSet myIgnoredAddedRecursive = new RecursiveFilePathSet(SystemInfo.isFileSystemCaseSensitive);

  @Override
  public void ignoreDeleted(@Nonnull Collection<? extends FilePath> filePath) {
    synchronized (LOCK) {
      myIgnoredDeleted.addAll(filePath);
    }
  }

  @Override
  public boolean isDeletionIgnored(@Nonnull FilePath filePath) {
    synchronized (LOCK) {
      return myIgnoredDeleted.contains(filePath);
    }
  }

  @Override
  public void ignoreAdded(@Nonnull Collection<? extends FilePath> filePaths) {
    synchronized (LOCK) {
      myIgnoredAdded.addAll(filePaths);
    }
  }

  @Override
  public void ignoreAddedRecursive(@Nonnull Collection<? extends FilePath> filePaths) {
    synchronized (LOCK) {
      myIgnoredAddedRecursive.addAll(filePaths);
    }
  }

  @Override
  public boolean isAdditionIgnored(@Nonnull FilePath filePath) {
    synchronized (LOCK) {
      return myIgnoredAdded.contains(filePath) ||
        myIgnoredAddedRecursive.hasAncestor(filePath);
    }
  }

  @Override
  public void clearContext() {
    synchronized (LOCK) {
      myIgnoredAdded.clear();
      myIgnoredAddedRecursive.clear();
      myIgnoredDeleted.clear();
    }
  }

  @Override
  public boolean isAdditionContextEmpty() {
    synchronized (LOCK) {
      return myIgnoredAdded.isEmpty() && myIgnoredAddedRecursive.isEmpty();
    }
  }

  @Override
  public boolean isDeletionContextEmpty() {
    synchronized (LOCK) {
      return myIgnoredDeleted.isEmpty();
    }
  }
}
