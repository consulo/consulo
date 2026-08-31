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
package consulo.compiler.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.compiler.*;
import consulo.compiler.impl.internal.scope.CompositeScope;
import consulo.compiler.scope.CompileScope;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.compiler.impl.internal.state.OutputSourceInfo;
import consulo.compiler.impl.internal.state.ProjectCompilerState;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.util.collection.Sets;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @since 2003-01-21
 */
public class CompileContextImpl extends UserDataHolderBase implements CompileContextEx {
    private class MyMessageBuilder extends AbstractCompileMessageBuilder {
        private MyMessageBuilder(CompilerMessageCategory category, LocalizeValue message) {
            super(category, message);
        }

        @Override
        public MessageBuilder url(String url) {
            return assignUrl(findPresentableUrlForMessage(url));
        }

        private @Nullable String findPresentableUrlForMessage(String url) {
            String path = TranslatingCompilerFilesMonitorImpl.normalizePath(VirtualFileUtil.urlToPath(url));
            for (Map.Entry<String, Pair<SourceGeneratingCompiler, Module>> entry : myOutputRootToSourceGeneratorMap.entrySet()) {
                String root = entry.getKey();
                if (FileUtil.isAncestor(root, path, false)) {
                    Pair<SourceGeneratingCompiler, Module> pair = entry.getValue();
                    Path presentableFile =
                        pair.getFirst().getPresentableFile(CompileContextImpl.this, pair.getSecond(), Path.of(root), Path.of(path));
                    return presentableFile != null ? VirtualFileUtil.pathToUrl(
                        FileUtil.toSystemIndependentName(presentableFile.toString())) : url;
                }
            }
            return url;
        }

        @Override
        public void add() {
            addMessage(new CompilerMessageImpl(myProject, myCategory, myMessage, myUrl, myRow, myColumn, myNavigatable));
        }
    }

    private static final Logger LOG = Logger.getInstance(CompileContextImpl.class);

    private final Project myProject;
    private final CompilerTask myTask;
    private CompileScope myCompileScope;
    private final CompositeDependencyCache myDependencyCache;
    private final boolean myMake;
    private final boolean myIsRebuild;
    private final CompileCounters myCounters;

