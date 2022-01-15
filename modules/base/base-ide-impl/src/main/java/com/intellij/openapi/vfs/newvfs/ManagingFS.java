// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.newvfs;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.function.Function;

/**
 * @author max
 */
public abstract class ManagingFS implements FileSystemInterface {
  private static class ManagingFSHolder {
    private static final ManagingFS ourInstance = ServiceManager.getService(ManagingFS.class);
  }

  public static ManagingFS getInstance() {
    return ManagingFSHolder.ourInstance;
  }

  @Nullable
  public abstract DataInputStream readAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att);

  @Nonnull
  public abstract DataOutputStream writeAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att);

  /**
   * @return a number that's incremented every time something changes for the file: name, size, flags, content.
   * This number is persisted between IDE sessions and so it'll always increase. This method invocation means disk access, so it's not terribly cheap.
   */
  public abstract int getModificationCount(@Nonnull VirtualFile fileOrDirectory);

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
   * @return a number that's incremented every time modification count for some file is advanced, @see {@link #getModificationCount(VirtualFile)}.
   * This number is persisted between IDE sessions and so it'll always increase. This method invocation means disk access, so it's not terribly cheap.
   */
  @TestOnly
  public abstract int getFilesystemModificationCount();

  public abstract long getCreationTimestamp();

  public abstract boolean areChildrenLoaded(@Nonnull VirtualFile dir);

  public abstract boolean wereChildrenAccessed(@Nonnull VirtualFile dir);

  @Nullable
  public abstract NewVirtualFile findRoot(@Nonnull String path, @Nonnull NewVirtualFileSystem fs);

  @Nonnull
  public abstract VirtualFile[] getRoots();

  @Nonnull
  public abstract VirtualFile[] getRoots(@Nonnull NewVirtualFileSystem fs);

  @Nonnull
  public abstract VirtualFile[] getLocalRoots();

  @Nullable
  public abstract VirtualFile findFileById(int id);

  @Nonnull
  protected abstract <P, R> Function<P, R> accessDiskWithCheckCanceled(Function<? super P, ? extends R> function);
}
