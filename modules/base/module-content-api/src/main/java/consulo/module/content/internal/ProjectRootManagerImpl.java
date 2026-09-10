// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.module.content.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.RootProvider;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.OrderEnumerator;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.project.Project;
import consulo.project.RootsChangeRescanningInfo;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Lists;
import consulo.util.collection.Maps;
import consulo.util.collection.SmartList;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author max
 */
public class ProjectRootManagerImpl extends ProjectRootManagerEx {
    private static final Logger LOG = Logger.getInstance(ProjectRootManagerImpl.class);

    protected final Project myProject;

    private final OrderRootsCache myRootsCache;

    private final Map<RootProvider, Set<OrderEntry>> myRegisteredRootProviders = Maps.newHashMap(HashingStrategy.identity());
    protected final List<OrderEntryWithTracking> myModuleExtensionWithSdkOrderEntries = new ArrayList<>();
    protected boolean myStartupActivityPerformed = false;
    private final RootProviderChangeListener myRootProviderChangeListener = new RootProviderChangeListener();
    private final VirtualFilePointerListener myRootsValidityChangedListener = new VirtualFilePointerListener() {
    };

    protected abstract class BatchSession<Change, ChangeList> {
        private final boolean myFileTypes;
        private int myBatchLevel;
        private int myPendingRootsChanged;
        private boolean myChanged;
        private @Nullable ChangeList myChanges;

        private BatchSession(boolean fileTypes) {
            myFileTypes = fileTypes;
        }

        public void levelUp() {
            if (myBatchLevel == 0) {
                myChanged = false;
                myChanges = null;
            }
            myBatchLevel += 1;
        }

        @RequiredWriteAction
        public void levelDown() {
            myBatchLevel -= 1;
            if (myChanged && myBatchLevel == 0) {
                try {
                    // todo make sure it should be not null here
                    if (myChanges == null) {
                        myChanges = initiateChangelist(getGenericChange());
                    }
                    myPendingRootsChanged--;
                    ChangeList changes = copy(myChanges);
                    myProject.getApplication().runWriteAction(() -> {
                        fireRootsChanged(changes);
                    });
                }
                finally {
                    if (myPendingRootsChanged == 0) {
                        myChanged = false;
                        myChanges = null;
                    }
                }
            }
        }

        @RequiredWriteAction
        public void beforeRootsChanged() {
            if (myBatchLevel == 0 || !myChanged) {
                fireBeforeRootsChanged(myFileTypes);
                myPendingRootsChanged++;
                myChanged = true;
            }
        }

        @RequiredWriteAction
        public void rootsChanged(Change change) {
            ChangeList current = myChanges;
            ChangeList accumulated = current == null ? initiateChangelist(change) : accumulate(current, change);
            myChanges = accumulated;

            if (myBatchLevel == 0 && myChanged) {
                myPendingRootsChanged--;
                if (fireRootsChanged(copy(accumulated)) && myPendingRootsChanged == 0) {
                    myChanged = false;
                    myChanges = null;
                }
            }
        }

        @RequiredWriteAction
        public void rootsChanged() {
            rootsChanged(getGenericChange());
        }

        protected abstract boolean fireRootsChanged(ChangeList change);

        protected abstract ChangeList initiateChangelist(Change change);

        protected abstract ChangeList accumulate(ChangeList current, Change change);

        protected abstract ChangeList copy(ChangeList changes);

        protected abstract Change getGenericChange();
    }

    protected final BatchSession<RootsChangeRescanningInfo, List<RootsChangeRescanningInfo>> myRootsChanged = new BatchSession<>(false) {
        @Override
        protected boolean fireRootsChanged(List<RootsChangeRescanningInfo> changes) {
            return ProjectRootManagerImpl.this.fireRootsChanged(false, changes);
        }

        @Override
        protected List<RootsChangeRescanningInfo> accumulate(List<RootsChangeRescanningInfo> current, RootsChangeRescanningInfo change) {
            current.add(change);
            return current;
        }

        @Override
        protected RootsChangeRescanningInfo getGenericChange() {
            return RootsChangeRescanningInfo.TOTAL_RESCAN;
        }

        @Override
        protected List<RootsChangeRescanningInfo> initiateChangelist(RootsChangeRescanningInfo change) {
            return new SmartList<>(change);
        }

        @Override
        protected List<RootsChangeRescanningInfo> copy(List<RootsChangeRescanningInfo> changes) {
            return new ArrayList<>(changes);
        }
    };