    private boolean myRebuildRequested = false;
    private LocalizeValue myRebuildReason;
    private final NavigableMap<String, Module> myRootToModuleMap = new TreeMap<>();
    private final Map<String, Module> myContentRootToModule = new HashMap<>();
    private final Map<Module, Set<Path>> myModuleToRootsMap = new HashMap<>();
    private final Map<String, Pair<SourceGeneratingCompiler, Module>> myOutputRootToSourceGeneratorMap = new HashMap<>();
    private final Set<String> myGeneratedTestRoots = new HashSet<>();
    private Path[] myOutputDirectories;
    private Set<Path> myTestOutputDirectories;
    private final Set<String> myGeneratedSources = Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);
    private final ProjectFileIndex myProjectFileIndex; // cached for performance reasons
    private final long myStartCompilationStamp;
    private final UUID mySessionId = UUID.randomUUID();

    @RequiredReadAction
    public CompileContextImpl(
        Project project,
        CompilerTask task,
        CompileScope compileScope,
        CompositeDependencyCache dependencyCache,
        boolean isMake,
        boolean isRebuild,
        CompileCounters counters
    ) {
        myProject = project;
        myTask = task;
        myCompileScope = compileScope;
        myDependencyCache = dependencyCache;
        myMake = isMake;
        myIsRebuild = isRebuild;
        myCounters = counters;
        myStartCompilationStamp = System.currentTimeMillis();
        myProjectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();

        recalculateOutputDirs();

        putUserData(CompileCounters.KEY, counters);
    }

    @Override
    @RequiredReadAction
    public void recalculateOutputDirs() {
        Module[] allModules = ModuleManager.getInstance(myProject).getModules();

        Set<Path> allDirs = new LinkedHashSet<>();
        Set<Path> testOutputDirs = new HashSet<>();
        Set<Path> productionOutputDirs = new HashSet<>();

        myContentRootToModule.clear();
        for (Module module : allModules) {
            for (String url : ModuleRootManager.getInstance(module).getContentRootUrls()) {
                myContentRootToModule.put(
                    TranslatingCompilerFilesMonitorImpl.normalizePath(VirtualFileUtil.urlToPath(url)),
                    module
                );
            }
        }

        for (Module module : allModules) {
            ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);

            Path output = moduleCompilerPathsManager.getCompilerOutputPath(ProductionContentFolderTypeProvider.getInstance());
            if (output != null) {
                allDirs.add(output);
                productionOutputDirs.add(output);
            }

            Path testsOutput = moduleCompilerPathsManager.getCompilerOutputPath(TestContentFolderTypeProvider.getInstance());
            if (testsOutput != null) {
                allDirs.add(testsOutput);
                testOutputDirs.add(testsOutput);
            }
        }
        myOutputDirectories = allDirs.toArray(new Path[allDirs.size()]);
        // need this to ensure that the sent contains only _dedicated_ test output dirs
        // Directories that are configured for both test and production classes must not be added in the resulting set
        testOutputDirs.removeAll(productionOutputDirs);
        myTestOutputDirectories = Collections.unmodifiableSet(testOutputDirs);
    }

    @Override
    public void markGenerated(Collection<Path> files) {
        for (Path file : files) {
            myGeneratedSources.add(TranslatingCompilerFilesMonitorImpl.normalizePath(file.toString()));
        }
    }

    @Override
    public long getStartCompilationStamp() {
        return myStartCompilationStamp;
    }

    @Override
    public boolean isGenerated(Path file) {
        String path = TranslatingCompilerFilesMonitorImpl.normalizePath(file.toString());
        if (myGeneratedSources.contains(path)) {
            return true;
        }
        if (findRootByPrefix(myRootToModuleMap, path) != null) {
            return true;
        }

        Module module = getModuleByFile(file);
        return module != null && module.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class).computeSafeIfAny(provider -> {
            List<ModuleAdditionalOutputDirectory> outputDirectories = provider.getOutputDirectories();
            for (ModuleAdditionalOutputDirectory outputDirectory : outputDirectories) {
                String outputPath = outputDirectory.path();
                if (outputPath != null && FileUtil.isAncestor(new File(outputPath), file.toFile(), true)) {
                    return provider;
                }
            }
            return null;
        }) != null;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public CompositeDependencyCache getDependencyCache() {
        return myDependencyCache;
    }

    @Override
    public MessageBuilder newMessage(CompilerMessageCategory category, LocalizeValue message) {
        return new MyMessageBuilder(category, message);
    }

    @Override
    public void addMessage(CompilerMessage msg) {
        myTask.addMessage(msg);
    }

    @Override
    public int getMessageCount(@Nullable CompilerMessageCategory category) {
        return myCounters.get(category);
    }

    @Override
    public CompileScope getCompileScope() {
        return myCompileScope;
    }

    @Override
    public void requestRebuildNextTime(LocalizeValue message) {
        if (!myRebuildRequested) {
            myRebuildRequested = true;
            myRebuildReason = message;
            newError(message).add();
        }
    }

    public boolean isRebuildRequested() {
        return myRebuildRequested;
    }

    public LocalizeValue getRebuildReason() {
        return myRebuildReason;
    }

    @Override
    public ProgressIndicator getProgressIndicator() {
        return myTask.getIndicator();
    }

    @Override
    public void assignModule(Path root, Module module, boolean isTestSource, @Nullable Compiler compiler) {
        String rootPath = TranslatingCompilerFilesMonitorImpl.normalizePath(root.toString());
        try {
            myRootToModuleMap.put(rootPath, module);
            Set<Path> set = myModuleToRootsMap.get(module);
            if (set == null) {
                set = new HashSet<>();
                myModuleToRootsMap.put(module, set);
            }
            set.add(root);
            if (isTestSource) {
                myGeneratedTestRoots.add(rootPath);
            }
            if (compiler instanceof SourceGeneratingCompiler sourceGeneratingCompiler) {
                myOutputRootToSourceGeneratorMap.put(rootPath, Pair.create(sourceGeneratingCompiler, module));
            }
        }
        finally {
            myModuleToRootsCache.remove(module);
        }
    }

    @Override
    public @Nullable Path getSourceFileByOutputFile(Path outputFile) {
        OutputSourceInfo info = ProjectCompilerState.getInstance(myProject)
            .getOutputInfo(TranslatingCompilerFilesMonitorImpl.normalizePath(outputFile.toString()));
        return info == null ? null : Path.of(info.sourcePath());
    }

    @Override
    public Module getModuleByFile(Path file) {
        String path = TranslatingCompilerFilesMonitorImpl.normalizePath(file.toString());
        String root = findRootByPrefix(myRootToModuleMap, path);
        if (root != null) {
            Module module = myRootToModuleMap.get(root);
            if (module != null) {
                LOG.assertTrue(!module.isDisposed());
            }
            return module;
        }
        String current = path;
        while (current != null) {
            Module module = myContentRootToModule.get(current);
            if (module != null) {
                LOG.assertTrue(!module.isDisposed());
                return module;
            }
            int slash = current.lastIndexOf('/');
            current = slash > 0 ? current.substring(0, slash) : null;
        }
        return null;
    }

    private static @Nullable String findRootByPrefix(NavigableMap<String, Module> rootMap, String path) {
        String bestRoot = null;
        for (String root : rootMap.keySet()) {
            if (FileUtil.startsWith(path, root)
                && (path.length() == root.length() || path.charAt(root.length()) == '/' || root.endsWith("/"))
                && (bestRoot == null || root.length() > bestRoot.length())) {
                bestRoot = root;
            }
        }
        return bestRoot;
    }

    private final Map<Module, Path[]> myModuleToRootsCache = new HashMap<>();

    @Override
    public Path[] getSourceRoots(Module module) {
        Path[] cachedRoots = myModuleToRootsCache.get(module);
        if (cachedRoots != null) {
            return cachedRoots;
        }

        Set<Path> additionalRoots = myModuleToRootsMap.get(module);
        VirtualFile[] moduleRootFiles =
            ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest());
        List<Path> allRoots = new ArrayList<>(moduleRootFiles.length + (additionalRoots == null ? 0 : additionalRoots.size()));
        for (VirtualFile moduleRootFile : moduleRootFiles) {
            allRoots.add(moduleRootFile.toNioPath());
        }
        if (additionalRoots != null) {
            allRoots.addAll(additionalRoots);
        }
        Path[] result = allRoots.toArray(new Path[allRoots.size()]);
        myModuleToRootsCache.put(module, result);
        return result;
    }

    @Override
    public Path[] getAllOutputDirectories() {
        return myOutputDirectories;
    }

    @Override
    public Set<Path> getTestOutputDirectories() {
        return myTestOutputDirectories;
    }

    @Override
    public Path getModuleOutputDirectory(Module module) {
        return CompilerPaths.getModuleOutputDirectory(module, false);
    }

    @Override
    public Path getModuleOutputDirectoryForTests(Module module) {
        return CompilerPaths.getModuleOutputDirectory(module, true);
    }

    @Override
    public Path getOutputForFile(Module module, Path file) {
        ContentFolderTypeProvider contentFolderTypeForFile = null;
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(file);
        if (virtualFile != null) {
            contentFolderTypeForFile = myProjectFileIndex.getContentFolderTypeForFile(virtualFile);
        }
        if (contentFolderTypeForFile == null) {
            contentFolderTypeForFile = ProductionContentFolderTypeProvider.getInstance();
        }

        return getOutputForFile(module, contentFolderTypeForFile);
    }

    @Override
    public @Nullable Path getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType) {
        return ModuleCompilerPathsManager.getInstance(module).getCompilerOutputPath(contentFolderType);
    }

    @Override
    public boolean isMake() {
        return myMake;
    }

    @Override
    public boolean isRebuild() {
        return myIsRebuild;
    }

    @Override
    public void addScope(CompileScope additionalScope) {
        myCompileScope = new CompositeScope(myCompileScope, additionalScope);
    }

    @Override
    public boolean isInTestSourceContent(Path fileOrDir) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(fileOrDir);
        if (virtualFile != null
            && (TestSourcesFilter.isTestSources(virtualFile, myProject) || myProjectFileIndex.isInTestResource(virtualFile))) {
            return true;
        }
        return isUnderRoots(myGeneratedTestRoots, fileOrDir);
    }

    @Override
    public boolean isInSourceContent(Path fileOrDir) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(fileOrDir);
        if (virtualFile != null
            && (myProjectFileIndex.isInSourceContent(virtualFile) || myProjectFileIndex.isInResource(virtualFile))) {
            return true;
        }
        return isUnderRoots(myRootToModuleMap.keySet(), fileOrDir);
    }

    private static boolean isUnderRoots(Set<String> roots, Path file) {
        String path = TranslatingCompilerFilesMonitorImpl.normalizePath(file.toString());
        for (String root : roots) {
            if (FileUtil.startsWith(path, root)
                && (path.length() == root.length() || path.charAt(root.length()) == '/' || root.endsWith("/"))) {
                return true;
            }
        }
        return false;
    }

    public UUID getSessionId() {
        return mySessionId;
    }
}
