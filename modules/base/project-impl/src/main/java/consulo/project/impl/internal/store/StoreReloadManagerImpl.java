/*
 * Copyright 2013-2023 consulo.io
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
package consulo.project.impl.internal.store;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.store.impl.internal.storage.StateStorageBase;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.component.store.internal.StateStorage;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.StoreReloadManager;
import consulo.ui.UIAccess;

import consulo.util.collection.Sets;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileManagerListener;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2023-09-11
 */
@Singleton
@ServiceImpl
public class StoreReloadManagerImpl implements StoreReloadManager, Disposable {
    private static final Logger LOG = Logger.getInstance(StoreReloadManagerImpl.class);

    private Future<Void> myChangedFilesFuture = CompletableFuture.completedFuture(null);

    private final Set<StateStorage> myChangedProjectFiles = Sets.newConcurrentHashSet();
    private final AtomicInteger myReloadBlockCount = new AtomicInteger(0);
    private final Project myProject;
    private final ApplicationConcurrency myConcurrency;
    private final AtomicBoolean myReloadInProgress = new AtomicBoolean();

    private final Callable<Void> reloadChangedStoragesTask = () -> {
        if (isReloadUnblocked()) {
            reloadChangedStorages();
        }
        return null;
    };

    @Inject
    public StoreReloadManagerImpl(
        Project project,
        VirtualFileManager virtualFileManager,
        ApplicationConcurrency concurrency
    ) {
        myProject = project;
        myConcurrency = concurrency;

        virtualFileManager.addVirtualFileManagerListener(new VirtualFileManagerListener() {
            @Override
            public void beforeRefreshStart(boolean asynchronous) {
                LOG.warn("RELOAD-DEBUG beforeRefreshStart async=" + asynchronous + debugState());
                blockReloadingProjectOnExternalChanges();
            }

            @Override
            public void afterRefreshFinish(boolean asynchronous) {
                LOG.warn("RELOAD-DEBUG afterRefreshFinish async=" + asynchronous + debugState());
                unblockReloadingProjectOnExternalChanges();
            }
        }, this);
    }

    public void projectStorageFileChanged(VirtualFileEvent event, StateStorage storage, Project project) {
        VirtualFile file = event.getFile();

        if (!StorageUtil.isChangedByStorageOrSaveSession(event) && !(event.getRequestor() instanceof ProjectManager)) {
            registerProjectToReload(file, storage);
        }
    }

    private static String debugState() {
        Application application = Application.get();
        return " [thread=" + Thread.currentThread().getName()
            + ", writeAccess=" + application.isWriteAccessAllowed()
            + ", uiThread=" + UIAccess.isUIThread() + "]";
    }

    /**
     * Reloads the components backed by the storages that changed on disk. Nothing is asked of the user and the
     * project is never reloaded as a whole - each component takes the new state through its own
     * {@code loadState}, and schemes (code style, keymaps, colors, inspection profiles) are refreshed
     * independently by the file tracker in {@code SchemeManagerImpl}.
     */
    private void reloadChangedStorages() {
        LOG.warn("RELOAD-DEBUG reloadChangedStorages enter pending=" + myChangedProjectFiles.size()
            + ", blockCount=" + myReloadBlockCount.get() + ", inProgress=" + myReloadInProgress.get() + debugState());

        if (myProject.isDisposed() || myChangedProjectFiles.isEmpty()) {
            return;
        }

        if (!myReloadInProgress.compareAndSet(false, true)) {
            return;
        }

        Set<StateStorage> causes = new HashSet<>(myChangedProjectFiles);
        myChangedProjectFiles.clear();

        // Re-read the changed storages and diff them against the in-memory (last-saved) state. A storage that we just
        // wrote ourselves has identical on-disk content, so no component actually changed - in that case a filesystem
        // refresh that merely observed our own write must not reload anything.
        Set<String> changedComponentNames = new HashSet<>();
        for (StateStorage storage : causes) {
            try {
                storage.analyzeExternalChangesAndUpdateIfNeed(changedComponentNames);
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        if (changedComponentNames.isEmpty()) {
            finishReload(causes);
            return;
        }

        LOG.warn("RELOAD-DEBUG reinit start components=" + changedComponentNames + debugState());

        try {
            myProject.getInstance(IProjectStore.class)
                .reinitComponents(changedComponentNames)
                .onFinish(continuation -> finishReload(causes))
                .onCancel(continuation -> finishReload(causes));
        }
        catch (Throwable e) {
            LOG.error(e);
            finishReload(causes);
        }
    }

    /**
     * Re-enables the saving that {@link #registerProjectToReload} disabled, otherwise the storage would stay
     * read-only once it has been reloaded.
     */
    private void finishReload(Collection<? extends StateStorage> causes) {
        LOG.warn("RELOAD-DEBUG finishReload" + debugState());

        for (StateStorage cause : causes) {
            if (cause instanceof StateStorageBase stateStorageBase) {
                stateStorageBase.enableSaving();
            }
        }

        myReloadInProgress.set(false);

        // changes that arrived while this reload was running
        if (!myChangedProjectFiles.isEmpty() && isReloadUnblocked()) {
            start();
        }
    }

    private void registerProjectToReload(VirtualFile file, StateStorage storage) {
        LOG.warn("RELOAD-DEBUG registerProjectToReload file=" + file.getName()
            + ", blockCount=" + myReloadBlockCount.get() + debugState());

        myChangedProjectFiles.add(storage);

        if (storage instanceof StateStorageBase) {
            ((StateStorageBase) storage).disableSaving();
        }

        if (isReloadUnblocked()) {
            start();
        }
    }

    private boolean isReloadUnblocked() {
        return myReloadBlockCount.get() == 0;
    }

    private void cancel() {
        myChangedFilesFuture.cancel(false);
        myChangedFilesFuture = CompletableFuture.completedFuture(null);
    }

    private void start() {
        if (myChangedProjectFiles.isEmpty()) {
            return;
        }

        if (myChangedFilesFuture.isDone() || myChangedFilesFuture.isCancelled()) {
            myChangedFilesFuture = myConcurrency.getScheduledExecutorService().schedule(reloadChangedStoragesTask, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void blockReloadingProjectOnExternalChanges() {
        cancel();

        myReloadBlockCount.incrementAndGet();
    }

    @Override
    public void unblockReloadingProjectOnExternalChanges() {
        if (myReloadBlockCount.decrementAndGet() == 0) {
            start();
        }
    }

    
    @Override
    public AccessToken blockReloadingOnExternalChanges() {
        blockReloadingProjectOnExternalChanges();
        return AccessToken.of(this::unblockReloadingProjectOnExternalChanges);
    }

    @Override
    public void dispose() {
        cancel();
    }
}
