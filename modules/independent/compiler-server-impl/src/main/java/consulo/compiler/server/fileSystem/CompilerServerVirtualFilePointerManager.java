/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.fileSystem;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.VirtualFilePointerContainerImpl;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerContainer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 19:54/19.08.13
 */
public class CompilerServerVirtualFilePointerManager extends VirtualFilePointerManager {
  @Nonnull
  @Override
  public VirtualFilePointer create(@Nonnull String url, @Nonnull Disposable parent, @javax.annotation.Nullable VirtualFilePointerListener listener) {
    return new CompilerServerVirtualFilePointer(url);
  }

  @Nonnull
  @Override
  public VirtualFilePointer create(@Nonnull VirtualFile file, @Nonnull Disposable parent, @javax.annotation.Nullable VirtualFilePointerListener listener) {
    return new CompilerServerVirtualFilePointer(file.getUrl());
  }

  @Nonnull
  @Override
  public VirtualFilePointer duplicate(@Nonnull VirtualFilePointer pointer,
                                      @Nonnull Disposable parent,
                                      @javax.annotation.Nullable VirtualFilePointerListener listener) {
    return new CompilerServerVirtualFilePointer(pointer.getUrl());
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
