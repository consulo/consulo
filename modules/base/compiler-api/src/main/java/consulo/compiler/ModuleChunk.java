/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler;

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkType;
import consulo.module.Module;
import consulo.module.content.layer.OrderEnumerator;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.util.collection.Chunk;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.OrderedSet;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Eugene Zhuravlev
 * @since 2004-09-29
 */
public class ModuleChunk extends Chunk<Module> {
    private final CompileContextEx myContext;
    private final Map<Module, List<Path>> myModuleToFilesMap = new HashMap<>();
    private final Map<Path, Path> myTransformedToOriginalMap = new HashMap<>();
    private int mySourcesFilter = ALL_SOURCES;

    public ModuleChunk(CompileContextEx context, Chunk<Module> chunk, Map<Module, List<Path>> moduleToFilesMap) {
        super(chunk.getNodes());
        myContext = context;
        for (Module module : chunk.getNodes()) {
            List<Path> files = moduleToFilesMap.get(module);
            // Important!!! Collections in the myModuleToFilesMap must be modifiable copies of the corresponding collections
            // from the moduleToFilesMap. This is needed to support SourceTransforming compilers
            myModuleToFilesMap.put(module, files == null ? Collections.<Path>emptyList() : new ArrayList<>(files));
        }
    }

    public static final int SOURCES = 0x1;
    public static final int TEST_SOURCES = 0x2;
    public static final int ALL_SOURCES = SOURCES | TEST_SOURCES;

    public void setSourcesFilter(int filter) {
        mySourcesFilter = filter;
    }

    public int getSourcesFilter() {
        return mySourcesFilter;
    }

    public void substituteWithTransformedVersion(Module module, int fileIndex, Path transformedFile) {
        List<Path> moduleFiles = getFilesToCompile(module);
        Path currentFile = moduleFiles.get(fileIndex);
        moduleFiles.set(fileIndex, transformedFile);
        Path originalFile = myTransformedToOriginalMap.remove(currentFile);
        if (originalFile == null) {
            originalFile = currentFile;
        }
        myTransformedToOriginalMap.put(transformedFile, originalFile);
    }

    public Path getOriginalFile(Path file) {
        Path original = myTransformedToOriginalMap.get(file);
        return original != null ? original : file;
    }

    public List<Path> getFilesToCompile(Module forModule) {
        return myModuleToFilesMap.get(forModule);
    }

    public List<Path> getFilesToCompile() {
        if (getModuleCount() == 0) {
            return Collections.emptyList();
        }
        Set<Module> modules = getNodes();

        List<Path> filesToCompile = new ArrayList<>();
        for (Module module : modules) {
            List<Path> moduleCompilableFiles = getFilesToCompile(module);
            if (mySourcesFilter == ALL_SOURCES) {
                filesToCompile.addAll(moduleCompilableFiles);
            }
            else {
                for (Path file : moduleCompilableFiles) {
                    Path originalFile = myTransformedToOriginalMap.get(file);
                    if (originalFile == null) {
                        originalFile = file;
                    }
                    if (mySourcesFilter == TEST_SOURCES) {
                        if (myContext.isInTestSourceContent(originalFile)) {
                            filesToCompile.add(file);
                        }
                    }
                    else if (!myContext.isInTestSourceContent(originalFile)) {
                        filesToCompile.add(file);
                    }
                }
            }
        }
        return filesToCompile;
    }

    public Path[] getSourceRoots() {
        return getSourceRoots(mySourcesFilter);
    }

    public Path[] getSourceRoots(int sourcesFilter) {
        if (getModuleCount() == 0) {
            return new Path[0];
        }

        return filterRoots(getAllSourceRoots(), getNodes().iterator().next().getProject(), sourcesFilter);
    }

    public Path[] getSourceRoots(Module module) {
        if (!getNodes().contains(module)) {
            return new Path[0];
        }
        return filterRoots(myContext.getSourceRoots(module), module.getProject(), mySourcesFilter);
    }

    private Path[] filterRoots(Path[] roots, Project project, int sourcesFilter) {
        List<Path> filteredRoots = new ArrayList<>(roots.length);
        for (Path root : roots) {
            if (sourcesFilter != ALL_SOURCES) {
                if (myContext.isInTestSourceContent(root)) {
                    if ((sourcesFilter & TEST_SOURCES) == 0) {
                        continue;
                    }
                }
                else if ((sourcesFilter & SOURCES) == 0) {
                    continue;
                }
            }
            if (CompilerManager.getInstance(project).isExcludedFromCompilation(root)) {
                continue;
            }
            filteredRoots.add(root);
        }
        return filteredRoots.toArray(new Path[filteredRoots.size()]);
    }

    private Path[] getAllSourceRoots() {
        Set<Module> modules = getNodes();
        Set<Path> roots = new HashSet<>();
        for (Module module : modules) {
            ContainerUtil.addAll(roots, myContext.getSourceRoots(module));
        }
        return roots.toArray(new Path[roots.size()]);
    }

