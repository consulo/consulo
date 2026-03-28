// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.pointer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.component.internal.RootComponentHolder;
import consulo.disposer.Disposable;

import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.APPLICATION)
public interface VirtualFilePointerManager extends ModificationTracker {
  public static VirtualFilePointerManager getInstance() {
    return RootComponentHolder.get().getComponent(VirtualFilePointerManager.class);
  }

  public abstract VirtualFilePointer create(String url, Disposable parent, @Nullable VirtualFilePointerListener listener);

  public abstract VirtualFilePointer create(VirtualFile file, Disposable parent, @Nullable VirtualFilePointerListener listener);

  public abstract VirtualFilePointer duplicate(VirtualFilePointer pointer, Disposable parent, @Nullable VirtualFilePointerListener listener);

  public abstract VirtualFilePointerContainer createContainer(Disposable parent);

  public abstract VirtualFilePointerContainer createContainer(Disposable parent, @Nullable VirtualFilePointerListener listener);

  public abstract VirtualFilePointer createDirectoryPointer(String url, boolean recursively, Disposable parent, VirtualFilePointerListener listener);
}
