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
package consulo.module.content.impl.internal.root;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.component.messagebus.MessageBusConnection;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.internal.WorkspaceExcludeFileSet;
import consulo.module.content.internal.WorkspaceFileIndexEx;
import consulo.module.content.internal.WorkspaceFileKindMask;
import consulo.module.content.internal.WorkspaceFileSets;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.content.layer.orderEntry.CustomOrderEntry;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.module.event.ModuleListener;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.url.VirtualFileUrl;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@ServiceImpl
public class WorkspaceFileIndexImpl implements WorkspaceFileIndexEx {
    private static final int CONTENT_EXCLUDED_KINDS = WorkspaceFileKindMask.CONTENT | WorkspaceFileKindMask.CONTENT_NON_INDEXABLE;
    private static final int EXTERNAL_EXCLUDED_KINDS = WorkspaceFileKindMask.EXTERNAL | WorkspaceFileKindMask.EXTERNAL_NON_INDEXABLE;

    private final Project myProject;
    private final VirtualFileUrlManager myUrlManager;

    private final AtomicLong myModificationCount = new AtomicLong();
    private volatile @Nullable Map<VirtualFileUrl, WorkspaceFileSets> myFileSets;

    @Inject
    public WorkspaceFileIndexImpl(Project project, VirtualFileUrlManager urlManager) {
        myProject = project;
        myUrlManager = urlManager;

        if (project.isDefault()) {
            return;
        }

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(ModuleListener.class, new ModuleListener() {
            @Override
            public void moduleAdded(Project project, Module module) {
                rebuild();
            }

            @Override
            public void beforeModuleRemoved(Project project, Module module) {
                rebuild();
            }

            @Override
            public void moduleRemoved(Project project, Module module) {
                rebuild();
            }

            @Override
            public void modulesRenamed(Project project, Map<Module, String> modulesWithOldName) {
                rebuild();
            }
        });
        connection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                rebuild();
            }
        });

        getFileSets();
    }

    private void rebuild() {
        myModificationCount.incrementAndGet();
        myFileSets = null;
        if (!myProject.isDisposed()) {
            getFileSets();
        }
    }

    @Override
    public boolean isUrlIndexableRecursiveFileSetRoot(String url) {
        VirtualFileUrl virtualFileUrl = myUrlManager.get(url);
        if (virtualFileUrl == null) {
            return false;
        }
        WorkspaceFileSets fileSets = getFileSetsAt(virtualFileUrl);
        if (!fileSets.excludes().isEmpty()) {
            return false;
        }
        return (fileSets.maskRecursive() & ~WorkspaceFileKindMask.CONTENT_NON_INDEXABLE) != 0;
    }

    @Override
    public WorkspaceFileSets getFileSetsAt(VirtualFileUrl url) {
        if (myProject.isDefault() || myProject.isDisposed()) {
            return WorkspaceFileSets.EMPTY;
        }
        return getFileSets().getOrDefault(url, WorkspaceFileSets.EMPTY);
    }

    private Map<VirtualFileUrl, WorkspaceFileSets> getFileSets() {
        Map<VirtualFileUrl, WorkspaceFileSets> fileSets = myFileSets;
        if (fileSets != null) {
            return fileSets;
        }
        long modificationCount = myModificationCount.get();
        fileSets = ReadAction.compute(this::buildFileSets);
        if (modificationCount == myModificationCount.get()) {
            myFileSets = fileSets;
        }
        return fileSets;
    }

    private Map<VirtualFileUrl, WorkspaceFileSets> buildFileSets() {
        Builder builder = new Builder();
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

            for (ContentEntry contentEntry : moduleRootManager.getContentEntries()) {
                builder.registerFileSet(contentEntry.getUrl(), WorkspaceFileKindMask.CONTENT);

                for (String excludedUrl : contentEntry.getFolderUrls(ExcludedContentFolderTypeProvider.onlyExcluded())) {
                    builder.registerExclude(excludedUrl, new ExcludedFileSetByFileKind(CONTENT_EXCLUDED_KINDS));
                }

                for (String folderUrl : contentEntry.getFolderUrls(ExcludedContentFolderTypeProvider.allExceptExcluded())) {
                    builder.registerFileSet(folderUrl, WorkspaceFileKindMask.CONTENT);
                }
            }

            for (OrderEntry orderEntry : moduleRootManager.getOrderEntries()) {
                if (!(orderEntry instanceof OrderEntryWithTracking)) {
                    continue;
                }

                for (String classUrl : orderEntry.getUrls(BinariesOrderRootType.ID)) {
                    builder.registerFileSet(classUrl, WorkspaceFileKindMask.EXTERNAL_BINARY);
                }
                for (String sourceUrl : orderEntry.getUrls(SourcesOrderRootType.ID)) {
                    builder.registerFileSet(sourceUrl, WorkspaceFileKindMask.EXTERNAL_SOURCE);
                }

                if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
                    Library library = libraryOrderEntry.getLibrary();
                    if (library != null) {
                        for (String excludedUrl : library.getExcludedRootUrls()) {
                            builder.registerExclude(excludedUrl, new ExcludedFileSetByFileKind(EXTERNAL_EXCLUDED_KINDS));
                        }
                    }
                }
                else if (orderEntry instanceof CustomOrderEntry<?> customOrderEntry) {
                    for (VirtualFile excludedRoot : customOrderEntry.getModel().getExcludedRoots()) {
                        builder.registerExclude(excludedRoot.getUrl(), new ExcludedFileSetByFileKind(EXTERNAL_EXCLUDED_KINDS));
                    }
                }
            }
        }

        myProject.getExtensionPoint(DirectoryIndexExcludePolicy.class).forEach(policy -> {
            for (VirtualFile excludedRoot : policy.getExcludeRootsForProject()) {
                builder.registerExclude(excludedRoot.getUrl(), new ExcludedFileSetByFileKind(WorkspaceFileKindMask.ALL));
            }
        });
        return builder.build();
    }

    private final class Builder {
        private final Map<VirtualFileUrl, int[]> myMasks = new HashMap<>();
        private final Map<VirtualFileUrl, List<WorkspaceExcludeFileSet>> myExcludes = new HashMap<>();

        void registerFileSet(String url, int kindMask) {
            int[] masks = myMasks.computeIfAbsent(myUrlManager.storeAndGet(url), it -> new int[2]);
            masks[0] |= kindMask;
            masks[1] |= kindMask;
        }

        void registerExclude(String url, WorkspaceExcludeFileSet exclude) {
            myExcludes.computeIfAbsent(myUrlManager.storeAndGet(url), it -> new ArrayList<>()).add(exclude);
        }

        Map<VirtualFileUrl, WorkspaceFileSets> build() {
            Map<VirtualFileUrl, WorkspaceFileSets> result = new HashMap<>();
            for (Map.Entry<VirtualFileUrl, int[]> entry : myMasks.entrySet()) {
                int[] masks = entry.getValue();
                List<WorkspaceExcludeFileSet> excludes = myExcludes.getOrDefault(entry.getKey(), List.of());
                result.put(entry.getKey(), new WorkspaceFileSets(masks[0], masks[1], List.copyOf(excludes)));
            }
            for (Map.Entry<VirtualFileUrl, List<WorkspaceExcludeFileSet>> entry : myExcludes.entrySet()) {
                result.putIfAbsent(entry.getKey(), new WorkspaceFileSets(0, 0, List.copyOf(entry.getValue())));
            }
            return Map.copyOf(result);
        }
    }
}