    public String getCompilationClasspath(SdkType sdkType) {
        OrderedSet<Path> cpFiles = getCompilationClasspathFiles(sdkType);
        return convertToStringPath(cpFiles);
    }

    public OrderedSet<Path> getCompilationClasspathFiles(SdkType sdkType) {
        return getCompilationClasspathFiles(sdkType, true);
    }

    public OrderedSet<Path> getCompilationClasspathFiles(SdkType sdkType, boolean exportedOnly) {
        Set<Module> modules = getNodes();

        OrderedSet<Path> cpFiles = new OrderedSet<>();
        for (Module module : modules) {
            addClassesUrls(cpFiles, orderEnumerator(module, exportedOnly, new AfterSdkOrderEntryCondition(sdkType)).classes().getUrls());
        }
        return cpFiles;
    }

    private OrderEnumerator orderEnumerator(Module module, boolean exportedOnly, Predicate<OrderEntry> condition) {
        OrderEnumerator enumerator = OrderEnumerator.orderEntries(module).compileOnly().satisfying(condition);
        if ((mySourcesFilter & TEST_SOURCES) == 0) {
            enumerator = enumerator.productionOnly();
        }
        enumerator = enumerator.recursively();
        return exportedOnly ? enumerator.exportedOnly() : enumerator;
    }

    public String getCompilationBootClasspath(SdkType sdkType) {
        return convertToStringPath(getCompilationBootClasspathFiles(sdkType));
    }

    public OrderedSet<Path> getCompilationBootClasspathFiles(SdkType sdkType) {
        return getCompilationBootClasspathFiles(sdkType, true);
    }

    public OrderedSet<Path> getCompilationBootClasspathFiles(SdkType sdkType, boolean exportedOnly) {
        Set<Module> modules = getNodes();
        OrderedSet<Path> cpFiles = new OrderedSet<>();
        OrderedSet<Path> jdkFiles = new OrderedSet<>();
        for (Module module : modules) {
            addClassesUrls(
                cpFiles,
                orderEnumerator(module, exportedOnly, new BeforeSdkOrderEntryCondition(sdkType, module)).classes().getUrls()
            );
            addClassesUrls(jdkFiles, OrderEnumerator.orderEntries(module).sdkOnly().classes().getUrls());
        }
        cpFiles.addAll(jdkFiles);
        return cpFiles;
    }

    private static void addClassesUrls(Collection<Path> container, String[] urls) {
        for (String url : urls) {
            String path = VirtualFileUtil.urlToPath(url);
            int archiveSeparatorIndex = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
            if (archiveSeparatorIndex >= 0) {
                path = path.substring(0, archiveSeparatorIndex);
            }
            container.add(Path.of(path));
        }
    }

    private static String convertToStringPath(OrderedSet<Path> cpFiles) {
        StringBuilder builder = new StringBuilder();
        for (Path cpFile : cpFiles) {
            if (builder.length() > 0) {
                builder.append(File.pathSeparatorChar);
            }
            builder.append(cpFile.toString());
        }
        return builder.toString();
    }

    public int getModuleCount() {
        return getNodes().size();
    }

    public Module[] getModules() {
        Set<Module> nodes = getNodes();
        return nodes.toArray(new Module[nodes.size()]);
    }

    public Module getModule() {
        return getNodes().iterator().next();
    }

    public Project getProject() {
        return myContext.getProject();
    }

    private static class BeforeSdkOrderEntryCondition implements Predicate<OrderEntry> {
        private boolean mySdkFound;
        private final SdkType mySdkType;
        private final Module myOwnerModule;

        private BeforeSdkOrderEntryCondition(SdkType sdkType, Module ownerModule) {
            mySdkType = sdkType;
            myOwnerModule = ownerModule;
        }

        @Override
        public boolean test(OrderEntry orderEntry) {
            if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry moduleExtensionWithSdkOrderEntry
                && myOwnerModule.equals(orderEntry.getOwnerModule())) {
                Sdk sdk = moduleExtensionWithSdkOrderEntry.getSdk();
                if (sdk == null || sdk.getSdkType() != mySdkType) {
                    return true;
                }

                mySdkFound = true;
            }
            return !mySdkFound;
        }
    }

    private static class AfterSdkOrderEntryCondition implements Predicate<OrderEntry> {
        private final SdkType mySdkType;
        private boolean mySdkFound;

        public AfterSdkOrderEntryCondition(SdkType sdkType) {
            mySdkType = sdkType;
        }

        @Override
        public boolean test(OrderEntry orderEntry) {
            if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry moduleExtensionWithSdkOrderEntry) {
                Sdk sdk = moduleExtensionWithSdkOrderEntry.getSdk();
                if (sdk == null || sdk.getSdkType() != mySdkType) {
                    return true;
                }

                mySdkFound = true;
                return false;
            }
            return mySdkFound;
        }
    }
}
