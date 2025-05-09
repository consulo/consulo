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
package consulo.virtualFileSystem.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.util.Iconable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;

public class BaseVirtualFileManager extends VirtualFileManagerEx {
    private static final Logger LOG = Logger.getInstance(BaseVirtualFileManager.class);

    private final EventDispatcher<VirtualFileListener> myVirtualFileListenerMulticaster = EventDispatcher.create(VirtualFileListener.class);
    private final List<VirtualFileManagerListener> myVirtualFileManagerListeners = Lists.newLockFreeCopyOnWriteList();

    private final Map<String, VirtualFileSystem> myVirtualFileSystems = new HashMap<>();
    private final List<VirtualFileSystem> myRefreshableFileSystems = new ArrayList<>();
    private final List<AsyncFileListener> myAsyncFileListeners = Lists.newLockFreeCopyOnWriteList();
    @Nonnull
    protected final Application myApplication;
    private int myRefreshCount = 0;

    @Inject
    public BaseVirtualFileManager(@Nonnull Application application) {
        this(application, application.getExtensionPoint(VirtualFileSystem.class).getExtensionList());
    }

    public BaseVirtualFileManager(@Nonnull Application application, @Nonnull List<VirtualFileSystem> fileSystems) {
        myApplication = application;
        for (VirtualFileSystem system : fileSystems) {
            registerFileSystem(system);
        }

        if (LOG.isDebugEnabled()) {
            addVirtualFileListener(new LoggingListener());
        }

        MessageBusConnection connect = application.getMessageBus().connect();
        connect.subscribe(BulkFileListener.class, new BulkVirtualFileListenerAdapter(myVirtualFileListenerMulticaster.getMulticaster()));

        addVirtualFileListener(application.getMessageBus().syncPublisher(VirtualFileListener.class));
    }

    private void registerFileSystem(@Nonnull VirtualFileSystem fileSystem) {
        if (myVirtualFileSystems.containsKey(fileSystem.getProtocol())) {
            throw new IllegalArgumentException("Duplicate file system with protocol: " + fileSystem.getProtocol());
        }

        if (!(fileSystem instanceof CachingVirtualFileSystem)) {
            fileSystem.addVirtualFileListener(myVirtualFileListenerMulticaster.getMulticaster());
        }

        if (fileSystem instanceof RefreshableFileSystem) {
            myRefreshableFileSystems.add(fileSystem);
        }
        myVirtualFileSystems.put(fileSystem.getProtocol(), fileSystem);
    }

    @Override
    public long getStructureModificationCount() {
        return 0;
    }

