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
package consulo.language.index.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.event.ApplicationListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.store.internal.BatchUpdateListener;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.OrderRootType;
import consulo.disposer.Disposable;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.LibraryScopeCache;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.internal.ModuleScopeProviderInternal;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.module.content.internal.ProjectRootManagerImpl;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.module.content.scope.ModuleScopeProvider;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.content.WatchedRootsProvider;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.event.VirtualFileManagerListener;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ProjectRootManager extended with ability to watch events.
 */
@Singleton
@ServiceImpl
public class ProjectRootManagerComponent extends ProjectRootManagerImpl implements Disposable {
    private static final Logger LOG = Logger.getInstance(ProjectRootManagerComponent.class);

    private boolean myPointerChangesDetected = false;
    private int myInsideWriteAction = 0;

    private Set<LocalFileSystem.WatchRequest> myRootsToWatch = new HashSet<>();
    private final boolean myDoLogCachesUpdate;

    @Inject
    public ProjectRootManagerComponent(Project project, VirtualFileManager virtualFileManager) {
        super(project);

        myDoLogCachesUpdate = ApplicationProperties.isInSandbox();

        if (project.isDefault()) {
            return;
        }

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(FileTypeListener.class, new FileTypeListener() {
            @Override
            @RequiredWriteAction
            public void beforeFileTypesChanged(@Nonnull FileTypeEvent event) {
                beforeRootsChange(true);
            }

            @Override
            @RequiredWriteAction
            public void fileTypesChanged(@Nonnull FileTypeEvent event) {
                rootsChanged(true);
            }
        });

        virtualFileManager.addVirtualFileManagerListener(new VirtualFileManagerListener() {
            @Override
            public void afterRefreshFinish(boolean asynchronous) {
                doUpdateOnRefresh();
            }
        }, this);

        BatchUpdateListener handler = new BatchUpdateListener() {
            @Override
            public void onBatchUpdateStarted() {
                myRootsChanged.levelUp();
                myFileTypesChanged.levelUp();
            }

            @Override
            @RequiredWriteAction
            public void onBatchUpdateFinished() {
                myRootsChanged.levelDown();
                myFileTypesChanged.levelDown();
            }
        };

        connection.subscribe(BatchUpdateListener.class, handler);
    }

    @RequiredReadAction
    public void projectOpened() {
        addRootsToWatch();
        Application application = myProject.getApplication();
        application.addApplicationListener(new AppListener(), myProject);
        myStartupActivityPerformed = true;
    }

    @Override
    public void dispose() {
        LocalFileSystem.getInstance().removeWatchedRoots(myRootsToWatch);
    }

    @Override
    @RequiredReadAction
    protected void addRootsToWatch() {
        Couple<Set<String>> roots = getAllRoots(false);
        if (roots == null) {
            return;
        }
        myRootsToWatch = LocalFileSystem.getInstance().replaceWatchedRoots(myRootsToWatch, roots.first, roots.second);
    }

    @RequiredWriteAction
    private void beforeRootsChange(boolean fileTypes) {
        if (myProject.isDisposed()) {
            return;
        }
        getBatchSession(fileTypes).beforeRootsChanged();
    }

    @RequiredWriteAction
    private void rootsChanged(boolean fileTypes) {
        getBatchSession(fileTypes).rootsChanged();
    }

    private void doUpdateOnRefresh() {
        if (myProject.getApplication().isUnitTestMode() && (!myStartupActivityPerformed || myProject.isDisposed())) {
            return; // in test mode suppress addition to a queue unless project is properly initialized
        }
        if (myProject.isDefault()) {
            return;
        }

        if (myDoLogCachesUpdate) {
            LOG.debug("refresh");
        }
        DumbService dumbService = DumbService.getInstance(myProject);
        DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(myProject);
        if (task != null) {
            dumbService.queueTask(task);
        }
    }

    @Nullable
    @RequiredReadAction
    private Couple<Set<String>> getAllRoots(boolean includeSourceRoots) {
        if (myProject.isDefault()) {
            return null;
        }

        Set<String> recursive = new HashSet<>();
        Set<String> flat = new HashSet<>();

        String projectFilePath = myProject.getProjectFilePath();
        File projectDirFile = new File(projectFilePath).getParentFile();
        if (projectDirFile != null && projectDirFile.getName().equals(Project.DIRECTORY_STORE_FOLDER)) {
            recursive.add(projectDirFile.getAbsolutePath());
        }
        else {
            flat.add(projectFilePath);
            VirtualFile workspaceFile = myProject.getWorkspaceFile();
            if (workspaceFile != null) {
                flat.add(workspaceFile.getPath());
            }
        }

        myProject.getExtensionPoint(WatchedRootsProvider.class).forEach(it -> recursive.addAll(it.getRootsToWatch()));

        addRootsFromModules(includeSourceRoots, recursive, flat);

        return Couple.of(recursive, flat);
    }

