// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.pointers;

import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class VirtualFilePointerManager extends SimpleModificationTracker {
  public static VirtualFilePointerManager getInstance() {
    return ApplicationManager.getApplication().getComponent(VirtualFilePointerManager.class);
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
