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
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.store.impl.internal.storage.StateStorageBase;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.component.store.internal.StateStorage;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.StoreReloadManager;
import consulo.ui.Alerts;
import consulo.util.collection.Sets;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileManagerListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
 * @since 11/09/2023
 */
@Singleton
@ServiceImpl
public class StoreReloadManagerImpl implements StoreReloadManager, Disposable {
    private static final Logger LOG = Logger.getInstance(StoreReloadManagerImpl.class);

    private Future<Void> myChangedFilesFuture = CompletableFuture.completedFuture(null);

    private final Set<StateStorage> myChangedProjectFiles = Sets.newConcurrentHashSet();
    private final AtomicInteger myReloadBlockCount = new AtomicInteger(0);
    private final Project myProject;
    private final ProjectManager myProjectManager;
    private final AtomicBoolean myDialogShow = new AtomicBoolean();

    private final Callable<Void> restartApplicationOrReloadProjectTask = () -> {
        if (isReloadUnblocked()) {
            askToReloadProjectIfConfigFilesChangedExternally();
        }
        return null;
    };

    @Inject
    public StoreReloadManagerImpl(
        Project project,
        VirtualFileManager virtualFileManager,
        ProjectManager projectManager
    ) {
        myProject = project;
        myProjectManager = projectManager;

        virtualFileManager.addVirtualFileManagerListener(new VirtualFileManagerListener() {
            @Override
            public void beforeRefreshStart(boolean asynchronous) {
                blockReloadingProjectOnExternalChanges();
            }

            @Override
            public void afterRefreshFinish(boolean asynchronous) {
                unblockReloadingProjectOnExternalChanges();
            }
        }, this);

        AppExecutorUtil.getAppScheduledExecutorService().schedule(restartApplicationOrReloadProjectTask, 1, TimeUnit.SECONDS);
    }

    public void projectStorageFileChanged(@Nonnull VirtualFileEvent event, @Nonnull StateStorage storage, @Nonnull Project project) {
        VirtualFile file = event.getFile();

        if (!StorageUtil.isChangedByStorageOrSaveSession(event) && !(event.getRequestor() instanceof ProjectManager)) {
            registerProjectToReload(file, storage);
        }
    }

    private void askToReloadProjectIfConfigFilesChangedExternally() {
        if (myChangedProjectFiles.isEmpty()) {
            return;
        }

        if (myDialogShow.compareAndSet(false, true)) {
            shouldReloadProject(myProject).thenAccept(restart -> {
                myDialogShow.set(false);

                if (restart) {
                    myProjectManager.reloadProject(myProject, myProject.getUIAccess());
                }
            });
        }
    }

    private CompletableFuture<Boolean> shouldReloadProject(@Nonnull Project project) {
        if (project.isDisposed() || myChangedProjectFiles.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        Set<StateStorage> causes = new HashSet<>(myChangedProjectFiles);
        myChangedProjectFiles.clear();
        if (causes.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return askToRestart(project, causes);
    }

    private static CompletableFuture<Boolean> askToRestart(Project project, @Nullable Collection<? extends StateStorage> changedStorages) {
        StringBuilder message = new StringBuilder();
        message.append("Project components were changed externally and cannot be reloaded");

        message.append("\nWould you like to ");
        message.append("reload project?");

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        project.getUIAccess().give(() -> {
            Alerts.yesNo()
                .title(LocalizeValue.localizeTODO("Project Files Changed"))
                .text(message.toString())
                .showAsync(project)
                .doWhenDone((it) -> {
                    if (it) {
                        if (changedStorages != null) {
                            for (StateStorage storage : changedStorages) {
                                if (storage instanceof StateStorageBase) {
                                    ((StateStorageBase) storage).disableSaving();
                                }
                            }
                        }
                    }
                    future.complete(it);
                });
        });

        return future;
    }

    private void registerProjectToReload(@Nonnull VirtualFile file, @Nonnull StateStorage storage) {
        LOG.info("[RELOAD] Registering project to reload: " + file + ", project: " + myProject.getBasePath());

        myChangedProjectFiles.add(storage);

        if (storage instanceof StateStorageBase) {
            ((StateStorageBase) storage).disableSaving();
        }

        if (isReloadUnblocked()) {
            start();
        }
    }

    private boolean isReloadUnblocked() {
        int count = myReloadBlockCount.get();
        if (LOG.isDebugEnabled()) {
            LOG.debug("[RELOAD] myReloadBlockCount = " + count);
        }
        return count == 0;
    }

    private void cancel() {
        myChangedFilesFuture.cancel(false);
        myChangedFilesFuture = CompletableFuture.completedFuture(null);
    }

    private void start() {
        if (myChangedFilesFuture.isDone() || myChangedFilesFuture.isCancelled()) {
            myChangedFilesFuture =
                AppExecutorUtil.getAppScheduledExecutorService().schedule(restartApplicationOrReloadProjectTask, 1, TimeUnit.SECONDS);
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

    @Nonnull
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
