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
package com.intellij.openapi.vfs.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerContainer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
public class CoreVirtualFilePointerManager extends VirtualFilePointerManager {
  @Nonnull
  @Override
  public VirtualFilePointer create(@Nonnull String url, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener) {
    VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl(url);
    return new IdentityVirtualFilePointer(vFile, url);
  }

  @Nonnull
  @Override
  public VirtualFilePointer create(@Nonnull VirtualFile file, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return new IdentityVirtualFilePointer(file, file.getUrl());
  }

  @Nonnull
  @Override
  public VirtualFilePointer duplicate(@Nonnull VirtualFilePointer pointer,
                                      @Nonnull Disposable parent,
                                      @Nullable VirtualFilePointerListener listener) {
    return new IdentityVirtualFilePointer(pointer.getFile(), pointer.getUrl());
  }

  @Nonnull
  @Override
  public VirtualFilePointerContainer createContainer(@Nonnull Disposable parent) {
    return createContainer(parent, null);
  }

  @Nonnull
  @Override
  public VirtualFilePointerContainer createContainer(@Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return new VirtualFilePointerContainerImpl(this, parent, listener);
  }

  @Override
  public void dispose() {
  }

  @Override
  public long getModificationCount() {
    return 0;
  }
}
