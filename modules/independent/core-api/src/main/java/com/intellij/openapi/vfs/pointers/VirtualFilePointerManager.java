/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.pointers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class VirtualFilePointerManager extends SimpleModificationTracker implements Disposable {
  @Nonnull
  public static VirtualFilePointerManager getInstance() {
    return ServiceManager.getService(VirtualFilePointerManager.class);
  }

  @Nonnull
  public abstract VirtualFilePointer create(@Nonnull String url, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointer create(@Nonnull VirtualFile file, @Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointer duplicate(@Nonnull VirtualFilePointer pointer, @Nonnull Disposable parent,
                                               @Nullable VirtualFilePointerListener listener);

  @Nonnull
  public abstract VirtualFilePointerContainer createContainer(@Nonnull Disposable parent);

  @Nonnull
  public abstract VirtualFilePointerContainer createContainer(@Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);
}
