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

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 21, 2003
 * Time: 4:19:03 PM
 */
package consulo.compiler.impl.internal;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.compiler.*;
import consulo.compiler.Compiler;
import consulo.compiler.impl.internal.scope.CompositeScope;
import consulo.compiler.scope.CompileScope;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.language.psi.stub.FileBasedIndex;
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

public class CompileContextImpl extends UserDataHolderBase implements CompileContextEx {
    private static final Logger LOG = Logger.getInstance(CompileContextImpl.class);
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

    public CompileContextImpl(
        final Project project,
        final CompilerTask compilerSession,
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
    public void recalculateOutputDirs() {
        final Module[] allModules = ModuleManager.getInstance(myProject).getModules();

        final Set<VirtualFile> allDirs = new OrderedSet<>();
        final Set<VirtualFile> testOutputDirs = new HashSet<>();
        final Set<VirtualFile> productionOutputDirs = new HashSet<>();

        for (Module module : allModules) {
            ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);

            final VirtualFile output = moduleCompilerPathsManager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
            if (output != null && output.isValid()) {
                allDirs.add(output);
                productionOutputDirs.add(output);
            }

            final VirtualFile testsOutput = moduleCompilerPathsManager.getCompilerOutput(TestContentFolderTypeProvider.getInstance());
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
        for (final VirtualFile file : files) {
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

        final Module module = getModuleByFile(file);
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
        final File zipFile = CompilerPathsEx.getZippedOutputPath(myProject, outputDir);
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
    private VirtualFile findPresentableFileForMessage(@Nullable final String url) {
        final VirtualFile file = findFileByUrl(url);
        if (file == null) {
            return null;
        }
        return ApplicationManager.getApplication().runReadAction(new Supplier<VirtualFile>() {
            @Override
            public VirtualFile get() {
                if (file.isValid()) {
                    for (final Map.Entry<VirtualFile, Pair<SourceGeneratingCompiler, Module>> entry : myOutputRootToSourceGeneratorMap.entrySet()) {
                        final VirtualFile root = entry.getKey();
                        if (VirtualFileUtil.isAncestor(root, file, false)) {
                            final Pair<SourceGeneratingCompiler, Module> pair = entry.getValue();
                            final VirtualFile presentableFile =
                                pair.getFirst().getPresentableFile(CompileContextImpl.this, pair.getSecond(), root, file);
                            return presentableFile != null ? presentableFile : file;
                        }
                    }
                }
                return file;
            }
        });
    }

    @Nullable
    private static VirtualFile findFileByUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file == null) {
            // groovy stubs may be placed in completely random directories which aren't refreshed automatically
            return VirtualFileManager.getInstance().refreshAndFindFileByUrl(url);
        }
        return file;
    }

    @Override
    public void addMessage(CompilerMessage msg) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
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
        final VirtualFile file = message.getVirtualFile();
        Navigatable navigatable = message.getNavigatable();
        if (navigatable == null && file != null) {
            OpenFileDescriptorFactory factory = OpenFileDescriptorFactory.getInstance(myProject);
            navigatable = factory.newBuilder(file).build();
        }
        final CompilerMessageCategory category = message.getCategory();
        final int type = translateCategory(category);
        final String[] text = ProblemsView.convertMessage(message.getMessage());
        final String groupName = file != null ? file.getPresentableUrl() : category.getPresentableText();
        ProblemsView.getInstance(myProject)
            .addMessage(type, text, groupName, navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix());
    }

    private static int translateCategory(CompilerMessageCategory category) {
        if (CompilerMessageCategory.ERROR.equals(category)) {
            return MessageCategory.ERROR;
        }
        if (CompilerMessageCategory.WARNING.equals(category)) {
            return MessageCategory.WARNING;
        }
        if (CompilerMessageCategory.STATISTICS.equals(category)) {
            return MessageCategory.STATISTICS;
        }
        if (CompilerMessageCategory.INFORMATION.equals(category)) {
            return MessageCategory.INFORMATION;
        }
        LOG.error("Unknown message category: " + category);
        return 0;
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
        //  return myProgressIndicatorProxy;
        //}
        return myTask.getIndicator();
    }

    @Override
    public void assignModule(@Nonnull VirtualFile root, @Nonnull Module module, final boolean isTestSource, @Nullable Compiler compiler) {
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
            if (compiler instanceof SourceGeneratingCompiler) {
                myOutputRootToSourceGeneratorMap.put(root, new Pair<>((SourceGeneratingCompiler)compiler, module));
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
        final Module module = myProjectFileIndex.getModuleForFile(file);
        if (module != null) {
            LOG.assertTrue(!module.isDisposed());
            return module;
        }
        for (final VirtualFile root : myRootToModuleMap.keySet()) {
            if (VirtualFileUtil.isAncestor(root, file, false)) {
                final Module mod = myRootToModuleMap.get(root);
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

        final VirtualFile[] allRoots = new VirtualFile[additionalRoots.size() + moduleRoots.length];
        System.arraycopy(moduleRoots, 0, allRoots, 0, moduleRoots.length);
        int index = moduleRoots.length;
        for (final VirtualFile additionalRoot : additionalRoots) {
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
    public void addScope(final CompileScope additionalScope) {
        myCompileScope = new CompositeScope(myCompileScope, additionalScope);
    }

    @Override
    public boolean isInTestSourceContent(@Nonnull final VirtualFile fileOrDir) {
        if (TestSourcesFilter.isTestSources(fileOrDir, myProject) || myProjectFileIndex.isInTestResource(fileOrDir)) {
            return true;
        }
        if (isUnderRoots(myGeneratedTestRoots, fileOrDir)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isInSourceContent(@Nonnull final VirtualFile fileOrDir) {
        if (myProjectFileIndex.isInSourceContent(fileOrDir) || myProjectFileIndex.isInResource(fileOrDir)) {
            return true;
        }
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
