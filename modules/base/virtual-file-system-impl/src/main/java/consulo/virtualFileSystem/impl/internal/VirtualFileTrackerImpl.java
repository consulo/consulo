/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.collection.Sets;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.internal.VirtualFileTracker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mike
 */
@Singleton
@ServiceImpl
public class VirtualFileTrackerImpl implements VirtualFileTracker {
  private final Map<String, Set<VirtualFileListener>> myNonRefreshTrackers = new ConcurrentHashMap<String, Set<VirtualFileListener>>();
  private final Map<String, Set<VirtualFileListener>> myAllTrackers = new ConcurrentHashMap<String, Set<VirtualFileListener>>();

  @Inject
  public VirtualFileTrackerImpl(VirtualFileManager virtualFileManager) {
    virtualFileManager.addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void propertyChanged(VirtualFilePropertyEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.propertyChanged(event);
        }
      }

      @Override
      public void contentsChanged(VirtualFileEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.contentsChanged(event);
        }
      }

      @Override
      public void fileCreated(VirtualFileEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;
                                             
        for (VirtualFileListener listener : listeners) {
          listener.fileCreated(event);
        }
      }

      @Override
      public void fileDeleted(VirtualFileEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.fileDeleted(event);
        }
      }

      @Override
      public void fileMoved(VirtualFileMoveEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.fileMoved(event);
        }
      }

      @Override
      public void fileCopied(VirtualFileCopyEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.fileCopied(event);
        }
      }

      @Override
      public void beforePropertyChange(VirtualFilePropertyEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.beforePropertyChange(event);
        }
      }

      @Override
      public void beforeContentsChange(VirtualFileEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.beforeContentsChange(event);
        }
      }

      @Override
      public void beforeFileDeletion(VirtualFileEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.beforeFileDeletion(event);
        }
      }

      @Override
      public void beforeFileMovement(VirtualFileMoveEvent event) {
        Collection<VirtualFileListener> listeners = getListeners(event.getFile(), event.isFromRefresh());
        if (listeners == null) return;

        for (VirtualFileListener listener : listeners) {
          listener.beforeFileMovement(event);
        }
      }
    });
  }

  @Override
  public void addTracker(
    @Nonnull final String fileUrl,
    @Nonnull final VirtualFileListener listener,
    final boolean fromRefreshOnly,
    @Nonnull Disposable parentDisposable) {

    getSet(fileUrl, myAllTrackers).add(listener);

    if (!fromRefreshOnly) {
      getSet(fileUrl, myNonRefreshTrackers).add(listener);
    }

    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        removeListener(fileUrl, listener, myAllTrackers);

        if (!fromRefreshOnly) {
          removeListener(fileUrl, listener, myNonRefreshTrackers);
        }
      }
    });
  }

  private static void removeListener(String fileUrl, VirtualFileListener listener, Map<String, Set<VirtualFileListener>> map) {
    Set<VirtualFileListener> listeners = map.get(fileUrl);
    if (listeners == null) return;

    listeners.remove(listener);
    if (listeners.isEmpty()) {
      map.remove(fileUrl);
    }
  }

  private static Set<VirtualFileListener> getSet(String fileUrl, Map<String, Set<VirtualFileListener>> map) {
    Set<VirtualFileListener> listeners = map.get(fileUrl);

    if (listeners == null) {
      listeners = Sets.newConcurrentHashSet();
      map.put(fileUrl, listeners);
    }
    return listeners;
  }

  @Nullable
  private Collection<VirtualFileListener> getListeners(VirtualFile virtualFile, boolean fromRefresh) {
    Set<VirtualFileListener> listeners = null;

    while (virtualFile != null) {
      String url = virtualFile.getUrl();


      if (!fromRefresh) {
        listeners = addToSet(listeners, myNonRefreshTrackers.get(url));
      }
      else {
        listeners = addToSet(listeners, myAllTrackers.get(url));
      }

      virtualFile = virtualFile.getParent();
    }

    if (listeners == null || listeners.isEmpty()) return null;

    return listeners;
  }

  private static <T> Set<T> addToSet(Set<T> to, Set<T> what) {
    if (what == null || what.size() == 0) return to;

    if (to == null) to = new HashSet<T>();
    to.addAll(what);
    return to;
  }
}
