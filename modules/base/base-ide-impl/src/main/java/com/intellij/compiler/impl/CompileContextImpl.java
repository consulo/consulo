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
package com.intellij.compiler.impl;

import com.intellij.compiler.CompilerMessageImpl;
import com.intellij.compiler.ProblemsView;
import com.intellij.compiler.progress.CompilerTask;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.pom.Navigatable;
import com.intellij.util.containers.OrderedSet;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.ui.MessageCategory;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.compiler.impl.AdditionalOutputDirectoriesProvider;
import consulo.compiler.make.impl.CompositeDependencyCache;
import consulo.compiler.roots.CompilerPathsImpl;
import consulo.logging.Logger;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.dataholder.UserDataHolderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

public class CompileContextImpl extends UserDataHolderBase implements CompileContextEx {
  private static final Logger LOG = Logger.getInstance(CompileContextImpl.class);
  private final Project myProject;
  private final CompilerTask myTask;
  private final Map<CompilerMessageCategory, Collection<CompilerMessage>> myMessages =
          new EnumMap<CompilerMessageCategory, Collection<CompilerMessage>>(CompilerMessageCategory.class);
  private CompileScope myCompileScope;
  private final CompositeDependencyCache myDependencyCache;
  private final boolean myMake;
  private final boolean myIsRebuild;

  private boolean myRebuildRequested = false;
  private String myRebuildReason;
  private final Map<VirtualFile, Module> myRootToModuleMap = new HashMap<VirtualFile, Module>();
  private final Map<Module, Set<VirtualFile>> myModuleToRootsMap = new HashMap<Module, Set<VirtualFile>>();
  private final Map<VirtualFile, Pair<SourceGeneratingCompiler, Module>> myOutputRootToSourceGeneratorMap =
          new HashMap<VirtualFile, Pair<SourceGeneratingCompiler, Module>>();
  private final Set<VirtualFile> myGeneratedTestRoots = new java.util.HashSet<VirtualFile>();
  private VirtualFile[] myOutputDirectories;
  private Set<VirtualFile> myTestOutputDirectories;
  private final IntSet myGeneratedSources = IntSets.newHashSet();
  private final ProjectFileIndex myProjectFileIndex; // cached for performance reasons
  private final long myStartCompilationStamp;
  private final UUID mySessionId = UUID.randomUUID();

  public CompileContextImpl(final Project project,
                            final CompilerTask compilerSession,
                            CompileScope compileScope,
                            CompositeDependencyCache dependencyCache,
                            boolean isMake,
                            boolean isRebuild) {
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

    final Set<VirtualFile> allDirs = new OrderedSet<VirtualFile>();
    final Set<VirtualFile> testOutputDirs = new java.util.HashSet<VirtualFile>();
    final Set<VirtualFile> productionOutputDirs = new java.util.HashSet<VirtualFile>();

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
    myOutputDirectories = VfsUtil.toVirtualFileArray(allDirs);
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
    if (module != null) {
      for (AdditionalOutputDirectoriesProvider provider : AdditionalOutputDirectoriesProvider.EP_NAME.getExtensionList()) {
        for (String path : provider.getOutputDirectories(getProject(), module)) {
          if (path != null && VfsUtilCore.isAncestor(new File(path), new File(file.getPath()), true)) {
            return true;
          }
        }
      }
    }
    return false;
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
  public void addMessage(CompilerMessageCategory category,
                         String message,
                         String url,
                         int lineNum,
                         int columnNum,
                         Navigatable navigatable) {
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
    return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {
        if (file.isValid()) {
          for (final Map.Entry<VirtualFile, Pair<SourceGeneratingCompiler, Module>> entry : myOutputRootToSourceGeneratorMap.entrySet()) {
            final VirtualFile root = entry.getKey();
            if (VfsUtilCore.isAncestor(root, file, false)) {
              final Pair<SourceGeneratingCompiler, Module> pair = entry.getValue();
              final VirtualFile presentableFile = pair.getFirst().getPresentableFile(CompileContextImpl.this, pair.getSecond(), root, file);
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
      navigatable = new OpenFileDescriptor(myProject, file, -1, -1);
    }
    final CompilerMessageCategory category = message.getCategory();
    final int type = translateCategory(category);
    final String[] text = ProblemsView.convertMessage(message.getMessage());
    final String groupName = file != null ? file.getPresentableUrl() : category.getPresentableText();
    ProblemsView.getInstance(myProject).addMessage(type, text, groupName, navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix());
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
        set = new HashSet<VirtualFile>();
        myModuleToRootsMap.put(module, set);
      }
      set.add(root);
      if (isTestSource) {
        myGeneratedTestRoots.add(root);
      }
      if (compiler instanceof SourceGeneratingCompiler) {
        myOutputRootToSourceGeneratorMap.put(root, new Pair<SourceGeneratingCompiler, Module>((SourceGeneratingCompiler)compiler, module));
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
      if (VfsUtil.isAncestor(root, file, false)) {
        final Module mod = myRootToModuleMap.get(root);
        if (mod != null) {
          LOG.assertTrue(!mod.isDisposed());
        }
        return mod;
      }
    }
    return null;
  }


  private final Map<Module, VirtualFile[]> myModuleToRootsCache = new HashMap<Module, VirtualFile[]>();

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
    VirtualFile[] moduleRoots = ModuleRootManager.getInstance(module).getContentFolderFiles(ContentFolderScopes.productionAndTest());
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
    return CompilerPathsImpl.getModuleOutputDirectory(module, false);
  }

  @Override
  public VirtualFile getModuleOutputDirectoryForTests(Module module) {
    return CompilerPathsImpl.getModuleOutputDirectory(module, true);
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
