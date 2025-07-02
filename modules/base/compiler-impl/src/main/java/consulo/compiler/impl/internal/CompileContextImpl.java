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
import consulo.language.psi.stub.FileBasedIndex;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.ui.ex.MessageCategory;
import consulo.util.collection.OrderedSet;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author Eugene Zhuravlev
 * @since 2003-01-21
 */
public class CompileContextImpl extends UserDataHolderBase implements CompileContextEx {
    private static final Logger LOG = Logger.getInstance(CompileContextImpl.class);
    @Nonnull
    private final Project myProject;
    private final CompilerTask myTask;
    private final Map<CompilerMessageCategory, Collection<CompilerMessage>> myMessages = new EnumMap<>(CompilerMessageCategory.class);
    private CompileScope myCompileScope;
    private final CompositeDependencyCache myDependencyCache;
    private final boolean myMake;
    private final boolean myIsRebuild;

    private boolean myRebuildRequested = false;
    private String myRebuildReason;
    private final Map<VirtualFile, Module> myRootToModuleMap = new HashMap<>();
    private final Map<Module, Set<VirtualFile>> myModuleToRootsMap = new HashMap<>();
    private final Map<VirtualFile, Pair<SourceGeneratingCompiler, Module>> myOutputRootToSourceGeneratorMap = new HashMap<>();
    private final Set<VirtualFile> myGeneratedTestRoots = new HashSet<>();
    private VirtualFile[] myOutputDirectories;
    private Set<VirtualFile> myTestOutputDirectories;
    private final IntSet myGeneratedSources = IntSets.newHashSet();
    private final ProjectFileIndex myProjectFileIndex; // cached for performance reasons
    private final long myStartCompilationStamp;
    private final UUID mySessionId = UUID.randomUUID();

    @RequiredReadAction
    public CompileContextImpl(
        @Nonnull Project project,
        CompilerTask compilerSession,
        CompileScope compileScope,
        CompositeDependencyCache dependencyCache,
        boolean isMake,
        boolean isRebuild
    ) {
        myProject = project;
        myTask = compilerSession;
        myCompileScope = compileScope;
        myDependencyCache = dependencyCache;
        myMake = isMake;
        myIsRebuild = isRebuild;
        myStartCompilationStamp = System.currentTimeMillis();
        myProjectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();

        recalculateOutputDirs();
    }

