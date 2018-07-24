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
package com.intellij.openapi.vfs.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.CachingVirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import gnu.trove.THashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VirtualFileManagerImpl extends VirtualFileManagerEx implements Disposable {
  private static final Logger LOG = Logger.getInstance(VirtualFileManagerImpl.class);

  private final Map<String, VirtualFileSystem> myVirtualFileSystems = new THashMap<>();
  private final List<VirtualFileSystem> myPhysicalFileSystems = new ArrayList<>();
  private final EventDispatcher<VirtualFileListener> myVirtualFileListenerMulticaster = EventDispatcher.create(VirtualFileListener.class);
  private final List<VirtualFileManagerListener> myVirtualFileManagerListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private int myRefreshCount = 0;

  public VirtualFileManagerImpl(@Nonnull MessageBus bus) {
    this(VirtualFileSystem.EP_NAME.getExtensions(), bus);
  }

  public VirtualFileManagerImpl(@Nonnull VirtualFileSystem[] fileSystems, @Nonnull MessageBus bus) {
    for (VirtualFileSystem fileSystem : fileSystems) {
      myVirtualFileSystems.put(fileSystem.getProtocol(), fileSystem);

      if (fileSystem.isPhysical()) {
        registerPhysicalFileSystem(fileSystem);
      }

      if (fileSystem instanceof Disposable) {
        Disposer.register(this, (Disposable)fileSystem);
      }
    }

    if (LOG.isDebugEnabled()) {
      addVirtualFileListener(new LoggingListener());
    }

    bus.connect().subscribe(VFS_CHANGES, new BulkVirtualFileListenerAdapter(myVirtualFileListenerMulticaster.getMulticaster()));
  }

  private void registerPhysicalFileSystem(@Nonnull VirtualFileSystem fileSystem) {
    if (!(fileSystem instanceof CachingVirtualFileSystem)) {
      fileSystem.addVirtualFileListener(myVirtualFileListenerMulticaster.getMulticaster());
    }
    myPhysicalFileSystems.add(fileSystem);
  }

  @Override
  public long getStructureModificationCount() {
    return 0;
  }

  @Override
  @Nullable
  public VirtualFileSystem getFileSystem(@Nullable String protocol) {
    return myVirtualFileSystems.get(protocol);
  }

  @Override
  public long syncRefresh() {
    return doRefresh(false, null);
  }

  @Override
  public long asyncRefresh(@Nullable Runnable postAction) {
    return doRefresh(true, postAction);
  }

  protected long doRefresh(boolean asynchronous, @Nullable Runnable postAction) {
    if (!asynchronous) {
      ApplicationManager.getApplication().assertIsDispatchThread();
    }

    for (VirtualFileSystem fileSystem : getPhysicalFileSystems()) {
      if (!(fileSystem instanceof CachingVirtualFileSystem)) {
        fileSystem.refresh(asynchronous);
      }
    }

    return 0;
  }

  @Override
  public void refreshWithoutFileWatcher(final boolean asynchronous) {
    if (!asynchronous) {
      ApplicationManager.getApplication().assertIsDispatchThread();
    }

    for (VirtualFileSystem fileSystem : getPhysicalFileSystems()) {
      if (fileSystem instanceof CachingVirtualFileSystem) {
        ((CachingVirtualFileSystem)fileSystem).refreshWithoutFileWatcher(asynchronous);
      }
      else {
        fileSystem.refresh(asynchronous);
      }
    }
  }

  private List<VirtualFileSystem> getPhysicalFileSystems() {
    return myPhysicalFileSystems;
  }

  @Override
  public VirtualFile findFileByUrl(@Nonnull String url) {
    VirtualFileSystem fileSystem = getFileSystemForUrl(url);
    if (fileSystem == null) return null;
    return fileSystem.findFileByPath(extractPath(url));
  }

  @Override
  public VirtualFile refreshAndFindFileByUrl(@Nonnull String url) {
    VirtualFileSystem fileSystem = getFileSystemForUrl(url);
    if (fileSystem == null) return null;
    return fileSystem.refreshAndFindFileByPath(extractPath(url));
  }

  @Nullable
  private VirtualFileSystem getFileSystemForUrl(String url) {
    String protocol = extractProtocol(url);
    if (protocol == null) return null;
    return getFileSystem(protocol);
  }

  @Override
  public void addVirtualFileListener(@Nonnull VirtualFileListener listener) {
    myVirtualFileListenerMulticaster.addListener(listener);
  }

  @Override
  public void addVirtualFileListener(@Nonnull VirtualFileListener listener, @Nonnull Disposable parentDisposable) {
    myVirtualFileListenerMulticaster.addListener(listener, parentDisposable);
  }

  @Override
  public void removeVirtualFileListener(@Nonnull VirtualFileListener listener) {
    myVirtualFileListenerMulticaster.removeListener(listener);
  }

  @Override
  public void addVirtualFileManagerListener(@Nonnull VirtualFileManagerListener listener) {
    myVirtualFileManagerListeners.add(listener);
  }

  @Override
  public void addVirtualFileManagerListener(@Nonnull final VirtualFileManagerListener listener, @Nonnull Disposable parentDisposable) {
    addVirtualFileManagerListener(listener);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        removeVirtualFileManagerListener(listener);
      }
    });
  }

  @Override
  public void removeVirtualFileManagerListener(@Nonnull VirtualFileManagerListener listener) {
    myVirtualFileManagerListeners.remove(listener);
  }

  @Override
  public void notifyPropertyChanged(@Nonnull final VirtualFile virtualFile, @Nonnull final String property, final Object oldValue, final Object newValue) {
    final Application application = ApplicationManager.getApplication();
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        if (virtualFile.isValid() && !application.isDisposed()) {
          application.runWriteAction(new Runnable() {
            @Override
            public void run() {
              List<VFilePropertyChangeEvent> events = Collections.singletonList(new VFilePropertyChangeEvent(this, virtualFile, property, oldValue, newValue, false));
              BulkFileListener listener = application.getMessageBus().syncPublisher(VirtualFileManager.VFS_CHANGES);
              listener.before(events);
              listener.after(events);
            }
          });
        }
      }
    };
    application.invokeLater(runnable, ModalityState.NON_MODAL);
  }

  @Override
  public void fireBeforeRefreshStart(boolean asynchronous) {
    if (myRefreshCount++ == 0) {
      for (final VirtualFileManagerListener listener : myVirtualFileManagerListeners) {
        try {
          listener.beforeRefreshStart(asynchronous);
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public void fireAfterRefreshFinish(boolean asynchronous) {
    if (--myRefreshCount == 0) {
      for (final VirtualFileManagerListener listener : myVirtualFileManagerListeners) {
        try {
          listener.afterRefreshFinish(asynchronous);
        }
        catch (Exception e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public long getModificationCount() {
    return 0;
  }

  @Override
  public List<LocalFileProvider> getLocalFileProviders() {
    return ContainerUtil.findAll(myPhysicalFileSystems, LocalFileProvider.class);
  }

  @Override
  public void dispose() {

  }

  private static class LoggingListener implements VirtualFileListener {
    @Override
    public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
      LOG.debug("propertyChanged: file = " +
                event.getFile() +
                ", propertyName = " +
                event.getPropertyName() +
                ", oldValue = " +
                event.getOldValue() +
                ", newValue = " +
                event.getNewValue() +
                ", requestor = " +
                event.getRequestor());
    }

    @Override
    public void contentsChanged(@Nonnull VirtualFileEvent event) {
      LOG.debug("contentsChanged: file = " + event.getFile() + ", requestor = " + event.getRequestor());
    }

    @Override
    public void fileCreated(@Nonnull VirtualFileEvent event) {
      LOG.debug("fileCreated: file = " + event.getFile() + ", requestor = " + event.getRequestor());
    }

    @Override
    public void fileDeleted(@Nonnull VirtualFileEvent event) {
      LOG.debug("fileDeleted: file = " + event.getFile() + ", parent = " + event.getParent() + ", requestor = " + event.getRequestor());
    }

    @Override
    public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
      LOG.debug("fileMoved: file = " + event.getFile() + ", oldParent = " + event.getOldParent() + ", newParent = " + event.getNewParent() + ", requestor = " + event.getRequestor());
    }

    @Override
    public void fileCopied(@Nonnull VirtualFileCopyEvent event) {
      LOG.debug("fileCopied: file = " + event.getFile() + "originalFile = " + event.getOriginalFile() + ", requestor = " + event.getRequestor());
    }

    @Override
    public void beforeContentsChange(@Nonnull VirtualFileEvent event) {
      LOG.debug("beforeContentsChange: file = " + event.getFile() + ", requestor = " + event.getRequestor());
    }

    @Override
    public void beforePropertyChange(@Nonnull VirtualFilePropertyEvent event) {
      LOG.debug("beforePropertyChange: file = " +
                event.getFile() +
                ", propertyName = " +
                event.getPropertyName() +
                ", oldValue = " +
                event.getOldValue() +
                ", newValue = " +
                event.getNewValue() +
                ", requestor = " +
                event.getRequestor());
    }

    @Override
    public void beforeFileDeletion(@Nonnull VirtualFileEvent event) {
      LOG.debug("beforeFileDeletion: file = " + event.getFile() + ", requestor = " + event.getRequestor());
      LOG.assertTrue(event.getFile().isValid());
    }

    @Override
    public void beforeFileMovement(@Nonnull VirtualFileMoveEvent event) {
      LOG.debug("beforeFileMovement: file = " + event.getFile() + ", oldParent = " + event.getOldParent() + ", newParent = " + event.getNewParent() + ", requestor = " + event.getRequestor());
    }
  }
}