    protected final BatchSession<Boolean, Boolean> myFileTypesChanged = new BatchSession<>(true) {
        @Override
        protected boolean fireRootsChanged(Boolean change) {
            return ProjectRootManagerImpl.this.fireRootsChanged(true, Collections.emptyList());
        }

        @Override
        protected Boolean accumulate(Boolean current, Boolean change) {
            return current || change;
        }

        @Override
        protected Boolean getGenericChange() {
            return Boolean.TRUE;
        }

        @Override
        protected Boolean initiateChangelist(Boolean change) {
            return change;
        }

        @Override
        protected Boolean copy(Boolean changes) {
            return changes;
        }
    };

    private class RootProviderChangeListener implements RootProvider.RootSetChangedListener {
        private boolean myInsideRootsChange;

        @Override
        @RequiredWriteAction
        public void rootSetChanged(RootProvider wrapper) {
            if (myInsideRootsChange) {
                return;
            }
            myInsideRootsChange = true;
            try {
                makeRootsChange(EmptyRunnable.INSTANCE, buildRootProviderChangeInfo(wrapper));
            }
            finally {
                myInsideRootsChange = false;
            }
        }
    }

    private RootsChangeRescanningInfo buildRootProviderChangeInfo(RootProvider provider) {
        Set<OrderEntry> owners = myRegisteredRootProviders.get(provider);
        if (owners == null || owners.isEmpty()) {
            return RootsChangeRescanningInfo.TOTAL_RESCAN;
        }

        BuildableRootsChangeRescanningInfo builder = BuildableRootsChangeRescanningInfo.newInstance();
        for (OrderEntry owner : owners) {
            if (owner instanceof LibraryOrderEntry libraryOrderEntry) {
                Library library = libraryOrderEntry.getLibrary();
                if (library == null) {
                    return RootsChangeRescanningInfo.TOTAL_RESCAN;
                }
                builder.addLibrary(library);
            }
            else if (owner instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
                Sdk sdk = sdkOrderEntry.getSdk();
                if (sdk == null) {
                    return RootsChangeRescanningInfo.TOTAL_RESCAN;
                }
                builder.addSdk(sdk);
            }
            else if (owner instanceof OrderEntryWithTracking) {
                builder.addOrderEntry(owner);
            }
            else {
                return RootsChangeRescanningInfo.TOTAL_RESCAN;
            }
        }
        return builder.buildInfo();
    }

    public static ProjectRootManagerImpl getInstanceImpl(Project project) {
        return (ProjectRootManagerImpl) getInstance(project);
    }

    public ProjectRootManagerImpl(Project project) {
        myProject = project;
        myRootsCache = new OrderRootsCache(project);
    }

    @Override
    public ProjectFileIndex getFileIndex() {
        return ProjectFileIndex.getInstance(myProject);
    }

    @Override
    public List<String> getContentRootUrls() {
        List<String> result = new ArrayList<>();
        for (Module module : getModuleManager().getModules()) {
            String[] urls = ModuleRootManager.getInstance(module).getContentRootUrls();
            ContainerUtil.addAll(result, urls);
        }
        return result;
    }

