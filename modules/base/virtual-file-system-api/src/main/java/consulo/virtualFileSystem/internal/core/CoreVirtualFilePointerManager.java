/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.virtualFileSystem.internal.core;

import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.VirtualFilePointerContainerImpl;
import consulo.virtualFileSystem.pointer.*;

import org.jspecify.annotations.Nullable;

/**
 * @author yole
 */
public class CoreVirtualFilePointerManager extends SimpleModificationTracker implements VirtualFilePointerManager, Disposable {
  @Override
  public VirtualFilePointer create(String url, Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return new LightFilePointer(url);
  }

  @Override
  public VirtualFilePointer create(VirtualFile file, Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return new LightFilePointer(file);
  }

  @Override
  public VirtualFilePointer duplicate(VirtualFilePointer pointer, Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return new LightFilePointer(pointer.getUrl());
  }

  @Override
  public VirtualFilePointerContainer createContainer(Disposable parent) {
    return createContainer(parent, null);
  }

  @Override
  public VirtualFilePointerContainer createContainer(Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return new VirtualFilePointerContainerImpl(this, parent, listener);
  }

  @Override
  public VirtualFilePointer createDirectoryPointer(String url, boolean recursively, Disposable parent, @Nullable VirtualFilePointerListener listener) {
    return create(url, parent, listener);
  }

  @Override
  public void dispose() {
  }
}
