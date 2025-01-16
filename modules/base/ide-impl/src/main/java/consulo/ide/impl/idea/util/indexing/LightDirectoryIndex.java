/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.indexing;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * This is a light version of DirectoryIndexImpl
 *
 * @author gregsh
 */
public final class LightDirectoryIndex<T> {
  private final ConcurrentIntObjectMap<T> myInfoCache = ContainerUtil.createConcurrentIntObjectMap();
  private final T myDefValue;
  private final Consumer<LightDirectoryIndex<T>> myInitializer;

  public LightDirectoryIndex(@Nonnull Disposable parentDisposable, @Nonnull T defValue, @Nonnull Consumer<LightDirectoryIndex<T>> initializer) {
    myDefValue = defValue;
    myInitializer = initializer;
    resetIndex();
    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(parentDisposable);
    connection.subscribe(FileTypeListener.class, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@Nonnull FileTypeEvent event) {
        resetIndex();
      }
    });

    connection.subscribe(BulkFileListener.class, new BulkFileListener() {
      @Override
      public void before(@Nonnull List<? extends VFileEvent> events) {
      }

      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
          VirtualFile file = event.getFile();
          if (file == null || file.isDirectory()) {
            resetIndex();
            break;
          }
        }
      }
    });
  }

  public void resetIndex() {
    myInfoCache.clear();
    myInitializer.accept(this);
  }

  public void putInfo(@Nullable VirtualFile file, @Nonnull T value) {
    if (!(file instanceof VirtualFileWithId)) return;
    cacheInfo(file, value);
  }

  @Nonnull
  public T getInfoForFile(@Nullable VirtualFile file) {
    if (!(file instanceof VirtualFileWithId)) return myDefValue;

    VirtualFile dir;
    if (!file.isDirectory()) {
      T info = getCachedInfo(file);
      if (info != null) {
        return info;
      }
      dir = file.getParent();
    }
    else {
      dir = file;
    }

    int count = 0;
    for (VirtualFile root = dir; root != null; root = root.getParent()) {
      if (++count > 1000) {
        throw new IllegalStateException("Possible loop in tree, started at " + dir.getName());
      }
      T info = getCachedInfo(root);
      if (info != null) {
        if (!dir.equals(root)) {
          cacheInfos(dir, root, info);
        }
        return info;
      }
    }

    return cacheInfos(dir, null, myDefValue);
  }

  @Nonnull
  private T cacheInfos(VirtualFile dir, @Nullable VirtualFile stopAt, @Nonnull T info) {
    while (dir != null) {
      cacheInfo(dir, info);
      if (dir.equals(stopAt)) {
        break;
      }
      dir = dir.getParent();
    }
    return info;
  }

  private void cacheInfo(VirtualFile file, T info) {
    myInfoCache.put(((VirtualFileWithId)file).getId(), info);
  }

  private T getCachedInfo(VirtualFile file) {
    return myInfoCache.get(((VirtualFileWithId)file).getId());
  }

}