    @Override
    public VirtualFile[] getContentRoots() {
        List<VirtualFile> result = new ArrayList<>();
        for (Module module : getModuleManager().getModules()) {
            VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
            ContainerUtil.addAll(result, contentRoots);
        }
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    @Override
    public VirtualFile[] getContentSourceRoots() {
        List<VirtualFile> result = new ArrayList<>();
        for (Module module : getModuleManager().getModules()) {
            VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getContentFolderFiles(ContentFolderTypeProvider.allExceptExcluded());
            ContainerUtil.addAll(result, sourceRoots);
        }
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    @Override
    public OrderEnumerator orderEntries() {
        return new ProjectOrderEnumerator(myProject, myRootsCache);
    }

    @Override
    public OrderEnumerator orderEntries(Collection<? extends Module> modules) {
        return new ModulesOrderEnumerator(myProject, modules);
    }

    @Override
    @RequiredReadAction
    public VirtualFile[] getContentRootsFromAllModules() {
        List<VirtualFile> result = new ArrayList<>();
        Module[] modules = getModuleManager().getSortedModules();
        for (Module module : modules) {
            VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
            ContainerUtil.addAll(result, files);
        }
        result.add(myProject.getBaseDir());
        return VirtualFileUtil.toVirtualFileArray(result);
    }

    @Override
    @RequiredWriteAction
    public void mergeRootsChangesDuring(Runnable runnable) {
        myProject.getApplication().assertWriteAccessAllowed();
        BatchSession<?, ?> batchSession = myRootsChanged;
        batchSession.levelUp();
        try {
            runnable.run();
        }
        finally {
            batchSession.levelDown();
        }
    }

    protected void clearScopesCaches() {
        clearScopesCachesForModules();
    }

    @Override
    public void clearScopesCachesForModules() {
        myRootsCache.clearCache();
        if (!myProject.isModulesReady()) {
            return;
        }
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        for (Module module : modules) {
            ((ModuleRootManagerInternal) ModuleRootManager.getInstance(module)).dropCaches();
        }
    }

    @Deprecated
    @Override
    @RequiredWriteAction
    public void makeRootsChange(Runnable runnable, boolean fileTypes, boolean fireEvents) {
        if (myProject.isDisposed()) {
            return;
        }

        BatchSession<?, ?> session = fileTypes ? myFileTypesChanged : myRootsChanged;
        try {
            if (fireEvents) {
                session.beforeRootsChanged();
            }
            runnable.run();
        }
        finally {
            if (fireEvents) {
                session.rootsChanged();
            }
        }
    }

    @Override
    @RequiredWriteAction
    public void makeRootsChange(Runnable runnable, RootsChangeRescanningInfo changes) {
        if (myProject.isDisposed()) {
            return;
        }
        try {
            myRootsChanged.beforeRootsChanged();
            runnable.run();
        }
        finally {
            myRootsChanged.rootsChanged(changes);
        }
    }

    @Override
    @RequiredWriteAction
    public AutoCloseable withRootsChange(RootsChangeRescanningInfo changes) {
        myRootsChanged.beforeRootsChanged();
        return () -> myRootsChanged.rootsChanged(changes);
    }

    protected BatchSession<?, ?> getBatchSession(boolean fileTypes) {
        return fileTypes ? myFileTypesChanged : myRootsChanged;
    }

    protected boolean myFiringEvent = false;

    @RequiredWriteAction
    private void fireBeforeRootsChanged(boolean fileTypes) {
        myProject.getApplication().assertWriteAccessAllowed();
        LOG.assertTrue(!myFiringEvent, "Do not use API that changes roots from roots events. Try using invoke later or something else.");
        fireBeforeRootsChangeEvent(fileTypes);
    }

    protected void fireBeforeRootsChangeEvent(boolean fileTypes) {
        myFiringEvent = true;
        try {
            myProject.getMessageBus()
                .syncPublisher(ModuleRootListener.class)
                .beforeRootsChange(new ModuleRootEventImpl(myProject, fileTypes));
        }
        finally {
            myFiringEvent = false;
        }
    }

    @RequiredWriteAction
    private boolean fireRootsChanged(boolean fileTypes, List<? extends RootsChangeRescanningInfo> indexingInfos) {
        if (myProject.isDisposed()) {
            return false;
        }

        myProject.getApplication().assertWriteAccessAllowed();
        LOG.assertTrue(!myFiringEvent, "Do not use API that changes roots from roots events. Try using invoke later or something else.");

        clearScopesCaches();

        incModificationCount();

        fireRootsChangedEvent(fileTypes, indexingInfos);

        addRootsToWatch();

        return true;
    }

    protected void fireRootsChangedEvent(boolean fileTypes, List<? extends RootsChangeRescanningInfo> indexingInfos) {
        myFiringEvent = true;
        try {
            myProject.getMessageBus()
                .syncPublisher(ModuleRootListener.class)
                .rootsChanged(new ModuleRootEventImpl(myProject, fileTypes, indexingInfos));
        }
        finally {
            myFiringEvent = false;
        }
    }

    protected void addRootsToWatch() {
    }

    public Project getProject() {
        return myProject;
    }

    @Override
    public List<VirtualFile> markRootsForRefresh() {
        return List.of();
    }

    private ModuleManager getModuleManager() {
        return ModuleManager.getInstance(myProject);
    }

    public void subscribeToRootProvider(OrderEntry owner, RootProvider provider) {
        Set<OrderEntry> owners = myRegisteredRootProviders.get(provider);
        if (owners == null) {
            owners = new HashSet<>();
            myRegisteredRootProviders.put(provider, owners);
            provider.addRootSetChangedListener(myRootProviderChangeListener);
        }
        owners.add(owner);
    }

    public void unsubscribeFromRootProvider(OrderEntry owner, RootProvider provider) {
        Set<OrderEntry> owners = myRegisteredRootProviders.get(provider);
        if (owners != null) {
            owners.remove(owner);
            if (owners.isEmpty()) {
                provider.removeRootSetChangedListener(myRootProviderChangeListener);
                myRegisteredRootProviders.remove(provider);
            }
        }
    }

    private boolean isLibraryTracked(Library library) {
        return myRegisteredRootProviders.containsKey(library.getRootProvider());
    }

    public void addListenerForTable(LibraryTable.Listener libraryListener, LibraryTable libraryTable) {
        LibraryTableMultilistener multilistener = myLibraryTableMultilisteners.get(libraryTable);
        if (multilistener == null) {
            multilistener = new LibraryTableMultilistener(libraryTable);
        }
        multilistener.addListener(libraryListener);
    }

    @Override
    public void addOrderWithTracking(OrderEntryWithTracking orderEntry) {
        myModuleExtensionWithSdkOrderEntries.add(orderEntry);
    }

    @Override
    public void removeOrderWithTracking(OrderEntryWithTracking orderEntry) {
        myModuleExtensionWithSdkOrderEntries.remove(orderEntry);
    }

    public void removeListenerForTable(LibraryTable.Listener libraryListener, LibraryTable libraryTable) {
        LibraryTableMultilistener multilistener = myLibraryTableMultilisteners.get(libraryTable);
        if (multilistener == null) {
            multilistener = new LibraryTableMultilistener(libraryTable);
        }
        multilistener.removeListener(libraryListener);
    }

    private final Map<LibraryTable, LibraryTableMultilistener> myLibraryTableMultilisteners = new HashMap<>();

    private class LibraryTableMultilistener implements LibraryTable.Listener {
        final List<LibraryTable.Listener> myListeners = Lists.newLockFreeCopyOnWriteList();
        private final LibraryTable myLibraryTable;

        private LibraryTableMultilistener(LibraryTable libraryTable) {
            myLibraryTable = libraryTable;
            myLibraryTable.addListener(this);
            myLibraryTableMultilisteners.put(myLibraryTable, this);
        }

        private void addListener(LibraryTable.Listener listener) {
            myListeners.add(listener);
        }

        private void removeListener(LibraryTable.Listener listener) {
            myListeners.remove(listener);
            if (myListeners.isEmpty()) {
                myLibraryTable.removeListener(this);
                myLibraryTableMultilisteners.remove(myLibraryTable);
            }
        }

        @RequiredWriteAction
        private void fireRootsChanged(Library library, RootsChangeRescanningInfo info) {
            if (isLibraryTracked(library)) {
                makeRootsChange(EmptyRunnable.INSTANCE, info);
            }
        }

        @Override
        @RequiredWriteAction
        public void afterLibraryAdded(Library newLibrary) {
            incModificationCount();
            mergeRootsChangesDuring(() -> {
                for (LibraryTable.Listener listener : myListeners) {
                    listener.afterLibraryAdded(newLibrary);
                }
                fireRootsChanged(newLibrary, BuildableRootsChangeRescanningInfo.newInstance().addLibrary(newLibrary).buildInfo());
            });
        }

        @Override
        @RequiredWriteAction
        public void afterLibraryRenamed(Library library) {
            incModificationCount();
            mergeRootsChangesDuring(() -> {
                for (LibraryTable.Listener listener : myListeners) {
                    listener.afterLibraryRenamed(library);
                }
                fireRootsChanged(library, BuildableRootsChangeRescanningInfo.newInstance().addLibrary(library).buildInfo());
            });
        }

        @Override
        @RequiredWriteAction
        public void beforeLibraryRemoved(Library library) {
            incModificationCount();
            mergeRootsChangesDuring(() -> {
                for (LibraryTable.Listener listener : myListeners) {
                    listener.beforeLibraryRemoved(library);
                }
                fireRootsChanged(library, RootsChangeRescanningInfo.NO_RESCAN_NEEDED);
            });
        }

        @Override
        @RequiredWriteAction
        public void afterLibraryRemoved(Library library) {
            incModificationCount();
            mergeRootsChangesDuring(() -> {
                for (LibraryTable.Listener listener : myListeners) {
                    listener.afterLibraryRemoved(library);
                }
                fireRootsChanged(library, RootsChangeRescanningInfo.NO_RESCAN_NEEDED);
            });
        }
    }

    public VirtualFilePointerListener getRootsValidityChangedListener() {
        return myRootsValidityChangedListener;
    }
}
