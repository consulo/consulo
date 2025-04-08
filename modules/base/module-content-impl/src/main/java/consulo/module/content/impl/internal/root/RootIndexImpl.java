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
package consulo.module.content.impl.internal.root;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.query.CollectionQuery;
import consulo.application.util.query.EmptyQuery;
import consulo.application.util.query.Query;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.module.content.layer.OrderEnumerator;
import consulo.module.content.layer.orderEntry.*;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class RootIndexImpl implements RootIndex {
    public static final Comparator<OrderEntry> BY_OWNER_MODULE = (o1, o2) -> {
        String name1 = o1.getOwnerModule().getName();
        String name2 = o2.getOwnerModule().getName();
        return name1.compareTo(name2);
    };

    public interface InfoCache {
        @Nullable
        DirectoryInfo getCachedInfo(@Nonnull VirtualFile dir);

        void cacheInfo(@Nonnull VirtualFile dir, @Nonnull DirectoryInfo info);
    }

    private final Set<VirtualFile> myProjectExcludedRoots = new HashSet<>();
    private final MultiMap<String, VirtualFile> myPackagePrefixRoots = new MultiMap<>() {
        @Override
        protected Collection<VirtualFile> createCollection() {
            return new LinkedHashSet<>();
        }
    };

    private final Map<String, List<VirtualFile>> myDirectoriesByPackageNameCache = ContainerUtil.newConcurrentMap();
    private final Set<String> myNonExistentPackages = ContainerUtil.newConcurrentSet();
    private final InfoCache myInfoCache;
    @Nonnull
    private final Project myProject;
    private volatile Map<VirtualFile, OrderEntry[]> myOrderEntries;

    @RequiredReadAction
    public RootIndexImpl(@Nonnull Project project, @Nonnull InfoCache cache) {
        myProject = project;
        myInfoCache = cache;

        RootInfo info = buildRootInfo(project);
        Set<VirtualFile> allRoots = info.getAllRoots();
        for (VirtualFile root : allRoots) {
            List<VirtualFile> hierarchy = getHierarchy(root, allRoots, info);
            Pair<DirectoryInfo, String> pair =
                hierarchy != null ? calcDirectoryInfo(root, hierarchy, info) : new Pair<>(NonProjectDirectoryInfo.IGNORED, null);
            cacheInfos(root, root, pair.first);
            myPackagePrefixRoots.putValue(pair.second, root);
            if (info.shouldMarkAsProjectExcluded(root, hierarchy)) {
                myProjectExcludedRoots.add(root);
            }
        }
    }

    public void onLowMemory() {
        myNonExistentPackages.clear();
    }

    @Nonnull
    @RequiredReadAction
    private RootInfo buildRootInfo(@Nonnull Project project) {
        RootInfo info = new RootInfo();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

            for (VirtualFile contentRoot : moduleRootManager.getContentRoots()) {
                if (!info.contentRootOf.containsKey(contentRoot)) {
                    info.contentRootOf.put(contentRoot, module);
                }
            }

            for (ContentEntry contentEntry : moduleRootManager.getContentEntries()) {
                for (VirtualFile excludeRoot : contentEntry.getFolderFiles(ExcludedContentFolderTypeProvider.onlyExcluded())) {
                    info.excludedFromModule.put(excludeRoot, module);
                }

                // Init module sources
                for (ContentFolder sourceFolder : contentEntry.getFolders(ExcludedContentFolderTypeProvider.allExceptExcluded())) {
                    VirtualFile sourceFolderRoot = sourceFolder.getFile();
                    if (sourceFolderRoot != null) {
                        info.contentFolders.put(sourceFolderRoot, sourceFolder);
                        info.classAndSourceRoots.add(sourceFolderRoot);
                        info.sourceRootOf.putValue(sourceFolderRoot, module);
                        info.packagePrefix.put(sourceFolderRoot, "");
                    }
                }
            }

            for (OrderEntry orderEntry : moduleRootManager.getOrderEntries()) {
                if (orderEntry instanceof OrderEntryWithTracking) {
                    VirtualFile[] sourceRoots = orderEntry.getFiles(SourcesOrderRootType.getInstance());
                    VirtualFile[] classRoots = orderEntry.getFiles(BinariesOrderRootType.getInstance());

                    // Init library sources
                    for (VirtualFile sourceRoot : sourceRoots) {
                        info.classAndSourceRoots.add(sourceRoot);
                        info.libraryOrSdkSources.add(sourceRoot);
                        info.packagePrefix.put(sourceRoot, "");
                    }

                    // init library classes
                    for (VirtualFile classRoot : classRoots) {
                        info.classAndSourceRoots.add(classRoot);
                        info.libraryOrSdkClasses.add(classRoot);
                        info.packagePrefix.put(classRoot, "");
                    }

                    if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
                        Library library = libraryOrderEntry.getLibrary();
                        if (library != null) {
                            for (VirtualFile root : library.getExcludedRoots()) {
                                info.excludedFromLibraries.putValue(root, library);
                            }
                            for (VirtualFile root : sourceRoots) {
                                info.sourceOfLibraries.putValue(root, library);
                            }
                            for (VirtualFile root : classRoots) {
                                info.classOfLibraries.putValue(root, library);
                            }
                        }
                    }
                }
            }
        }

        for (DirectoryIndexExcludePolicy policy : DirectoryIndexExcludePolicy.EP_NAME.getExtensionList(project)) {
            Collections.addAll(info.excludedFromProject, policy.getExcludeRootsForProject());
        }
        return info;
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public OrderEntry[] getOrderEntries(@Nonnull DirectoryInfo info) {
        if (!(info instanceof DirectoryInfoImpl)) {
            return OrderEntry.EMPTY_ARRAY;
        }
        OrderEntry[] entries = getOrderEntries().get(((DirectoryInfoImpl)info).getRoot());
        return entries == null ? OrderEntry.EMPTY_ARRAY : entries;
    }

    @Nonnull
    @RequiredReadAction
    private Map<VirtualFile, OrderEntry[]> getOrderEntries() {
        Map<VirtualFile, OrderEntry[]> result = myOrderEntries;
        if (result != null) {
            return result;
        }

        MultiMap<VirtualFile, OrderEntry> libClassRootEntries = MultiMap.createSmart();
        MultiMap<VirtualFile, OrderEntry> libSourceRootEntries = MultiMap.createSmart();
        MultiMap<VirtualFile, OrderEntry> depEntries = MultiMap.createSmart();

        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            for (OrderEntry orderEntry : moduleRootManager.getOrderEntries()) {
                if (orderEntry instanceof ModuleOrderEntry moduleOrderEntry) {
                    Module depModule = moduleOrderEntry.getModule();
                    if (depModule != null) {
                        VirtualFile[] importedClassRoots =
                            OrderEnumerator.orderEntries(depModule).exportedOnly().recursively().classes().usingCache().getRoots();
                        for (VirtualFile importedClassRoot : importedClassRoots) {
                            depEntries.putValue(importedClassRoot, orderEntry);
                        }
                    }
                    for (VirtualFile sourceRoot : orderEntry.getFiles(SourcesOrderRootType.getInstance())) {
                        depEntries.putValue(sourceRoot, orderEntry);
                    }
                }
                else if (orderEntry instanceof OrderEntryWithTracking) {
                    for (VirtualFile sourceRoot : orderEntry.getFiles(SourcesOrderRootType.getInstance())) {
                        libSourceRootEntries.putValue(sourceRoot, orderEntry);
                    }
                    for (VirtualFile classRoot : orderEntry.getFiles(BinariesOrderRootType.getInstance())) {
                        libClassRootEntries.putValue(classRoot, orderEntry);
                    }
                }
            }
        }

        RootInfo rootInfo = buildRootInfo(myProject);
        result = new HashMap<>();
        Set<VirtualFile> allRoots = rootInfo.getAllRoots();
        for (VirtualFile file : allRoots) {
            List<VirtualFile> hierarchy = getHierarchy(file, allRoots, rootInfo);
            result.put(
                file,
                hierarchy == null ? OrderEntry.EMPTY_ARRAY
                    : calcOrderEntries(rootInfo, depEntries, libClassRootEntries, libSourceRootEntries, hierarchy)
            );
        }
        myOrderEntries = result;
        return result;
    }

    private static OrderEntry[] calcOrderEntries(
        @Nonnull RootInfo info,
        @Nonnull MultiMap<VirtualFile, OrderEntry> depEntries,
        @Nonnull MultiMap<VirtualFile, OrderEntry> libClassRootEntries,
        @Nonnull MultiMap<VirtualFile, OrderEntry> libSourceRootEntries,
        @Nonnull List<VirtualFile> hierarchy
    ) {
        @Nullable VirtualFile libraryClassRoot = info.findLibraryRootInfo(hierarchy, false);
        @Nullable VirtualFile librarySourceRoot = info.findLibraryRootInfo(hierarchy, true);
        Set<OrderEntry> orderEntries = new LinkedHashSet<>();
        orderEntries.addAll(info.getLibraryOrderEntries(
            hierarchy,
            libraryClassRoot,
            librarySourceRoot,
            libClassRootEntries,
            libSourceRootEntries
        ));
        for (VirtualFile root : hierarchy) {
            orderEntries.addAll(depEntries.get(root));
        }
        VirtualFile moduleContentRoot = info.findModuleRootInfo(hierarchy);
        if (moduleContentRoot != null) {
            ContainerUtil.addIfNotNull(orderEntries, info.getModuleSourceEntry(hierarchy, moduleContentRoot, libClassRootEntries));
        }
        if (orderEntries.isEmpty()) {
            return null;
        }

        OrderEntry[] array = orderEntries.toArray(new OrderEntry[orderEntries.size()]);
        Arrays.sort(array, RootIndexImpl.BY_OWNER_MODULE);
        return array;
    }


    public void checkConsistency() {
        for (VirtualFile file : myProjectExcludedRoots) {
            assert file.exists() : file.getPath() + " does not exist";
        }

        for (VirtualFile file : myPackagePrefixRoots.values()) {
            assert file.exists() : file.getPath() + " does not exist";
        }
    }

    @Override
    @Nonnull
    public DirectoryInfo getInfoForFile(@Nonnull VirtualFile file) {
        if (!file.isValid()) {
            return NonProjectDirectoryInfo.INVALID;
        }
        VirtualFile dir;
        if (!file.isDirectory()) {
            DirectoryInfo info = myInfoCache.getCachedInfo(file);
            if (info != null) {
                return info;
            }
            if (isIgnored(file)) {
                return NonProjectDirectoryInfo.IGNORED;
            }
            dir = file.getParent();
        }
        else {
            dir = file;
        }

        int count = 0;
        for (VirtualFile root = dir; root != null; root = root.getParent()) {
            if (++count > 1000) {
                throw new IllegalStateException("Possible loop in tree, started at " + dir.getName());
            }
            DirectoryInfo info = myInfoCache.getCachedInfo(root);
            if (info != null) {
                if (!dir.equals(root)) {
                    cacheInfos(dir, root, info);
                }
                return info;
            }

            if (isIgnored(root)) {
                return cacheInfos(dir, root, NonProjectDirectoryInfo.IGNORED);
            }
        }

        return cacheInfos(dir, null, NonProjectDirectoryInfo.NOT_UNDER_PROJECT_ROOTS);
    }

    @Nonnull
    private DirectoryInfo cacheInfos(VirtualFile dir, @Nullable VirtualFile stopAt, @Nonnull DirectoryInfo info) {
        while (dir != null) {
            myInfoCache.cacheInfo(dir, info);
            if (dir.equals(stopAt)) {
                break;
            }
            dir = dir.getParent();
        }
        return info;
    }

    @Override
    @Nonnull
    public Query<VirtualFile> getDirectoriesByPackageName(@Nonnull String packageName, boolean includeLibrarySources) {
        List<VirtualFile> result = myDirectoriesByPackageNameCache.get(packageName);
        if (result == null) {
            if (myNonExistentPackages.contains(packageName)) {
                return EmptyQuery.getEmptyQuery();
            }

            result = new SmartList<>();

            if (StringUtil.isNotEmpty(packageName) && !StringUtil.startsWithChar(packageName, '.')) {
                int i = packageName.lastIndexOf('.');
                while (true) {
                    String shortName = packageName.substring(i + 1);
                    String parentPackage = i > 0 ? packageName.substring(0, i) : "";
                    for (VirtualFile parentDir : getDirectoriesByPackageName(parentPackage, true)) {
                        VirtualFile child = !parentDir.isValid() ? null : parentDir.findChild(shortName);
                        if (child != null && child.isDirectory() && getInfoForFile(child).isInProject(child) && packageName.equals(
                            getPackageName(child))) {
                            result.add(child);
                        }
                    }
                    if (i < 0) {
                        break;
                    }
                    i = packageName.lastIndexOf('.', i - 1);
                }
            }

            for (VirtualFile file : myPackagePrefixRoots.get(packageName)) {
                if (file.isDirectory()) {
                    result.add(file);
                }
            }

            if (!result.isEmpty()) {
                myDirectoriesByPackageNameCache.put(packageName, result);
            }
            else {
                myNonExistentPackages.add(packageName);
            }
        }

        if (!includeLibrarySources) {
            result = ContainerUtil.filter(result, file -> {
                DirectoryInfo info = getInfoForFile(file);
                return info.isInProject(file) && (!info.isInLibrarySource(file) || info.isInModuleSource(file) || info.hasLibraryClassRoot());
            });
        }
        return new CollectionQuery<>(result);
    }

    @Override
    @Nullable
    public String getPackageName(@Nonnull VirtualFile dir) {
        if (dir.isDirectory()) {
            if (isIgnored(dir)) {
                return null;
            }

            for (Map.Entry<String, Collection<VirtualFile>> entry : myPackagePrefixRoots.entrySet()) {
                if (entry.getValue().contains(dir)) {
                    return entry.getKey();
                }
            }

            VirtualFile parent = dir.getParent();
            if (parent != null) {
                return getPackageNameForSubdir(getPackageName(parent), dir.getName());
            }
        }

        return null;
    }

    @Nullable
    protected static String getPackageNameForSubdir(String parentPackageName, @Nonnull String subdirName) {
        if (parentPackageName == null) {
            return null;
        }
        return parentPackageName.isEmpty() ? subdirName : parentPackageName + "." + subdirName;
    }

    @Override
    @Nullable
    public ContentFolderTypeProvider getContentFolderType(@Nonnull DirectoryInfo directoryInfo) {
        return directoryInfo.getSourceRootTypeId();
    }

    boolean resetOnEvents(@Nonnull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null || file.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static List<VirtualFile> getHierarchy(VirtualFile dir, @Nonnull Set<VirtualFile> allRoots, @Nonnull RootInfo info) {
        List<VirtualFile> hierarchy = new ArrayList<>();
        boolean hasContentRoots = false;
        while (dir != null) {
            hasContentRoots |= info.contentRootOf.get(dir) != null;
            if (!hasContentRoots && isIgnored(dir)) {
                return null;
            }
            if (allRoots.contains(dir)) {
                hierarchy.add(dir);
            }
            dir = dir.getParent();
        }
        return hierarchy;
    }

    private static boolean isIgnored(@Nonnull VirtualFile dir) {
        return FileTypeRegistry.getInstance().isFileIgnored(dir);
    }

    private static class RootInfo {
        // getDirectoriesByPackageName used to be in this order, some clients might rely on that
        @Nonnull
        final LinkedHashSet<VirtualFile> classAndSourceRoots = new LinkedHashSet<>();

        @Nonnull
        final Set<VirtualFile> libraryOrSdkSources = new HashSet<>();
        @Nonnull
        final Set<VirtualFile> libraryOrSdkClasses = new HashSet<>();
        @Nonnull
        final Map<VirtualFile, Module> contentRootOf = new HashMap<>();
        @Nonnull
        final MultiMap<VirtualFile, Module> sourceRootOf = MultiMap.createSet();
        @Nonnull
        final Map<VirtualFile, ContentFolder> contentFolders = new HashMap<>();
        @Nonnull
        final MultiMap<VirtualFile, Library> excludedFromLibraries = MultiMap.createSmart();
        @Nonnull
        final MultiMap<VirtualFile, Library> classOfLibraries = MultiMap.createSmart();
        @Nonnull
        final MultiMap<VirtualFile, Library> sourceOfLibraries = MultiMap.createSmart();
        @Nonnull
        final Set<VirtualFile> excludedFromProject = new HashSet<>();
        @Nonnull
        final Map<VirtualFile, Module> excludedFromModule = new HashMap<>();
        @Nonnull
        final Map<VirtualFile, String> packagePrefix = new HashMap<>();

        @Nonnull
        Set<VirtualFile> getAllRoots() {
            LinkedHashSet<VirtualFile> result = new LinkedHashSet<>();
            result.addAll(classAndSourceRoots);
            result.addAll(contentRootOf.keySet());
            result.addAll(excludedFromLibraries.keySet());
            result.addAll(excludedFromModule.keySet());
            result.addAll(excludedFromProject);
            return result;
        }

        private boolean shouldMarkAsProjectExcluded(@Nonnull VirtualFile root, @Nullable List<VirtualFile> hierarchy) {
            return hierarchy != null
                && (excludedFromProject.contains(root) || excludedFromModule.containsKey(root))
                && ContainerUtil.find(hierarchy, contentRootOf::containsKey) == null;
        }

        @Nullable
        private VirtualFile findModuleRootInfo(@Nonnull List<VirtualFile> hierarchy) {
            for (VirtualFile root : hierarchy) {
                ContentFolder contentFolder = contentFolders.get(root);
                // if we found content folder - try search content root
                if (contentFolder != null) {
                    return findNearestContentRootForExcluded(hierarchy);
                }
            }

            for (VirtualFile root : hierarchy) {
                Module module = contentRootOf.get(root);
                Module excludedFrom = excludedFromModule.get(root);
                if (module != null && excludedFrom != module) {
                    return root;
                }
                if (excludedFrom != null || excludedFromProject.contains(root)) {
                    return null;
                }
            }
            return null;
        }

        @Nullable
        private VirtualFile findNearestContentRootForExcluded(@Nonnull List<VirtualFile> hierarchy) {
            for (VirtualFile root : hierarchy) {
                if (contentRootOf.containsKey(root)) {
                    return root;
                }
            }
            return null;
        }

        @Nullable
        private VirtualFile findLibraryRootInfo(@Nonnull List<VirtualFile> hierarchy, boolean source) {
            Set<Library> librariesToIgnore = new HashSet<>();
            for (VirtualFile root : hierarchy) {
                librariesToIgnore.addAll(excludedFromLibraries.get(root));
                if (source && libraryOrSdkSources.contains(root) && (!sourceOfLibraries.containsKey(root)
                    || !librariesToIgnore.containsAll(sourceOfLibraries.get(root)))) {
                    return root;
                }
                else if (!source && libraryOrSdkClasses.contains(root) && (!classOfLibraries.containsKey(root)
                    || !librariesToIgnore.containsAll(classOfLibraries.get(root)))) {
                    return root;
                }
            }
            return null;
        }

        private String calcPackagePrefix(
            @Nonnull VirtualFile root,
            @Nonnull List<VirtualFile> hierarchy,
            VirtualFile moduleContentRoot,
            VirtualFile libraryClassRoot,
            VirtualFile librarySourceRoot
        ) {
            VirtualFile packageRoot = findPackageRootInfo(hierarchy, moduleContentRoot, libraryClassRoot, librarySourceRoot);
            String prefix = packagePrefix.get(packageRoot);
            if (prefix != null && packageRoot != root) {
                assert packageRoot != null;
                String relative = VirtualFileUtil.getRelativePath(root, packageRoot, '.');
                prefix = StringUtil.isEmpty(prefix) ? relative : prefix + '.' + relative;
            }
            return prefix;
        }

        @Nullable
        private VirtualFile findPackageRootInfo(
            @Nonnull List<VirtualFile> hierarchy,
            VirtualFile moduleContentRoot,
            VirtualFile libraryClassRoot,
            VirtualFile librarySourceRoot
        ) {
            for (VirtualFile root : hierarchy) {
                if (moduleContentRoot != null && sourceRootOf.get(root)
                    .contains(contentRootOf.get(moduleContentRoot)) && librarySourceRoot == null) {
                    return root;
                }
                if (root == libraryClassRoot || root == librarySourceRoot) {
                    return root;
                }
                if (root == moduleContentRoot && !sourceRootOf.containsKey(root) && librarySourceRoot == null && libraryClassRoot == null) {
                    return null;
                }
            }
            return null;
        }

        @Nonnull
        private LinkedHashSet<OrderEntry> getLibraryOrderEntries(
            @Nonnull List<VirtualFile> hierarchy,
            @Nullable VirtualFile libraryClassRoot,
            @Nullable VirtualFile librarySourceRoot,
            @Nonnull MultiMap<VirtualFile, OrderEntry> libClassRootEntries,
            @Nonnull MultiMap<VirtualFile, OrderEntry> libSourceRootEntries
        ) {
            LinkedHashSet<OrderEntry> orderEntries = new LinkedHashSet<>();
            for (VirtualFile root : hierarchy) {
                if (root == libraryClassRoot && !sourceRootOf.containsKey(root)) {
                    orderEntries.addAll(libClassRootEntries.get(root));
                }
                if (root == librarySourceRoot && libraryClassRoot == null) {
                    orderEntries.addAll(libSourceRootEntries.get(root));
                }
                if (libClassRootEntries.containsKey(root) || sourceRootOf.containsKey(root) && librarySourceRoot == null) {
                    break;
                }
            }
            return orderEntries;
        }

        @Nullable
        private ModuleSourceOrderEntry getModuleSourceEntry(
            @Nonnull List<VirtualFile> hierarchy,
            @Nonnull VirtualFile moduleContentRoot,
            @Nonnull MultiMap<VirtualFile, OrderEntry> libClassRootEntries
        ) {
            Module module = contentRootOf.get(moduleContentRoot);
            for (VirtualFile root : hierarchy) {
                if (sourceRootOf.get(root).contains(module)) {
                    return ContainerUtil.findInstance(
                        ModuleRootManager.getInstance(module).getOrderEntries(),
                        ModuleSourceOrderEntry.class
                    );
                }
                if (libClassRootEntries.containsKey(root)) {
                    return null;
                }
            }
            return null;
        }
    }

    @Nonnull
    private Pair<DirectoryInfo, String> calcDirectoryInfo(
        @Nonnull VirtualFile root,
        @Nonnull List<VirtualFile> hierarchy,
        @Nonnull RootInfo info
    ) {
        VirtualFile moduleContentRoot = info.findModuleRootInfo(hierarchy);
        VirtualFile libraryClassRoot = info.findLibraryRootInfo(hierarchy, false);
        VirtualFile librarySourceRoot = info.findLibraryRootInfo(hierarchy, true);
        boolean inProject = moduleContentRoot != null || libraryClassRoot != null || librarySourceRoot != null;
        VirtualFile nearestContentRoot;
        if (inProject) {
            nearestContentRoot = moduleContentRoot;
        }
        else {
            nearestContentRoot = info.findNearestContentRootForExcluded(hierarchy);
            if (nearestContentRoot == null) {
                return Pair.create(NonProjectDirectoryInfo.EXCLUDED, null);
            }
        }

        VirtualFile sourceRoot = info.findPackageRootInfo(hierarchy, moduleContentRoot, null, librarySourceRoot);

        VirtualFile moduleSourceRoot = info.findPackageRootInfo(hierarchy, moduleContentRoot, null, null);
        boolean inModuleSources = moduleSourceRoot != null;
        boolean inLibrarySource = librarySourceRoot != null;
        ContentFolder folder = moduleSourceRoot != null ? info.contentFolders.get(moduleSourceRoot) : null;
        ContentFolderTypeProvider typeId = folder == null ? null : folder.getType();

        Module module = info.contentRootOf.get(nearestContentRoot);
        DirectoryInfo directoryInfo = new DirectoryInfoImpl(
            root,
            module,
            nearestContentRoot,
            sourceRoot,
            folder,
            libraryClassRoot,
            inModuleSources,
            inLibrarySource,
            !inProject,
            typeId,
            null
        );

        String packagePrefix = info.calcPackagePrefix(root, hierarchy, moduleContentRoot, libraryClassRoot, librarySourceRoot);

        return Pair.create(directoryInfo, packagePrefix);
    }
}