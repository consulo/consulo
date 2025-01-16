/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
import consulo.application.AccessRule;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.ContentIterator;
import consulo.content.OrderRootType;
import consulo.content.TestLikeContentFolderTypeProvider;
import consulo.module.Module;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.internal.FileIndexBase;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryType;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class ModuleFileIndexImpl extends FileIndexBase implements ModuleFileIndex {
    private final Module myModule;

    @Inject
    public ModuleFileIndexImpl(Module module,
                               Provider<DirectoryIndex> directoryIndexProvider,
                               FileTypeRegistry fileTypeManager) {
        super(directoryIndexProvider, fileTypeManager);
        myModule = module;
    }

    @Override
    public boolean iterateContent(@Nonnull ContentIterator processor, @Nullable VirtualFileFilter filter) {
        DirectoryIndex directoryIndex = myDirectoryIndexProvider.get();

        final Set<VirtualFile> contentRoots = AccessRule.read(() -> {
            if (myModule.isDisposed()) {
                return Collections.emptySet();
            }

            Set<VirtualFile> result = new LinkedHashSet<>();
            VirtualFile[][] allRoots = getModuleContentAndSourceRoots(myModule);
            for (VirtualFile[] roots : allRoots) {
                for (VirtualFile root : roots) {
                    DirectoryInfo info = getInfoForFileOrDirectory(root);
                    if (!info.isInProject(root)) {
                        continue;
                    }

                    VirtualFile parent = root.getParent();
                    if (parent != null) {
                        DirectoryInfo parentInfo = directoryIndex.getInfoForFile(parent);
                        if (parentInfo.isInProject(parent) && myModule.equals(parentInfo.getModule())) {
                            continue; // inner content - skip it
                        }
                    }
                    result.add(root);
                }
            }

            return result;
        });
        for (VirtualFile contentRoot : contentRoots) {
            if (!iterateContentUnderDirectory(contentRoot, processor, filter)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isInContent(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInProject(fileOrDir) && myModule.equals(info.getModule());
    }

    @Override
    public boolean isInSourceContent(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInModuleSource(fileOrDir) && myModule.equals(info.getModule());
    }

    @Override
    @Nonnull
    public List<OrderEntry> getOrderEntriesForFile(@Nonnull VirtualFile fileOrDir) {
        return findAllOrderEntriesWithOwnerModule(myModule, Arrays.asList(myDirectoryIndexProvider.get().getOrderEntries(getInfoForFileOrDirectory(fileOrDir))));
    }

    @Override
    public OrderEntry getOrderEntryForFile(@Nonnull VirtualFile fileOrDir) {
        return findOrderEntryWithOwnerModule(myModule, Arrays.asList(myDirectoryIndexProvider.get().getOrderEntries(getInfoForFileOrDirectory(fileOrDir))));
    }

    @Override
    public boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInModuleSource(fileOrDir)
            && myModule.equals(info.getModule())
            && myDirectoryIndexProvider.get().getContentFolderType(fileOrDir, info) instanceof TestLikeContentFolderTypeProvider;
    }

    @Nullable
    @Override
    public ContentFolderTypeProvider getContentFolderTypeForFile(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        if (info.isInModuleSource(fileOrDir) && myModule.equals(info.getModule())) {
            return myDirectoryIndexProvider.get().getContentFolderType(fileOrDir, info);
        }
        return null;
    }

    @Override
    protected boolean isScopeDisposed() {
        return myModule.isDisposed();
    }

    @Nullable
    public static OrderEntry findOrderEntryWithOwnerModule(@Nonnull Module ownerModule, @Nonnull List<OrderEntry> orderEntries) {
        if (orderEntries.size() < 10) {
            for (OrderEntry orderEntry : orderEntries) {
                if (orderEntry.getOwnerModule() == ownerModule) {
                    return orderEntry;
                }
            }
            return null;
        }
        int index = Collections.binarySearch(orderEntries, new FakeOrderEntry(ownerModule), RootIndexImpl.BY_OWNER_MODULE);
        return index < 0 ? null : orderEntries.get(index);
    }

    @Nonnull
    private static List<OrderEntry> findAllOrderEntriesWithOwnerModule(@Nonnull Module ownerModule, @Nonnull List<OrderEntry> entries) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        if (entries.size() == 1) {
            OrderEntry entry = entries.get(0);
            return entry.getOwnerModule() == ownerModule ? new ArrayList<>(entries) : Collections.emptyList();
        }
        int index = Collections.binarySearch(entries, new FakeOrderEntry(ownerModule), RootIndexImpl.BY_OWNER_MODULE);
        if (index < 0) {
            return Collections.emptyList();
        }
        int firstIndex = index;
        while (firstIndex - 1 >= 0 && entries.get(firstIndex - 1).getOwnerModule() == ownerModule) {
            firstIndex--;
        }
        int lastIndex = index + 1;
        while (lastIndex < entries.size() && entries.get(lastIndex).getOwnerModule() == ownerModule) {
            lastIndex++;
        }
        return new ArrayList<>(entries.subList(firstIndex, lastIndex));
    }

    private static class FakeOrderEntry implements OrderEntry {
        private final Module myOwnerModule;

        FakeOrderEntry(Module ownerModule) {
            myOwnerModule = ownerModule;
        }

        @Nonnull
        @Override
        public OrderEntryType<?> getType() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public VirtualFile[] getFiles(@Nonnull OrderRootType type) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public String[] getUrls(@Nonnull OrderRootType rootType) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public String getPresentableName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Module getOwnerModule() {
            return myOwnerModule;
        }

        @Nonnull
        @Override
        public ModuleRootLayer getModuleRootLayer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R accept(@Nonnull RootPolicy<R> policy, @Nullable R initialValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEquivalentTo(@Nonnull OrderEntry other) {
            return false;
        }

        @Override
        public int compareTo(@Nonnull OrderEntry o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSynthetic() {
            throw new UnsupportedOperationException();
        }
    }
}
