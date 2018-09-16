/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author max
 */
public abstract class NewVirtualFileSystem implements FileSystemInterface, CachingVirtualFileSystem, VirtualFileSystem {
  private final Map<VirtualFileListener, VirtualFileListener> myListenerWrappers = new HashMap<VirtualFileListener, VirtualFileListener>();

  @Nullable
  public abstract VirtualFile findFileByPathIfCached(@Nonnull @NonNls final String path);

  @Nullable
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
    synchronized (myListenerWrappers) {
      VirtualFileListener wrapper = new VirtualFileFilteringListener(listener, this);
      VirtualFileManager.getInstance().addVirtualFileListener(wrapper);
      myListenerWrappers.put(listener, wrapper);
    }
  }

  @Override
  public void removeVirtualFileListener(@Nonnull final VirtualFileListener listener) {
    synchronized (myListenerWrappers) {
      final VirtualFileListener wrapper = myListenerWrappers.remove(listener);
      if (wrapper != null) {
        VirtualFileManager.getInstance().removeVirtualFileListener(wrapper);
      }
    }
  }

  public abstract int getRank();

  @Override
  public abstract VirtualFile copyFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent, @Nonnull String copyName) throws IOException;

  @Override
  @Nonnull
  public abstract VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException;

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

  public String getCanonicallyCasedName(@Nonnull VirtualFile file) {
    return file.getName();
  }

  /**
   * Reads various file attributes in one shot (to reduce the number of native I/O calls).
   *
   * @param file file to get attributes of.
   * @return attributes of a given file, or <code>null</code> if the file doesn't exist.
   * @since 11.1
   */
  @Nullable
  public abstract FileAttributes getAttributes(@Nonnull VirtualFile file);
}