    @Override
    @RequiredReadAction
    public void recalculateOutputDirs() {
        Module[] allModules = ModuleManager.getInstance(myProject).getModules();

        Set<VirtualFile> allDirs = new OrderedSet<>();
        Set<VirtualFile> testOutputDirs = new HashSet<>();
        Set<VirtualFile> productionOutputDirs = new HashSet<>();

        for (Module module : allModules) {
            ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);

            VirtualFile output = moduleCompilerPathsManager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
            if (output != null && output.isValid()) {
                allDirs.add(output);
                productionOutputDirs.add(output);
            }

            VirtualFile testsOutput = moduleCompilerPathsManager.getCompilerOutput(TestContentFolderTypeProvider.getInstance());
            if (testsOutput != null && testsOutput.isValid()) {
                allDirs.add(testsOutput);
                testOutputDirs.add(testsOutput);
            }
        }
        myOutputDirectories = VirtualFileUtil.toVirtualFileArray(allDirs);
        // need this to ensure that the sent contains only _dedicated_ test output dirs
        // Directories that are configured for both test and production classes must not be added in the resulting set
        testOutputDirs.removeAll(productionOutputDirs);
        myTestOutputDirectories = Collections.unmodifiableSet(testOutputDirs);
    }

    @Override
    public void markGenerated(Collection<VirtualFile> files) {
        for (VirtualFile file : files) {
            myGeneratedSources.add(FileBasedIndex.getFileId(file));
        }
    }

    @Override
    public long getStartCompilationStamp() {
        return myStartCompilationStamp;
    }

    @Override
    public boolean isGenerated(VirtualFile file) {
        if (myGeneratedSources.contains(FileBasedIndex.getFileId(file))) {
            return true;
        }
        if (isUnderRoots(myRootToModuleMap.keySet(), file)) {
            return true;
        }

        Module module = getModuleByFile(file);
        return module != null && module.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class).computeSafeIfAny(provider -> {
            List<ModuleAdditionalOutputDirectory> outputDirectories = provider.getOutputDirectories();
            for (ModuleAdditionalOutputDirectory outputDirectory : outputDirectories) {
                String path = outputDirectory.path();
                if (path != null && FileUtil.isAncestor(new File(path), new File(file.getPath()), true)) {
                    return provider;
                }
            }
            return null;
        }) != null;
    }

    /*
    private JBZipFile lookupZip(String outputDir) {
        synchronized (myOpenZipFiles) {
            JBZipFile zip = myOpenZipFiles.get(outputDir);
            if (zip == null) {
                File zipFile = CompilerPathsEx.getZippedOutputPath(myProject, outputDir);
                try {
                    try {
                        zip = new JBZipFile(zipFile);
                    }
                    catch (FileNotFoundException e) {
                        try {
                            zipFile.createNewFile();
                            zip = new JBZipFile(zipFile);
                        }
                        catch (IOException e1) {
                            zipFile.getParentFile().mkdirs();
                            zipFile.createNewFile();
                            zip = new JBZipFile(zipFile);
                        }
                    }
                    myOpenZipFiles.put(outputDir, zip);
                }
                catch (IOException e) {
                    LOG.info(e);
                    addMessage(CompilerMessageCategory.ERROR, "Cannot create zip file " + zipFile.getPath() + ": " + e.getMessage(), null, -1, -1);
                }
            }
            return zip;
        }
    }
    */

    @Nonnull
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public CompositeDependencyCache getDependencyCache() {
        return myDependencyCache;
    }

    @Override
    public CompilerMessage[] getMessages(CompilerMessageCategory category) {
        Collection<CompilerMessage> collection = myMessages.get(category);
        if (collection == null) {
            return CompilerMessage.EMPTY_ARRAY;
        }
        return collection.toArray(new CompilerMessage[collection.size()]);
    }

    @Override
    public void addMessage(CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) {
        CompilerMessageImpl msg =
            new CompilerMessageImpl(myProject, category, message, findPresentableFileForMessage(url), lineNum, columnNum, null);
        addMessage(msg);
    }

    @Override
    public void addMessage(
        CompilerMessageCategory category,
        String message,
        String url,
        int lineNum,
        int columnNum,
        Navigatable navigatable
    ) {
        CompilerMessageImpl msg =
            new CompilerMessageImpl(myProject, category, message, findPresentableFileForMessage(url), lineNum, columnNum, navigatable);
        addMessage(msg);
    }

    @Nullable
    private VirtualFile findPresentableFileForMessage(@Nullable String url) {
        VirtualFile file = findFileByUrl(url);
        if (file == null) {
            return null;
        }
        return myProject.getApplication().runReadAction((Supplier<VirtualFile>)() -> {
            if (file.isValid()) {
                for (Map.Entry<VirtualFile, Pair<SourceGeneratingCompiler, Module>> entry : myOutputRootToSourceGeneratorMap.entrySet()) {
                    VirtualFile root = entry.getKey();
                    if (VirtualFileUtil.isAncestor(root, file, false)) {
                        Pair<SourceGeneratingCompiler, Module> pair = entry.getValue();
                        VirtualFile presentableFile =
                            pair.getFirst().getPresentableFile(CompileContextImpl.this, pair.getSecond(), root, file);
                        return presentableFile != null ? presentableFile : file;
                    }
                }
            }
            return file;
        });
    }

    @Nullable
    private static VirtualFile findFileByUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        VirtualFile file = virtualFileManager.findFileByUrl(url);
        if (file == null) {
            // groovy stubs may be placed in completely random directories which aren't refreshed automatically
            return virtualFileManager.refreshAndFindFileByUrl(url);
        }
        return file;
    }

    @Override
    public void addMessage(CompilerMessage msg) {
        if (myProject.getApplication().isUnitTestMode()) {
            LOG.info("addMessage: " + msg + " this=" + this);
        }

        Collection<CompilerMessage> messages = myMessages.get(msg.getCategory());
        if (messages == null) {
            messages = new LinkedHashSet<>();
            myMessages.put(msg.getCategory(), messages);
        }
        if (messages.add(msg)) {
            myTask.addMessage(msg);
        }

        addMessageToProblemsView(msg);
    }

    private void addMessageToProblemsView(CompilerMessage message) {
        VirtualFile file = message.getVirtualFile();
        Navigatable navigatable = message.getNavigatable();
        if (navigatable == null && file != null) {
            OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(myProject);
            navigatable = factory.newBuilder(file).build();
        }
        CompilerMessageCategory category = message.getCategory();
        int type = translateCategory(category);
        String[] text = ProblemsView.convertMessage(message.getMessage());
        LocalizeValue groupName = file != null ? LocalizeValue.of(file.getPresentableUrl()) : category.getPresentableText();
        ProblemsView.getInstance(myProject)
            .addMessage(type, text, groupName.get(), navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix());
    }

    private static int translateCategory(CompilerMessageCategory category) {
        return switch (category) {
            case ERROR -> MessageCategory.ERROR;
            case WARNING -> MessageCategory.WARNING;
            case STATISTICS -> MessageCategory.STATISTICS;
            case INFORMATION -> MessageCategory.INFORMATION;
        };
    }

    @Override
    public int getMessageCount(CompilerMessageCategory category) {
        if (category != null) {
            Collection<CompilerMessage> collection = myMessages.get(category);
            return collection != null ? collection.size() : 0;
        }
        int count = 0;
        for (Collection<CompilerMessage> collection : myMessages.values()) {
            if (collection != null) {
                count += collection.size();
            }
        }
        return count;
    }

    @Override
    public CompileScope getCompileScope() {
        return myCompileScope;
    }

    @Override
    public void requestRebuildNextTime(String message) {
        if (!myRebuildRequested) {
            myRebuildRequested = true;
            myRebuildReason = message;
            addMessage(CompilerMessageCategory.ERROR, message, null, -1, -1);
        }
    }

    public boolean isRebuildRequested() {
        return myRebuildRequested;
    }

    public String getRebuildReason() {
        return myRebuildReason;
    }

    @Nonnull
    @Override
    public ProgressIndicator getProgressIndicator() {
        //if (myProgressIndicatorProxy != null) {
        //    return myProgressIndicatorProxy;
        //}
        return myTask.getIndicator();
    }

    @Override
    public void assignModule(@Nonnull VirtualFile root, @Nonnull Module module, boolean isTestSource, @Nullable Compiler compiler) {
        try {
            myRootToModuleMap.put(root, module);
            Set<VirtualFile> set = myModuleToRootsMap.get(module);
            if (set == null) {
                set = new HashSet<>();
                myModuleToRootsMap.put(module, set);
            }
            set.add(root);
            if (isTestSource) {
                myGeneratedTestRoots.add(root);
            }
            if (compiler instanceof SourceGeneratingCompiler sourceGeneratingCompiler) {
                myOutputRootToSourceGeneratorMap.put(root, Pair.create(sourceGeneratingCompiler, module));
            }
        }
        finally {
            myModuleToRootsCache.remove(module);
        }
    }

    @Override
    @Nullable
    public VirtualFile getSourceFileByOutputFile(VirtualFile outputFile) {
        return TranslatingCompilerFilesMonitorImpl.getSourceFileByOutput(outputFile);
    }

    @Override
    public Module getModuleByFile(VirtualFile file) {
        Module module = myProjectFileIndex.getModuleForFile(file);
        if (module != null) {
            LOG.assertTrue(!module.isDisposed());
            return module;
        }
        for (VirtualFile root : myRootToModuleMap.keySet()) {
            if (VirtualFileUtil.isAncestor(root, file, false)) {
                Module mod = myRootToModuleMap.get(root);
                if (mod != null) {
                    LOG.assertTrue(!mod.isDisposed());
                }
                return mod;
            }
        }
        return null;
    }


    private final Map<Module, VirtualFile[]> myModuleToRootsCache = new HashMap<>();

    @Override
    public VirtualFile[] getSourceRoots(Module module) {
        VirtualFile[] cachedRoots = myModuleToRootsCache.get(module);
        if (cachedRoots != null) {
            if (areFilesValid(cachedRoots)) {
                return cachedRoots;
            }
            else {
                myModuleToRootsCache.remove(module); // clear cache for this module and rebuild list of roots
            }
        }

        Set<VirtualFile> additionalRoots = myModuleToRootsMap.get(module);
        VirtualFile[] moduleRoots =
            ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest());
        if (additionalRoots == null || additionalRoots.isEmpty()) {
            myModuleToRootsCache.put(module, moduleRoots);
            return moduleRoots;
        }

        VirtualFile[] allRoots = new VirtualFile[additionalRoots.size() + moduleRoots.length];
        System.arraycopy(moduleRoots, 0, allRoots, 0, moduleRoots.length);
        int index = moduleRoots.length;
        for (VirtualFile additionalRoot : additionalRoots) {
            allRoots[index++] = additionalRoot;
        }
        myModuleToRootsCache.put(module, allRoots);
        return allRoots;
    }

    private static boolean areFilesValid(VirtualFile[] files) {
        for (VirtualFile file : files) {
            if (!file.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public VirtualFile[] getAllOutputDirectories() {
        return myOutputDirectories;
    }

    @Override
    @Nonnull
    public Set<VirtualFile> getTestOutputDirectories() {
        return myTestOutputDirectories;
    }

    @Override
    public VirtualFile getModuleOutputDirectory(Module module) {
        return CompilerPaths.getModuleOutputDirectory(module, false);
    }

    @Override
    public VirtualFile getModuleOutputDirectoryForTests(Module module) {
        return CompilerPaths.getModuleOutputDirectory(module, true);
    }

    @Override
    public VirtualFile getOutputForFile(Module module, VirtualFile virtualFile) {
        ContentFolderTypeProvider contentFolderTypeForFile = myProjectFileIndex.getContentFolderTypeForFile(virtualFile);
        if (contentFolderTypeForFile == null) {
            contentFolderTypeForFile = ProductionContentFolderTypeProvider.getInstance();
        }

        return getOutputForFile(module, contentFolderTypeForFile);
    }

    @Nullable
    @Override
    public VirtualFile getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType) {
        return ModuleCompilerPathsManager.getInstance(module).getCompilerOutput(contentFolderType);
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
    public boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir) {
        if (TestSourcesFilter.isTestSources(fileOrDir, myProject) || myProjectFileIndex.isInTestResource(fileOrDir)) {
            return true;
        }
        //noinspection RedundantIfStatement
        if (isUnderRoots(myGeneratedTestRoots, fileOrDir)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isInSourceContent(@Nonnull VirtualFile fileOrDir) {
        if (myProjectFileIndex.isInSourceContent(fileOrDir) || myProjectFileIndex.isInResource(fileOrDir)) {
            return true;
        }
        //noinspection RedundantIfStatement
        if (isUnderRoots(myRootToModuleMap.keySet(), fileOrDir)) {
            return true;
        }
        return false;
    }

    public static boolean isUnderRoots(@Nonnull Set<VirtualFile> roots, @Nonnull VirtualFile file) {
        VirtualFile parent = file;
        while (true) {
            if (parent == null) {
                return false;
            }
            if (roots.contains(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
    }

    public UUID getSessionId() {
        return mySessionId;
    }
}
