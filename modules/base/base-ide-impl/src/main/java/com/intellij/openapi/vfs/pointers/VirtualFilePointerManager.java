// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.pointers;

import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.component.extension.internal.RootComponentHolder;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface VirtualFilePointerManager extends ModificationTracker {
  public static VirtualFilePointerManager getInstance() {
    return RootComponentHolder.getRootComponent().getComponent(VirtualFilePointerManager.class);
  }

  @Nonnull
  public abstract VirtualFilePointer create(@Nonnull String url, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointer create(@Nonnull VirtualFile file, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointer duplicate(@Nonnull VirtualFilePointer pointer, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointerContainer createContainer(@Nonnull Disposable parent);

  @Nonnull
  public abstract VirtualFilePointerContainer createContainer(@Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointer createDirectoryPointer(@Nonnull String url, boolean recursively, @Nonnull Disposable parent, @Nonnull VirtualFilePointerListener listener);
}
