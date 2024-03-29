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
package consulo.module.impl.internal.layer.orderEntry;

import consulo.content.OrderRootType;
import consulo.disposer.Disposable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointerContainer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author nik
 */
public class OrderRootsCache {
  private final Map<CacheKey, VirtualFilePointerContainer> myRoots = new ConcurrentHashMap<>();
  private final Disposable myParentDisposable;

  public OrderRootsCache(@Nonnull Disposable parentDisposable) {
    myParentDisposable = parentDisposable;
  }

  public VirtualFilePointerContainer setCachedRoots(OrderRootType rootType, int flags, Collection<String> urls) {
    final VirtualFilePointerContainer container = VirtualFilePointerManager.getInstance().createContainer(myParentDisposable);
    for (String url : urls) {
      container.add(url);
    }
    myRoots.put(new CacheKey(rootType, flags), container);
    return container;
  }

  @Nullable
  public VirtualFile[] getCachedRoots(OrderRootType rootType, int flags) {
    final VirtualFilePointerContainer cached = myRoots.get(new CacheKey(rootType, flags));
    return cached == null ? null : cached.getFiles();
  }

  @Nullable
  public String[] getCachedUrls(OrderRootType rootType, int flags) {
    final VirtualFilePointerContainer cached = myRoots.get(new CacheKey(rootType, flags));
    return cached != null ? cached.getUrls() : null;
  }

  public void clearCache() {
    for (VirtualFilePointerContainer container : myRoots.values()) {
      container.killAll();
    }
    myRoots.clear();
  }

  private static final class CacheKey {
    private final OrderRootType myRootType;
    private final int myFlags;

    private CacheKey(OrderRootType rootType, int flags) {
      myRootType = rootType;
      myFlags = flags;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      CacheKey cacheKey = (CacheKey)o;
      return myFlags == cacheKey.myFlags && myRootType.equals(cacheKey.myRootType);

    }

    @Override
    public int hashCode() {
      return 31 * myRootType.hashCode() + myFlags;
    }
  }
}