    @RequiredReadAction
    private void addRootsFromModules(boolean includeSourceRoots, Set<String> recursive, Set<String> flat) {
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        for (Module module : modules) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

            addRootsToTrack(moduleRootManager.getContentRootUrls(), recursive, flat);

            if (includeSourceRoots) {
                addRootsToTrack(moduleRootManager.getContentFolderUrls(ContentFolderTypeProvider.allExceptExcluded()), recursive, flat);
            }

            OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            for (OrderEntry entry : orderEntries) {
                if (entry instanceof OrderEntryWithTracking) {
                    for (OrderRootType orderRootType : OrderRootType.getAllTypes()) {
                        addRootsToTrack(entry.getUrls(orderRootType), recursive, flat);
                    }
                }
            }
        }
    }

    private static void addRootsToTrack(String[] urls, Collection<String> recursive, Collection<String> flat) {
        for (String url : urls) {
            if (url != null) {
                String protocol = VirtualFileManager.extractProtocol(url);
                if (protocol == null || LocalFileSystem.PROTOCOL.equals(protocol)) {
                    recursive.add(ProjectRootManagerEx.extractLocalPath(url));
                }
                else {
                    VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);
                    if (fileSystem instanceof ArchiveFileSystem) {
                        flat.add(ProjectRootManagerEx.extractLocalPath(url));
                    }
                }
            }
        }
    }

    @Override
    protected void doSynchronizeRoots() {
        if (!myStartupActivityPerformed) {
            return;
        }

        if (myDoLogCachesUpdate) {
            LOG.debug(new Throwable("sync roots"));
        }
        else if (!myProject.getApplication().isUnitTestMode()) {
            LOG.info("project roots have changed");
        }

        DumbService dumbService = DumbService.getInstance(myProject);
        if (FileBasedIndex.getInstance() instanceof FileBasedIndexImpl) {
            dumbService.queueTask(new UnindexedFilesUpdater(myProject));
        }
    }

    @Override
    @RequiredReadAction
    public void markRootsForRefresh() {
        Set<String> paths = Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);
        addRootsFromModules(false, paths, paths);

        LocalFileSystem fs = LocalFileSystem.getInstance();
        for (String path : paths) {
            VirtualFile root = fs.findFileByPath(path);
            if (root instanceof NewVirtualFile newVirtualFile) {
                newVirtualFile.markDirtyRecursively();
            }
        }
    }

    @Override
    protected void clearScopesCaches() {
        super.clearScopesCaches();

        PsiManager.getInstance(myProject).dropPsiCaches();
        LibraryScopeCache.getInstance(myProject).clear();
    }

    @Override
    @RequiredReadAction
    public void clearScopesCachesForModules() {
        super.clearScopesCachesForModules();
        if (!myProject.isModulesReady()) {
            return;
        }

        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        for (Module module : modules) {
            ModuleScopeProvider scopeProvider = ModuleScopeProvider.getInstance(module);
            if (scopeProvider instanceof ModuleScopeProviderInternal moduleScopeProvider) {
                moduleScopeProvider.clearCache();
            }
        }
    }

    private class AppListener implements ApplicationListener {
        @Override
        public void beforeWriteActionStart(@Nonnull Object action) {
            myInsideWriteAction++;
        }

        @Override
        @RequiredWriteAction
        public void writeActionFinished(@Nonnull Object action) {
            if (--myInsideWriteAction == 0 && myPointerChangesDetected) {
                myPointerChangesDetected = false;
                myRootsChanged.levelDown();
            }
        }
    }

    private final VirtualFilePointerListener myRootsChangedListener = new VirtualFilePointerListener() {
        @Override
        @RequiredWriteAction
        public void beforeValidityChanged(@Nonnull VirtualFilePointer[] pointers) {
            if (myProject.isDisposed()) {
                return;
            }

            if (!isInsideWriteAction() && !myPointerChangesDetected) {
                myPointerChangesDetected = true;
                //this is the first pointer changing validity
                myRootsChanged.levelUp();
            }

            myRootsChanged.beforeRootsChanged();
            if (myDoLogCachesUpdate || LOG.isTraceEnabled()) {
                LOG.trace(new Throwable(pointers.length > 0 ? pointers[0].getPresentableUrl() : ""));
            }
        }

        @Override
        @RequiredWriteAction
        public void validityChanged(@Nonnull VirtualFilePointer[] pointers) {
            if (myProject.isDisposed()) {
                return;
            }

            if (isInsideWriteAction()) {
                myRootsChanged.rootsChanged();
            }
            else {
                clearScopesCaches();
            }
        }

        private boolean isInsideWriteAction() {
            return myInsideWriteAction == 0;
        }
    };
    
    @Nonnull
    @Override
    public VirtualFilePointerListener getRootsValidityChangedListener() {
        return myRootsChangedListener;
    }
}