    @Override
    @Nullable
    public VirtualFileSystem getFileSystem(@Nonnull String protocol) {
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

    @RequiredUIAccess
    protected long doRefresh(boolean asynchronous, @Nullable Runnable postAction) {
        if (!asynchronous) {
            UIAccess.assertIsUIThread();
        }

        for (VirtualFileSystem fileSystem : myRefreshableFileSystems) {
            if (!(fileSystem instanceof CachingVirtualFileSystem)) {
                fileSystem.refresh(asynchronous);
            }
        }

        return 0;
    }

    @Override
    @RequiredUIAccess
    public void refreshWithoutFileWatcher(final boolean asynchronous) {
        if (!asynchronous) {
            UIAccess.assertIsUIThread();
        }

        for (VirtualFileSystem fileSystem : myRefreshableFileSystems) {
            if (fileSystem instanceof CachingVirtualFileSystem) {
                ((CachingVirtualFileSystem) fileSystem).refreshWithoutFileWatcher(asynchronous);
            }
            else {
                fileSystem.refresh(asynchronous);
            }
        }
    }

    @Override
    public VirtualFile findFileByUrl(@Nonnull String url) {
        VirtualFileSystem fileSystem = getFileSystemForUrl(url);
        if (fileSystem == null) {
            return null;
        }
        return fileSystem.findFileByPath(extractPath(url));
    }

    @Override
    public VirtualFile refreshAndFindFileByUrl(@Nonnull String url) {
        VirtualFileSystem fileSystem = getFileSystemForUrl(url);
        if (fileSystem == null) {
            return null;
        }
        return fileSystem.refreshAndFindFileByPath(extractPath(url));
    }

    @Nullable
    private VirtualFileSystem getFileSystemForUrl(String url) {
        String protocol = extractProtocol(url);
        if (protocol == null) {
            return null;
        }
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
        Disposer.register(parentDisposable, () -> removeVirtualFileManagerListener(listener));
    }

    @Override
    public void removeVirtualFileManagerListener(@Nonnull VirtualFileManagerListener listener) {
        myVirtualFileManagerListeners.remove(listener);
    }

    @Override
    public void addAsyncFileListener(@Nonnull AsyncFileListener listener, @Nonnull Disposable parentDisposable) {
        myAsyncFileListeners.add(listener);
        Disposer.register(parentDisposable, () -> myAsyncFileListeners.remove(listener));
    }

    public List<AsyncFileListener> getAsyncFileListeners() {
        return Collections.unmodifiableList(myAsyncFileListeners);
    }

    @Override
    public void notifyPropertyChanged(@Nonnull final VirtualFile virtualFile,
                                      @Nonnull final String property,
                                      final Object oldValue,
                                      final Object newValue) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (virtualFile.isValid() && !myApplication.isDisposed()) {
                    myApplication.runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            List<VFilePropertyChangeEvent> events =
                                Collections.singletonList(new VFilePropertyChangeEvent(this, virtualFile, property, oldValue, newValue, false));
                            BulkFileListener listener = myApplication.getMessageBus().syncPublisher(BulkFileListener.class);
                            listener.before(events);
                            listener.after(events);
                        }
                    });
                }
            }
        };
        myApplication.invokeLater(runnable, myApplication.getNoneModalityState());
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
        return ContainerUtil.findAll(myVirtualFileSystems.values(), LocalFileProvider.class);
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
            LOG.debug("fileMoved: file = " + event.getFile() + ", oldParent = " + event.getOldParent() + ", newParent = " + event.getNewParent() + ", requestor = " + event
                .getRequestor());
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
            LOG.debug("beforeFileMovement: file = " + event.getFile() + ", oldParent = " + event.getOldParent() + ", newParent = " + event.getNewParent() + ", requestor = " + event
                .getRequestor());
        }
    }

    @Override
    public int storeName(@Nonnull String name) {
        throw new AbstractMethodError();
    }

    @Nonnull
    @Override
    public CharSequence getVFileName(int nameId) {
        throw new AbstractMethodError();
    }

    @Override
    @Nullable
    public VirtualFile findFileByNioPath(@Nonnull Path path) {
        return findByNioPath(path, false);
    }

    @Override
    @Nullable
    public VirtualFile refreshAndFindFileByNioPath(@Nonnull Path path) {
        return findByNioPath(path, true);
    }

    @Nullable
    private VirtualFile findByNioPath(@Nonnull Path nioPath, boolean refresh) {
        if (!FileSystems.getDefault().equals(nioPath.getFileSystem())) {
            return null;
        }
        VirtualFileSystem fileSystem = getFileSystem(StandardFileSystems.FILE_PROTOCOL);
        if (fileSystem == null) {
            return null;
        }
        String path = nioPath.toString();
        return refresh ? fileSystem.refreshAndFindFileByPath(path) : fileSystem.findFileByPath(path);
    }

    @Override
    public Image getBaseFileIcon(@Nonnull VirtualFile file) {
        return VirtualFilePresentation.getIcon(file);
    }

    @Override
    public Image getFileIcon(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return null;
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public Image getFileIconNoDefer(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return Image.empty(Image.DEFAULT_ICON_SIZE);
    }
}
