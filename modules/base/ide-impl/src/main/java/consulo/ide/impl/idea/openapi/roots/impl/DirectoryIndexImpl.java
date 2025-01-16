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
package consulo.ide.impl.idea.openapi.roots.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressManager;
import consulo.application.util.LowMemoryWatcher;
import consulo.application.util.query.Query;
import consulo.component.messagebus.MessageBusConnection;
import consulo.content.ContentFolderTypeProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.file.event.FileTypeEvent;
import consulo.language.file.event.FileTypeListener;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.event.ModuleListener;
import consulo.module.extension.event.ModuleExtensionChangeListener;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
@ServiceImpl
public class DirectoryIndexImpl extends DirectoryIndex implements Disposable {
    private static final Logger LOG = Logger.getInstance(DirectoryIndexImpl.class);

    private final Project myProject;
    private final MessageBusConnection myConnection;

    private volatile boolean myDisposed = false;
    private volatile RootIndexImpl myRootIndex = null;

    @Inject
    @RequiredReadAction
    public DirectoryIndexImpl(@Nonnull Project project) {
        myProject = project;
        myConnection = project.getMessageBus().connect(project);

        if (myProject.isDefault()) {
            return;
        }

        myConnection.subscribe(FileTypeListener.class, new FileTypeListener() {
            @Override
            public void fileTypesChanged(@Nonnull FileTypeEvent event) {
                myRootIndex = null;
            }
        });

        myConnection.subscribe(ModuleExtensionChangeListener.class, (oldExtension, newExtension) -> {
            myRootIndex = null;
        });

        myConnection.subscribe(ModuleListener.class, new ModuleListener() {
            @Override
            public void moduleAdded(Project project, Module module) {
                myRootIndex = null;
            }

            @Override
            public void beforeModuleRemoved(Project project, Module module) {
                myRootIndex = null;
            }

            @Override
            public void moduleRemoved(Project project, Module module) {
                myRootIndex = null;
            }

            @Override
            public void modulesRenamed(Project project, List<Module> modules) {
                myRootIndex = null;
            }
        });

        myConnection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                myRootIndex = null;
            }
        });

        myConnection.subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(@Nonnull List<? extends VFileEvent> events) {
                RootIndexImpl rootIndex = myRootIndex;
                if (rootIndex != null && rootIndex.resetOnEvents(events)) {
                    myRootIndex = null;
                }
            }
        });

        LowMemoryWatcher.register(() -> {
            RootIndexImpl index = myRootIndex;
            if (index != null) {
                index.onLowMemory();
            }
        }, project);
    }

    private void dispatchPendingEvents() {
        myConnection.deliverImmediately();
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myRootIndex = null;
    }

    @Override
    @Nonnull
    public Query<VirtualFile> getDirectoriesByPackageName(@Nonnull String packageName, boolean includeLibrarySources) {
        return getRootIndex().getDirectoriesByPackageName(packageName, includeLibrarySources);
    }

    @Nonnull
    private RootIndex getRootIndex() {
        if (!myProject.isModulesReady()) {
            return StubRootIndex.INSTANCE;
        }

        RootIndexImpl rootIndex = myRootIndex;
        if (rootIndex == null) {
            myRootIndex = rootIndex = new RootIndexImpl(myProject, createRootInfoCache());
        }
        return rootIndex;
    }

    protected RootIndexImpl.InfoCache createRootInfoCache() {
        return new RootIndexImpl.InfoCache() {
            // Upsource can't use int-mapping because different files may have the same id there
            private final ConcurrentIntObjectMap<DirectoryInfo> myInfoCache = ContainerUtil.createConcurrentIntObjectMap();

            @Override
            public void cacheInfo(@Nonnull VirtualFile dir, @Nonnull DirectoryInfo info) {
                myInfoCache.put(((NewVirtualFile) dir).getId(), info);
            }

            @Override
            public DirectoryInfo getCachedInfo(@Nonnull VirtualFile dir) {
                return myInfoCache.get(((NewVirtualFile) dir).getId());
            }
        };
    }

    @Override
    public DirectoryInfo getInfoForDirectory(@Nonnull VirtualFile dir) {
        DirectoryInfo info = getInfoForFile(dir);
        return info.isInProject(dir) ? info : null;
    }

    @Nonnull
    @Override
    public DirectoryInfo getInfoForFile(@Nonnull VirtualFile file) {
        checkAvailability();
        dispatchPendingEvents();

        if (!(file instanceof NewVirtualFile)) {
            return NonProjectDirectoryInfo.NOT_SUPPORTED_VIRTUAL_FILE_IMPLEMENTATION;
        }

        return getRootIndex().getInfoForFile(file);
    }

    @Override
    @Nullable
    public ContentFolderTypeProvider getContentFolderType(@Nonnull VirtualFile file, @Nonnull DirectoryInfo info) {
        if (info.isInModuleSource(file)) {
            return getRootIndex().getContentFolderType(info);
        }
        return null;
    }

    @Override
    public String getPackageName(@Nonnull VirtualFile dir) {
        checkAvailability();
        if (!(dir instanceof NewVirtualFile)) {
            return null;
        }

        return getRootIndex().getPackageName(dir);
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public OrderEntry[] getOrderEntries(@Nonnull DirectoryInfo info) {
        checkAvailability();
        return getRootIndex().getOrderEntries(info);
    }

    private void checkAvailability() {
        if (myDisposed) {
            ProgressManager.checkCanceled();
            LOG.error("Directory index is already disposed for " + myProject);
        }
    }
}
