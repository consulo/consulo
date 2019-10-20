// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

/**
 * @author max
 */
public abstract class NewVirtualFileSystem implements FileSystemInterface, CachingVirtualFileSystem, VirtualFileSystem {
  private final Map<VirtualFileListener, VirtualFileListener> myListenerWrappers = ContainerUtil.newConcurrentMap();

  @Nullable
  public abstract VirtualFile findFileByPathIfCached(@Nonnull String path);

  protected String normalize(@Nonnull String path) {
    return path;
  }

  @Override
  public void refreshWithoutFileWatcher(final boolean asynchronous) {
    refresh(asynchronous);
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public boolean isSymLink(@Nonnull final VirtualFile file) {
    return false;
  }

  @Override
  public String resolveSymLink(@Nonnull VirtualFile file) {
    return null;
  }

  @Nonnull
  protected abstract String extractRootPath(@Nonnull String path);

  @Override
  public void addVirtualFileListener(@Nonnull final VirtualFileListener listener) {
    VirtualFileListener wrapper = new VirtualFileFilteringListener(listener, this);
    VirtualFileManager.getInstance().addVirtualFileListener(wrapper);
    myListenerWrappers.put(listener, wrapper);
  }

  @Override
  public void removeVirtualFileListener(@Nonnull final VirtualFileListener listener) {
    VirtualFileListener wrapper = myListenerWrappers.remove(listener);
    if (wrapper != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(wrapper);
    }
  }

  public abstract int getRank();

  @Nonnull
  @Override
  public abstract VirtualFile copyFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent, @Nonnull String copyName) throws IOException;

  @Override
  @Nonnull
  public abstract VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException;

  @Nonnull
  @Override
  public abstract VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException;

  @Override
  public abstract void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException;

  @Override
  public abstract void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException;

  @Override
  public abstract void renameFile(final Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException;

  public boolean markNewFilesAsDirty() {
    return false;
  }

  @Nonnull
  public String getCanonicallyCasedName(@Nonnull VirtualFile file) {
    return file.getName();
  }

  /**
   * Reads various file attributes in one shot (to reduce the number of native I/O calls).
   *
   * @param file file to get attributes of.
   * @return attributes of a given file, or {@code null} if the file doesn't exist.
   */
  @Nullable
  public abstract FileAttributes getAttributes(@Nonnull VirtualFile file);

  /**
   * Returns {@code true} if {@code path} represents a directory with at least one child.
   * Override if your file system can answer this question more efficiently (e.g. without enumerating all children).
   */
  public boolean hasChildren(@Nonnull VirtualFile file) {
    return list(file).length != 0;
  }
}