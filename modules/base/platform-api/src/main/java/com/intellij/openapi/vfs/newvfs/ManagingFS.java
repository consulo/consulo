/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * @author max
 */
public abstract class ManagingFS implements FileSystemInterface {
  private static class ManagingFSHolder {
    private static final ManagingFS ourInstance = ApplicationManager.getApplication().getComponent(ManagingFS.class);
  }

  public static ManagingFS getInstance() {
    return ManagingFSHolder.ourInstance;
  }

  @Nullable
  public abstract DataInputStream readAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att);

  @Nonnull
  public abstract DataOutputStream writeAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att);

  public abstract int getModificationCount(@Nonnull VirtualFile fileOrDirectory);

  /**
   * @deprecated to be removed in IDEA 16
   * @see #getModificationCount()
   */
  public int getCheapFileSystemModificationCount() {
    return getModificationCount();
  }

  /**
   * @return a number that's incremented every time something changes in the VFS, i.e. file hierarchy, names, flags, attributes, contents.
   * This only counts modifications done in current IDE session.
   * @see #getStructureModificationCount()
   * @see #getFilesystemModificationCount()
   */
  public abstract int getModificationCount();

  /**
   * @return a number that's incremented every time something changes in the VFS structure, i.e. file hierarchy or names.
   * This only counts modifications done in current IDE session.
   * @see #getModificationCount()
   */
  public abstract int getStructureModificationCount();

  /**
   * @deprecated to be removed in IDEA 16
   * @return a number that's incremented every time something changes in the VFS, i.e. file hierarchy, names, flags, attributes, contents.
   * This number is persisted between IDE sessions and so it'll always increase. This method invocation means disk access, so it's not terribly cheap.
   */
  public abstract int getFilesystemModificationCount();

  public abstract long getCreationTimestamp();

  public abstract boolean areChildrenLoaded(@Nonnull VirtualFile dir);

  public abstract boolean wereChildrenAccessed(@Nonnull VirtualFile dir);

  @Nullable
  public abstract NewVirtualFile findRoot(@Nonnull String basePath, @Nonnull NewVirtualFileSystem fs);

  @Nonnull
  public abstract VirtualFile[] getRoots();

  @Nonnull
  public abstract VirtualFile[] getRoots(@Nonnull NewVirtualFileSystem fs);

  @Nonnull
  public abstract VirtualFile[] getLocalRoots();

  @Nullable
  public abstract VirtualFile findFileById(int id);
}
