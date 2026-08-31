// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.language.index.impl.internal.roots.IndexableFilesContributor;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ExtensionImpl
public class DefaultProjectIndexableFilesContributor implements IndexableFilesContributor {
    @Override
    public List<IndexableFilesIterator> getIndexableFiles(Project project) {
        Set<Library> seenLibraries = new HashSet<>();
        Set<Sdk> seenSdks = new HashSet<>();
        Set<Object> seenTrackedEntries = new HashSet<>();
        Module[] modules = ModuleManager.getInstance(project).getSortedModules();

        List<IndexableFilesIterator> providers = new ArrayList<>();
        for (Module module : modules) {
            providers.addAll(ModuleIndexableFilesIteratorImpl.getModuleIterators(module));

            OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
            for (OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
                    Library library = libraryOrderEntry.getLibrary();
                    if (library != null && seenLibraries.add(library)) {
                        providers.add(new LibraryIndexableFilesIteratorImpl(library));
                    }
                }
                else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
                    Sdk sdk = sdkOrderEntry.getSdk();
                    if (sdk != null && seenSdks.add(sdk)) {
                        providers.add(new SdkIndexableFilesIteratorImpl(sdk));
                    }
                }
                else if (orderEntry instanceof OrderEntryWithTracking tracking) {
                    Object equalObject = tracking.getEqualObject();
                    if (equalObject == null || seenTrackedEntries.add(equalObject)) {
                        providers.add(new CustomOrderEntryIndexableFilesIterator(orderEntry));
                    }
                }
            }
        }
        return providers;
    }

    @Override
    public Predicate<VirtualFile> getOwnFilePredicate(Project project) {
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);

        return file -> {
            if (projectFileIndex.isInContent(file) || projectFileIndex.isInLibrary(file)) {
                return !FileTypeRegistry.getInstance().isFileIgnored(file);
            }
            return false;
        };
    }
}
