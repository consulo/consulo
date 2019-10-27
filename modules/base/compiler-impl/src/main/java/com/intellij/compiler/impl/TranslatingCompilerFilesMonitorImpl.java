/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.compiler.impl;

import com.intellij.ProjectTopics;
import com.intellij.compiler.CompilerIOUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SLRUCache;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.*;
import com.intellij.util.messages.MessageBusConnection;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.compiler.impl.TranslatingCompilerFilesMonitor;
import consulo.compiler.impl.TranslatingCompilerFilesMonitorHelper;
import consulo.compiler.impl.TranslationCompilerFilesMonitorVfsListener;
import consulo.compiler.make.DependencyCache;
import consulo.logging.Logger;
import consulo.module.extension.ModuleExtension;
import consulo.roots.ContentFolderScopes;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;
import consulo.ui.UIAccess;
import gnu.trove.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author Eugene Zhuravlev
 * @since Jun 3, 2008
 * <p>
 * A source file is scheduled for recompilation if
 * 1. its timestamp has changed
 * 2. one of its corresponding output files was deleted
 * 3. output root of containing module has changed
 * <p>
 * An output file is scheduled for deletion if:
 * 1. corresponding source file has been scheduled for recompilation (see above)
 * 2. corresponding source file has been deleted
 */
@Singleton
public class TranslatingCompilerFilesMonitorImpl extends TranslatingCompilerFilesMonitor implements Disposable {
  static class ProjectListener implements ProjectManagerListener {
    private final Map<Project, MessageBusConnection> myConnections = new HashMap<>();
    private final Provider<TranslatingCompilerFilesMonitor> myMonitorProvider;

    @Inject
    ProjectListener(Provider<TranslatingCompilerFilesMonitor> monitorProvider) {
      myMonitorProvider = monitorProvider;
    }

    @Override
    public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
      TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

      final MessageBusConnection conn = project.getMessageBus().connect();
      myConnections.put(project, conn);
      final ProjectRef projRef = new ProjectRef(project);
      final int projectId = monitor.getProjectId(project);

      monitor.watchProject(project);

      conn.subscribe(ModuleExtension.CHANGE_TOPIC, (oldExtension, newExtension) -> {
        for (TranslatingCompilerFilesMonitorHelper helper : TranslatingCompilerFilesMonitorHelper.EP_NAME.getExtensionList()) {
          if (helper.isModuleExtensionAffectToCompilation(newExtension)) {
            monitor.myForceCompiling = true;
            break;
          }
        }
      });

      conn.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
        private VirtualFile[] myRootsBefore;
        private Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);

        @Override
        public void beforeRootsChange(final ModuleRootEvent event) {
          if (monitor.isSuspended(projectId)) {
            return;
          }
          try {
            myRootsBefore = monitor.getRootsForScan(projRef.get());
          }
          catch (ProjectRef.ProjectClosedException e) {
            myRootsBefore = null;
          }
        }

        @Override
        public void rootsChanged(final ModuleRootEvent event) {
          if (monitor.isSuspended(projectId)) {
            return;
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Before roots changed for projectId=" + projectId + "; url=" + project.getPresentableUrl());
          }
          try {
            final VirtualFile[] rootsBefore = myRootsBefore;
            myRootsBefore = null;
            final VirtualFile[] rootsAfter = monitor.getRootsForScan(projRef.get());
            final Set<VirtualFile> newRoots = new HashSet<>();
            final Set<VirtualFile> oldRoots = new HashSet<>();
            {
              if (rootsAfter.length > 0) {
                ContainerUtil.addAll(newRoots, rootsAfter);
              }
              if (rootsBefore != null) {
                newRoots.removeAll(Arrays.asList(rootsBefore));
              }
            }
            {
              if (rootsBefore != null) {
                ContainerUtil.addAll(oldRoots, rootsBefore);
              }
              if (!oldRoots.isEmpty() && rootsAfter.length > 0) {
                oldRoots.removeAll(Arrays.asList(rootsAfter));
              }
            }

            myAlarm.cancelAllRequests(); // need alarm to deal with multiple rootsChanged events
            myAlarm.addRequest(new Runnable() {
              @Override
              public void run() {
                monitor.startAsyncScan(projectId);
                new Task.Backgroundable(project, CompilerBundle.message("compiler.initial.scanning.progress.text"), false) {
                  @Override
                  public void run(@Nonnull final ProgressIndicator indicator) {
                    try {
                      if (newRoots.size() > 0) {
                        monitor.scanSourceContent(projRef, newRoots, newRoots.size(), true);
                      }
                      if (oldRoots.size() > 0) {
                        monitor.scanSourceContent(projRef, oldRoots, oldRoots.size(), false);
                      }
                      monitor.markOldOutputRoots(projRef, monitor.buildOutputRootsLayout(projRef));
                    }
                    catch (ProjectRef.ProjectClosedException swallowed) {
                      // ignored
                    }
                    finally {
                      monitor.terminateAsyncScan(projectId, false);
                    }
                  }
                }.queue();
              }
            }, 500, ModalityState.NON_MODAL);
          }
          catch (ProjectRef.ProjectClosedException e) {
            LOG.info(e);
          }
        }
      });

      monitor.scanSourcesForCompilableFiles(project);
    }

    @Override
    public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
      TranslatingCompilerFilesMonitorImpl monitor = getMonitor();

      final int projectId = monitor.getProjectId(project);
      monitor.terminateAsyncScan(projectId, true);
      myConnections.remove(project).disconnect();
      synchronized (monitor.myDataLock) {
        monitor.mySourcesToRecompile.remove(projectId);
        monitor.myOutputsToDelete.remove(projectId);  // drop cache to save memory
      }
    }

    @Nonnull
    TranslatingCompilerFilesMonitorImpl getMonitor() {
      return (TranslatingCompilerFilesMonitorImpl)myMonitorProvider.get();
    }
  }

  private static final Logger LOG = Logger.getInstance(TranslatingCompilerFilesMonitorImpl.class);

  public static final boolean DEBUG_MODE = false;

  private static final Key<Map<String, VirtualFile>> SOURCE_FILES_CACHE = Key.create("_source_url_to_vfile_cache_");

  private final Object myDataLock = new Object();

  private final TIntHashSet mySuspendedProjects = new TIntHashSet(); // projectId for all projects that should not be monitored

  private final TIntObjectHashMap<TIntHashSet> mySourcesToRecompile = new TIntObjectHashMap<>();
  // ProjectId->set of source file paths
  private PersistentHashMap<Integer, TIntObjectHashMap<Pair<Integer, Integer>>> myOutputRootsStorage;
  // ProjectId->map[moduleId->Pair(outputDirId, testOutputDirId)]

  // Map: projectId -> Map{output path -> [sourceUrl; className]}
  private final SLRUCache<Integer, Outputs> myOutputsToDelete = new SLRUCache<Integer, Outputs>(3, 3) {
    @Override
    public Outputs getIfCached(Integer key) {
      final Outputs value = super.getIfCached(key);
      if (value != null) {
        value.allocate();
      }
      return value;
    }

    @Nonnull
    @Override
    public Outputs get(Integer key) {
      final Outputs value = super.get(key);
      value.allocate();
      return value;
    }

    @Nonnull
    @Override
    public Outputs createValue(Integer key) {
      final String dirName = getFilePath(key);
      final File storeFile;
      if (StringUtil.isEmpty(dirName)) {
        storeFile = null;
      }
      else {
        final File compilerCacheDir = CompilerPaths.getCacheStoreDirectory(dirName);
        storeFile = compilerCacheDir.exists() ? new File(compilerCacheDir, "paths_to_delete.dat") : null;
      }
      return new Outputs(storeFile, loadPathsToDelete(storeFile));
    }

    @Override
    protected void onDropFromCache(Integer key, Outputs value) {
      value.release();
    }
  };
  private final SLRUCache<Project, File> myGeneratedDataPaths = new SLRUCache<Project, File>(8, 8) {
    @Override
    @Nonnull
    public File createValue(final Project project) {
      Disposer.register(project, () -> myGeneratedDataPaths.remove(project));
      return CompilerPaths.getGeneratedDataDirectory(project);
    }
  };
  private final SLRUCache<Integer, TIntObjectHashMap<Pair<Integer, Integer>>> myProjectOutputRoots = new SLRUCache<Integer, TIntObjectHashMap<Pair<Integer, Integer>>>(2, 2) {
    @Override
    protected void onDropFromCache(Integer key, TIntObjectHashMap<Pair<Integer, Integer>> value) {
      try {
        myOutputRootsStorage.put(key, value);
      }
      catch (IOException e) {
        LOG.info(e);
      }
    }

    @Override
    @Nonnull
    public TIntObjectHashMap<Pair<Integer, Integer>> createValue(Integer key) {
      TIntObjectHashMap<Pair<Integer, Integer>> map = null;
      try {
        map = myOutputRootsStorage.get(key);
      }
      catch (IOException e) {
        LOG.info(e);
      }
      return map != null ? map : new TIntObjectHashMap<>();
    }
  };

  private static final int VERSION = 2;

  private final TIntIntHashMap myInitInProgress = new TIntIntHashMap(); // projectId for successfully initialized projects
  private final Object myAsyncScanLock = new Object();

  private boolean myForceCompiling;

  private PersistentStringEnumerator myFilePathsEnumerator;

  private boolean myInitialized;

  @Inject
  public TranslatingCompilerFilesMonitorImpl() {
    ensureOutputStorageInitialized();
  }

  @Override
  public void suspendProject(Project project) {
    final int projectId = getProjectId(project);

    synchronized (myDataLock) {
      if (!mySuspendedProjects.add(projectId)) {
        return;
      }
      FileUtil.createIfDoesntExist(CompilerPaths.getRebuildMarkerFile(project));
      // cleanup internal structures to free memory
      mySourcesToRecompile.remove(projectId);
      myOutputsToDelete.remove(projectId);
      myGeneratedDataPaths.remove(project);
    }

    synchronized (myProjectOutputRoots) {
      myProjectOutputRoots.remove(projectId);
      try {
        myOutputRootsStorage.remove(projectId);
      }
      catch (IOException e) {
        LOG.info(e);
      }
    }
  }

  @Nullable
  public File getGeneratedPath(Project project) {
    return myGeneratedDataPaths.get(project);
  }

  @Override
  public void watchProject(Project project) {
    synchronized (myDataLock) {
      mySuspendedProjects.remove(getProjectId(project));
    }
  }

  @Override
  public boolean isSuspended(Project project) {
    return isSuspended(getProjectId(project));
  }

  @Override
  public boolean isSuspended(int projectId) {
    synchronized (myDataLock) {
      return mySuspendedProjects.contains(projectId);
    }
  }

  @Nullable
  public static VirtualFile getSourceFileByOutput(VirtualFile outputFile) {
    final TranslationOutputFileInfo outputFileInfo = TranslationOutputFileInfo.loadOutputInfo(outputFile);
    if (outputFileInfo != null) {
      final String path = outputFileInfo.getSourceFilePath();
      if (path != null) {
        return LocalFileSystem.getInstance().findFileByPath(path);
      }
    }
    return null;
  }

  @Override
  public void collectFiles(CompileContext context,
                           final TranslatingCompiler compiler,
                           Iterator<VirtualFile> scopeSrcIterator,
                           boolean forceCompile,
                           final boolean isRebuild,
                           Collection<VirtualFile> toCompile,
                           Collection<Trinity<File, String, Boolean>> toDelete) {
    final Project project = context.getProject();
    final int projectId = getProjectId(project);
    final CompilerManager configuration = CompilerManager.getInstance(project);
    final boolean _forceCompile = forceCompile || isRebuild || myForceCompiling;
    final Set<VirtualFile> selectedForRecompilation = new HashSet<>();
    synchronized (myDataLock) {
      final TIntHashSet pathsToRecompile = mySourcesToRecompile.get(projectId);
      if (_forceCompile || pathsToRecompile != null && !pathsToRecompile.isEmpty()) {
        if (DEBUG_MODE) {
          System.out.println("Analysing potentially recompilable files for " + compiler.getDescription());
        }
        while (scopeSrcIterator.hasNext()) {
          final VirtualFile file = scopeSrcIterator.next();
          if (!file.isValid()) {
            if (LOG.isDebugEnabled() || DEBUG_MODE) {
              LOG.debug("Skipping invalid file " + file.getPresentableUrl());
              if (DEBUG_MODE) {
                System.out.println("\t SKIPPED(INVALID) " + file.getPresentableUrl());
              }
            }
            continue;
          }
          final int fileId = getFileId(file);
          if (_forceCompile) {
            if (compiler.isCompilableFile(file, context) && !configuration.isExcludedFromCompilation(file)) {
              toCompile.add(file);
              if (DEBUG_MODE) {
                System.out.println("\t INCLUDED " + file.getPresentableUrl());
              }
              selectedForRecompilation.add(file);
              if (pathsToRecompile == null || !pathsToRecompile.contains(fileId)) {
                loadInfoAndAddSourceForRecompilation(projectId, file);
              }
            }
            else {
              if (DEBUG_MODE) {
                System.out.println("\t NOT COMPILABLE OR EXCLUDED " + file.getPresentableUrl());
              }
            }
          }
          else if (pathsToRecompile.contains(fileId)) {
            if (compiler.isCompilableFile(file, context) && !configuration.isExcludedFromCompilation(file)) {
              toCompile.add(file);
              if (DEBUG_MODE) {
                System.out.println("\t INCLUDED " + file.getPresentableUrl());
              }
              selectedForRecompilation.add(file);
            }
            else {
              if (DEBUG_MODE) {
                System.out.println("\t NOT COMPILABLE OR EXCLUDED " + file.getPresentableUrl());
              }
            }
          }
          else {
            if (DEBUG_MODE) {
              System.out.println("\t NOT INCLUDED " + file.getPresentableUrl());
            }
          }
        }
      }
      // it is important that files to delete are collected after the files to compile (see what happens if forceCompile == true)
      if (!isRebuild) {
        final Outputs outputs = myOutputsToDelete.get(projectId);
        try {
          final VirtualFileManager vfm = VirtualFileManager.getInstance();
          final LocalFileSystem lfs = LocalFileSystem.getInstance();
          final List<String> zombieEntries = new ArrayList<>();
          final Map<String, VirtualFile> srcFileCache = getFileCache(context);
          for (Map.Entry<String, SourceUrlClassNamePair> entry : outputs.getEntries()) {
            final String outputPath = entry.getKey();
            final SourceUrlClassNamePair classNamePair = entry.getValue();
            final String sourceUrl = classNamePair.getSourceUrl();

            final VirtualFile srcFile;
            if (srcFileCache.containsKey(sourceUrl)) {
              srcFile = srcFileCache.get(sourceUrl);
            }
            else {
              srcFile = vfm.findFileByUrl(sourceUrl);
              srcFileCache.put(sourceUrl, srcFile);
            }

            final boolean sourcePresent = srcFile != null;
            if (sourcePresent) {
              if (!compiler.isCompilableFile(srcFile, context)) {
                continue; // do not collect files that were compiled by another compiler
              }
              if (!selectedForRecompilation.contains(srcFile)) {
                if (!isMarkedForRecompilation(projectId, getFileId(srcFile))) {
                  if (LOG.isDebugEnabled() || DEBUG_MODE) {
                    final String message = "Found zombie entry (output is marked, but source is present and up-to-date): " + outputPath;
                    LOG.debug(message);
                    if (DEBUG_MODE) {
                      System.out.println(message);
                    }
                  }
                  zombieEntries.add(outputPath);
                }
                continue;
              }
            }
            if (lfs.findFileByPath(outputPath) != null) {
              //noinspection UnnecessaryBoxing
              final File file = new File(outputPath);
              toDelete.add(new Trinity<>(file, classNamePair.getClassName(), Boolean.valueOf(sourcePresent)));
              if (LOG.isDebugEnabled() || DEBUG_MODE) {
                final String message = "Found file to delete: " + file;
                LOG.debug(message);
                if (DEBUG_MODE) {
                  System.out.println(message);
                }
              }
            }
            else {
              if (LOG.isDebugEnabled() || DEBUG_MODE) {
                final String message = "Found zombie entry marked for deletion: " + outputPath;
                LOG.debug(message);
                if (DEBUG_MODE) {
                  System.out.println(message);
                }
              }
              // must be gagbage entry, should cleanup
              zombieEntries.add(outputPath);
            }
          }
          for (String path : zombieEntries) {
            unmarkOutputPathForDeletion(projectId, path);
          }
        }
        finally {
          outputs.release();
        }
      }
    }
  }

  private static Map<String, VirtualFile> getFileCache(CompileContext context) {
    Map<String, VirtualFile> cache = context.getUserData(SOURCE_FILES_CACHE);
    if (cache == null) {
      context.putUserData(SOURCE_FILES_CACHE, cache = new HashMap<>());
    }
    return cache;
  }

  public static int getFileId(final VirtualFile file) {
    return FileBasedIndex.getFileId(file);
  }

  public static VirtualFile findFileById(int id) {
    return ManagingFS.getInstance().findFileById(id);
  }

  @Override
  public void update(final CompileContext context, @Nullable final String outputRoot, final Collection<TranslatingCompiler.OutputItem> successfullyCompiled, final VirtualFile[] filesToRecompile)
          throws IOException {
    myForceCompiling = false;
    final Project project = context.getProject();
    final DependencyCache dependencyCache = ((CompileContextEx)context).getDependencyCache();
    final int projectId = getProjectId(project);
    if (!successfullyCompiled.isEmpty()) {
      final LocalFileSystem lfs = LocalFileSystem.getInstance();
      final IOException[] exceptions = {null};
      // need read action here to ensure that no modifications were made to VFS while updating file attributes
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          try {
            final Map<VirtualFile, TranslationSourceFileInfo> compiledSources = new HashMap<>();
            final Set<VirtualFile> forceRecompile = new HashSet<>();

            for (TranslatingCompiler.OutputItem item : successfullyCompiled) {
              final VirtualFile sourceFile = item.getSourceFile();
              final boolean isSourceValid = sourceFile.isValid();
              TranslationSourceFileInfo srcInfo = compiledSources.get(sourceFile);
              if (isSourceValid && srcInfo == null) {
                srcInfo = TranslationSourceFileInfo.loadSourceInfo(sourceFile);
                if (srcInfo != null) {
                  srcInfo.clearPaths(projectId);
                }
                else {
                  srcInfo = new TranslationSourceFileInfo();
                }
                compiledSources.put(sourceFile, srcInfo);
              }

              final String outputPath = item.getOutputPath();
              if (outputPath != null) { // can be null for packageinfo
                final VirtualFile outputFile = lfs.findFileByPath(outputPath);

                //assert outputFile != null : "Virtual file was not found for \"" + outputPath + "\"";

                if (outputFile != null) {
                  if (!sourceFile.equals(outputFile)) {
                    final String className = outputRoot == null ? null : dependencyCache.relativePathToQName(outputPath.substring(outputRoot.length()), '/');
                    if (isSourceValid) {
                      srcInfo.addOutputPath(projectId, outputPath);
                      TranslationOutputFileInfo.saveOutputInfo(outputFile, new TranslationOutputFileInfo(sourceFile.getPath(), className));
                    }
                    else {
                      markOutputPathForDeletion(projectId, outputPath, className, sourceFile.getUrl());
                    }
                  }
                }
                else {  // output file was not found
                  LOG.warn("TranslatingCompilerFilesMonitor.update():  Virtual file was not found for \"" + outputPath + "\"");
                  if (isSourceValid) {
                    forceRecompile.add(sourceFile);
                  }
                }
              }
            }
            final long compilationStartStamp = ((CompileContextEx)context).getStartCompilationStamp();
            for (Map.Entry<VirtualFile, TranslationSourceFileInfo> entry : compiledSources.entrySet()) {
              final TranslationSourceFileInfo info = entry.getValue();
              final VirtualFile file = entry.getKey();

              final long fileStamp = file.getTimeStamp();
              info.updateTimestamp(projectId, fileStamp);
              TranslationSourceFileInfo.saveSourceInfo(file, info);
              if (LOG.isDebugEnabled() || DEBUG_MODE) {
                final String message = "Unschedule recompilation (successfully compiled) " + file.getPresentableUrl();
                LOG.debug(message);
                if (DEBUG_MODE) {
                  System.out.println(message);
                }
              }
              removeSourceForRecompilation(projectId, Math.abs(getFileId(file)));
              if (fileStamp > compilationStartStamp && !((CompileContextEx)context).isGenerated(file) || forceRecompile.contains(file)) {
                // changes were made during compilation, need to re-schedule compilation
                // it is important to invoke removeSourceForRecompilation() before this call to make sure
                // the corresponding output paths will be scheduled for deletion
                addSourceForRecompilation(projectId, file, info);
              }
            }
          }
          catch (IOException e) {
            exceptions[0] = e;
          }
        }
      });
      if (exceptions[0] != null) {
        throw exceptions[0];
      }
    }

    if (filesToRecompile.length > 0) {
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          for (VirtualFile file : filesToRecompile) {
            if (file.isValid()) {
              loadInfoAndAddSourceForRecompilation(projectId, file);
            }
          }
        }
      });
    }
  }

  @Override
  public void updateOutputRootsLayout(Project project) {
    final TIntObjectHashMap<Pair<Integer, Integer>> map = buildOutputRootsLayout(new ProjectRef(project));
    final int projectId = getProjectId(project);
    synchronized (myProjectOutputRoots) {
      myProjectOutputRoots.put(projectId, map);
    }
  }

  private static File getOutputRootsFile() {
    return new File(CompilerPaths.getCompilerSystemDirectory(), "output_roots.dat");
  }

  private static File getFilePathsFile() {
    return new File(CompilerPaths.getCompilerSystemDirectory(), "file_paths.dat");
  }

  private static Map<String, SourceUrlClassNamePair> loadPathsToDelete(@Nullable final File file) {
    final Map<String, SourceUrlClassNamePair> map = new HashMap<>();
    try {
      if (file != null && file.length() > 0) {
        try (DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
          final int size = is.readInt();
          for (int i = 0; i < size; i++) {
            final String _outputPath = CompilerIOUtil.readString(is);
            final String srcUrl = CompilerIOUtil.readString(is);
            final String className = CompilerIOUtil.readString(is);
            map.put(FileUtil.toSystemIndependentName(_outputPath), new SourceUrlClassNamePair(srcUrl, className));
          }
        }
      }
    }
    catch (FileNotFoundException ignored) {
    }
    catch (IOException e) {
      LOG.info(e);
    }
    return map;
  }

  private void ensureOutputStorageInitialized() {
    if (myInitialized) {
      throw new IllegalArgumentException();
    }

    int i = readVersion();
    if (i != VERSION) {
      try {
        dropCache();

        FileUtil.writeToFile(getFileVersion(), String.valueOf(VERSION));
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    try {
      initOutputRootsFile();
    }
    catch (IOException e) {
      LOG.info(e);

      dropCache();

      try {
        initOutputRootsFile();
      }
      catch (IOException e1) {
        LOG.error(e1);
      }
    }
  }

  private void close() {
    boolean failed = false;
    try {
      myFilePathsEnumerator.close();
    }
    catch (IOException e) {
      LOG.error(e);
      failed = true;
    }

    try {
      myOutputRootsStorage.close();
    }
    catch (IOException e) {
      LOG.error(e);
      failed = true;
    }

    if (failed) {
      dropCache();
    }
  }

  private static void dropCache() {
    try {
      PersistentHashMap.deleteFilesStartingWith(getOutputRootsFile());
      PersistentHashMap.deleteFilesStartingWith(getFilePathsFile());
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  private static int readVersion() {
    File versionFile = getFileVersion();
    try {
      return StringUtil.parseInt(FileUtil.loadFile(versionFile), -1);
    }
    catch (IOException ignored) {
    }

    return -1;
  }

  private static File getFileVersion() {
    return new File(CompilerPaths.getCompilerSystemDirectory(), "tr_compiler_ver");
  }

  private TIntObjectHashMap<Pair<Integer, Integer>> buildOutputRootsLayout(ProjectRef projRef) {
    final TIntObjectHashMap<Pair<Integer, Integer>> map = new TIntObjectHashMap<>();
    for (Module module : ModuleManager.getInstance(projRef.get()).getModules()) {
      ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);

      final VirtualFile output = moduleCompilerPathsManager.getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
      final int first = output != null ? Math.abs(getFileId(output)) : -1;
      final VirtualFile testsOutput = moduleCompilerPathsManager.getCompilerOutput(TestContentFolderTypeProvider.getInstance());
      final int second = testsOutput != null ? Math.abs(getFileId(testsOutput)) : -1;
      map.put(getModuleId(module), new Pair<>(first, second));
    }
    return map;
  }

  private void initOutputRootsFile() throws IOException {
    myFilePathsEnumerator = new PersistentStringEnumerator(getFilePathsFile());

    myOutputRootsStorage = new PersistentHashMap<>(getOutputRootsFile(), EnumeratorIntegerDescriptor.INSTANCE, new DataExternalizer<TIntObjectHashMap<Pair<Integer, Integer>>>() {
      @Override
      public void save(DataOutput out, TIntObjectHashMap<Pair<Integer, Integer>> value) throws IOException {
        for (final TIntObjectIterator<Pair<Integer, Integer>> it = value.iterator(); it.hasNext(); ) {
          it.advance();
          DataInputOutputUtil.writeINT(out, it.key());
          final Pair<Integer, Integer> pair = it.value();
          DataInputOutputUtil.writeINT(out, pair.first);
          DataInputOutputUtil.writeINT(out, pair.second);
        }
      }

      @Override
      public TIntObjectHashMap<Pair<Integer, Integer>> read(DataInput in) throws IOException {
        final DataInputStream _in = (DataInputStream)in;
        final TIntObjectHashMap<Pair<Integer, Integer>> map = new TIntObjectHashMap<>();
        while (_in.available() > 0) {
          final int key = DataInputOutputUtil.readINT(_in);
          final int first = DataInputOutputUtil.readINT(_in);
          final int second = DataInputOutputUtil.readINT(_in);
          map.put(key, new Pair<>(first, second));
        }
        return map;
      }
    });

    myInitialized = true;
  }

  @Override
  public void dispose() {
    try {
      synchronized (myProjectOutputRoots) {
        myProjectOutputRoots.clear();
      }
    }
    finally {
      synchronized (myDataLock) {
        myOutputsToDelete.clear();
      }
    }

    close();
  }

  private static void savePathsToDelete(final File file, final Map<String, SourceUrlClassNamePair> outputs) {
    try {
      FileUtil.createParentDirs(file);
      final DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
      try {
        if (outputs != null) {
          os.writeInt(outputs.size());
          for (Map.Entry<String, SourceUrlClassNamePair> entry : outputs.entrySet()) {
            CompilerIOUtil.writeString(entry.getKey(), os);
            final SourceUrlClassNamePair pair = entry.getValue();
            CompilerIOUtil.writeString(pair.getSourceUrl(), os);
            CompilerIOUtil.writeString(pair.getClassName(), os);
          }
        }
        else {
          os.writeInt(0);
        }
      }
      finally {
        os.close();
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  public int getProjectId(Project project) {
    return cacheFilePath(CompilerPaths.getCompilerSystemDirectoryName(project));
  }

  private int getModuleId(Module module) {
    return cacheFilePath(module.getName().toLowerCase(Locale.US));
  }

  @Override
  public List<String> getCompiledClassNames(VirtualFile srcFile, Project project) {
    final TranslationSourceFileInfo info = TranslationSourceFileInfo.loadSourceInfo(srcFile);
    if (info == null) {
      return Collections.emptyList();
    }

    final ArrayList<String> result = new ArrayList<>();

    info.processOutputPaths(getProjectId(project), new Proc() {
      @Override
      public boolean execute(int projectId, String outputPath) {
        VirtualFile clsFile = LocalFileSystem.getInstance().findFileByPath(outputPath);
        if (clsFile != null) {
          TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(clsFile);
          if (outputInfo != null) {
            ContainerUtil.addIfNotNull(result, outputInfo.getClassName());
          }
        }
        return true;
      }
    });
    return result;
  }

  @Override
  public void scanSourceContent(final ProjectRef projRef, final Collection<VirtualFile> roots, final int totalRootCount, final boolean isNewRoots) {
    if (roots.isEmpty()) {
      return;
    }
    final int projectId = getProjectId(projRef.get());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Scanning source content for project projectId=" + projectId + "; url=" + projRef.get().getPresentableUrl());
    }

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(projRef.get()).getFileIndex();
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    int processed = 0;
    for (VirtualFile srcRoot : roots) {
      if (indicator != null) {
        projRef.get();
        indicator.setText2(srcRoot.getPresentableUrl());
        indicator.setFraction(++processed / (double)totalRootCount);
      }
      if (isNewRoots) {
        fileIndex.iterateContentUnderDirectory(srcRoot, new ContentIterator() {
          @Override
          public boolean processFile(final VirtualFile file) {
            if (!file.isDirectory()) {
              if (!isMarkedForRecompilation(projectId, Math.abs(getFileId(file)))) {
                final TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(file);
                if (srcInfo == null || srcInfo.getTimestamp(projectId) != file.getTimeStamp()) {
                  addSourceForRecompilation(projectId, file, srcInfo);
                }
              }
            }
            else {
              projRef.get();
            }
            return true;
          }
        });
      }
      else {
        final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        VfsUtilCore.visitChildrenRecursively(srcRoot, new VirtualFileVisitor() {
          @Override
          public boolean visitFile(@Nonnull VirtualFile file) {
            if (fileTypeManager.isFileIgnored(file)) {
              return false;
            }
            final int fileId = getFileId(file);
            if (fileId > 0 /*file is valid*/) {
              if (file.isDirectory()) {
                projRef.get();
              }
              else if (!isMarkedForRecompilation(projectId, fileId)) {
                final TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(file);
                if (srcInfo != null) {
                  addSourceForRecompilation(projectId, file, srcInfo);
                }
              }
            }
            return true;
          }
        });
      }
    }
  }

  @Override
  public void ensureInitializationCompleted(Project project, ProgressIndicator indicator) {
    final int id = getProjectId(project);
    synchronized (myAsyncScanLock) {
      while (myInitInProgress.containsKey(id)) {
        if (!project.isOpen() || project.isDisposed() || (indicator != null && indicator.isCanceled())) {
          // makes no sense to continue waiting
          break;
        }
        try {
          myAsyncScanLock.wait(500);
        }
        catch (InterruptedException ignored) {
          break;
        }
      }
    }
  }

  private void markOldOutputRoots(final ProjectRef projRef, final TIntObjectHashMap<Pair<Integer, Integer>> currentLayout) {
    final int projectId = getProjectId(projRef.get());

    final TIntHashSet rootsToMark = new TIntHashSet();
    synchronized (myProjectOutputRoots) {
      final TIntObjectHashMap<Pair<Integer, Integer>> oldLayout = myProjectOutputRoots.get(projectId);
      for (final TIntObjectIterator<Pair<Integer, Integer>> it = oldLayout.iterator(); it.hasNext(); ) {
        it.advance();
        final Pair<Integer, Integer> currentRoots = currentLayout.get(it.key());
        final Pair<Integer, Integer> oldRoots = it.value();
        if (shouldMark(oldRoots.first, currentRoots != null ? currentRoots.first : -1)) {
          rootsToMark.add(oldRoots.first);
        }
        if (shouldMark(oldRoots.second, currentRoots != null ? currentRoots.second : -1)) {
          rootsToMark.add(oldRoots.second);
        }
      }
    }

    for (TIntIterator it = rootsToMark.iterator(); it.hasNext(); ) {
      final int id = it.next();
      final VirtualFile outputRoot = findFileById(id);
      if (outputRoot != null) {
        processOldOutputRoot(projectId, outputRoot);
      }
    }
  }

  private static boolean shouldMark(Integer oldOutputRoot, Integer currentOutputRoot) {
    return oldOutputRoot != null && oldOutputRoot.intValue() > 0 && !Comparing.equal(oldOutputRoot, currentOutputRoot);
  }

  private void processOldOutputRoot(final int projectId, VirtualFile outputRoot) {
    // recursively mark all corresponding sources for recompilation
    VfsUtilCore.visitChildrenRecursively(outputRoot, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (!file.isDirectory()) {
          // todo: possible optimization - process only those outputs that are not marked for deletion yet
          final TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(file);
          if (outputInfo != null) {
            final String srcPath = outputInfo.getSourceFilePath();
            final VirtualFile srcFile = srcPath != null ? LocalFileSystem.getInstance().findFileByPath(srcPath) : null;
            if (srcFile != null) {
              loadInfoAndAddSourceForRecompilation(projectId, srcFile);
            }
          }
        }
        return true;
      }
    });
  }

  @Override
  public void scanSourcesForCompilableFiles(final Project project) {
    final int projectId = getProjectId(project);
    if (isSuspended(projectId)) {
      return;
    }
    startAsyncScan(projectId);
    StartupManager.getInstance(project).registerPostStartupActivity((ui) -> new Task.Backgroundable(project, CompilerBundle.message("compiler.initial.scanning.progress.text"), false) {
      @Override
      public void run(@Nonnull final ProgressIndicator indicator) {
        final ProjectRef projRef = new ProjectRef(project);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Initial sources scan for project hash=" + projectId + "; url=" + projRef.get().getPresentableUrl());
        }
        try {
          final IntermediateOutputCompiler[] compilers = CompilerManager.getInstance(projRef.get()).getCompilers(IntermediateOutputCompiler.class);

          final Set<VirtualFile> intermediateRoots = new HashSet<>();
          if (compilers.length > 0) {
            final Module[] modules = ReadAction.compute(() -> ModuleManager.getInstance(projRef.get()).getModules());
            for (IntermediateOutputCompiler compiler : compilers) {
              for (Module module : modules) {
                if (module.isDisposed() || module.getModuleDirUrl() == null) {
                  continue;
                }
                final VirtualFile outputRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(CompilerPaths.getGenerationOutputPath(compiler, module, false));
                if (outputRoot != null) {
                  intermediateRoots.add(outputRoot);
                }
                final VirtualFile testsOutputRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(CompilerPaths.getGenerationOutputPath(compiler, module, true));
                if (testsOutputRoot != null) {
                  intermediateRoots.add(testsOutputRoot);
                }
              }
            }
          }

          final List<VirtualFile> projectRoots = Arrays.asList(getRootsForScan(projRef.get()));
          final int totalRootsCount = projectRoots.size() + intermediateRoots.size();
          scanSourceContent(projRef, projectRoots, totalRootsCount, true);

          if (!intermediateRoots.isEmpty()) {
            final Consumer<VirtualFile> processor = file -> {
              if (!isMarkedForRecompilation(projectId, Math.abs(getFileId(file)))) {
                final TranslationSourceFileInfo srcInfo = TranslationSourceFileInfo.loadSourceInfo(file);
                if (srcInfo == null || srcInfo.getTimestamp(projectId) != file.getTimeStamp()) {
                  addSourceForRecompilation(projectId, file, srcInfo);
                }
              }
            };
            int processed = projectRoots.size();
            for (VirtualFile root : intermediateRoots) {
              projRef.get();
              indicator.setText2(root.getPresentableUrl());
              indicator.setFraction(++processed / (double)totalRootsCount);

              TranslationCompilerFilesMonitorVfsListener.processRecursively(root, false, processor);
            }
          }

          markOldOutputRoots(projRef, buildOutputRootsLayout(projRef));
        }
        catch (ProjectRef.ProjectClosedException swallowed) {
        }
        finally {
          terminateAsyncScan(projectId, false);
        }
      }
    }.queue());
  }

  private void terminateAsyncScan(int projectId, final boolean clearCounter) {
    synchronized (myAsyncScanLock) {
      int counter = myInitInProgress.remove(projectId);
      if (clearCounter) {
        myAsyncScanLock.notifyAll();
      }
      else {
        if (--counter > 0) {
          myInitInProgress.put(projectId, counter);
        }
        else {
          myAsyncScanLock.notifyAll();
        }
      }
    }
  }

  private void startAsyncScan(final int projectId) {
    synchronized (myAsyncScanLock) {
      int counter = myInitInProgress.get(projectId);
      counter = (counter > 0) ? counter + 1 : 1;
      myInitInProgress.put(projectId, counter);
      myAsyncScanLock.notifyAll();
    }
  }

  public boolean isIgnoredOrUnderIgnoredDirectory(ProjectManager projectManager, VirtualFile file) {
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    if (fileTypeManager.isFileIgnored(file)) {
      return true;
    }

    //optimization: if file is in content of some project it's definitely not ignored
    boolean isInContent = ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        for (Project project : projectManager.getOpenProjects()) {
          if (project.isInitialized() && ProjectRootManager.getInstance(project).getFileIndex().isInContent(file)) {
            return true;
          }
        }
        return false;
      }
    });
    if (isInContent) {
      return false;
    }

    VirtualFile current = file.getParent();
    while (current != null) {
      if (fileTypeManager.isFileIgnored(current)) {
        return true;
      }
      current = current.getParent();
    }
    return false;
  }

  public void loadInfoAndAddSourceForRecompilation(final int projectId, final VirtualFile srcFile) {
    addSourceForRecompilation(projectId, srcFile, TranslationSourceFileInfo.loadSourceInfo(srcFile));
  }

  public void addSourceForRecompilation(final int projectId, final VirtualFile srcFile, @Nullable final TranslationSourceFileInfo srcInfo) {
    final boolean alreadyMarked;
    synchronized (myDataLock) {
      TIntHashSet set = mySourcesToRecompile.get(projectId);
      if (set == null) {
        set = new TIntHashSet();
        mySourcesToRecompile.put(projectId, set);
      }
      alreadyMarked = !set.add(Math.abs(getFileId(srcFile)));
      if (!alreadyMarked && (LOG.isDebugEnabled() || DEBUG_MODE)) {
        final String message = "Scheduled recompilation " + srcFile.getPresentableUrl();
        LOG.debug(message);
        if (DEBUG_MODE) {
          System.out.println(message);
        }
      }
    }

    if (!alreadyMarked && srcInfo != null) {
      srcInfo.updateTimestamp(projectId, -1L);
      srcInfo.processOutputPaths(projectId, new ScheduleOutputsForDeletionProc(srcFile.getUrl()));
      TranslationSourceFileInfo.saveSourceInfo(srcFile, srcInfo);
    }
  }

  public void removeSourceForRecompilation(final int projectId, final int srcId) {
    synchronized (myDataLock) {
      TIntHashSet set = mySourcesToRecompile.get(projectId);
      if (set != null) {
        set.remove(srcId);
        if (set.isEmpty()) {
          mySourcesToRecompile.remove(projectId);
        }
      }
    }
  }

  private VirtualFile[] getRootsForScan(Project project) {
    List<VirtualFile> list = new ArrayList<>();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    List<TranslatingCompilerFilesMonitorHelper> extensions = TranslatingCompilerFilesMonitorHelper.EP_NAME.getExtensionList();
    for (Module module : modules) {
      for (TranslatingCompilerFilesMonitorHelper extension : extensions) {
        VirtualFile[] rootsForModule = extension.getRootsForModule(module);
        if (rootsForModule != null) {
          Collections.addAll(list, rootsForModule);
        }
      }

      VirtualFile[] contentFolderFiles = ModuleRootManager.getInstance(module).getContentFolderFiles(ContentFolderScopes.all(false));
      Collections.addAll(list, contentFolderFiles);
    }
    return VfsUtil.toVirtualFileArray(list);
  }

  @Override
  public boolean isMarkedForCompilation(Project project, VirtualFile file) {
    return isMarkedForRecompilation(getProjectId(project), getFileId(file));
  }

  private boolean isMarkedForRecompilation(int projectId, final int srcId) {
    synchronized (myDataLock) {
      final TIntHashSet set = mySourcesToRecompile.get(projectId);
      return set != null && set.contains(srcId);
    }
  }

  @FunctionalInterface
  public interface Proc {
    boolean execute(final int projectId, String outputPath);
  }

  public class ScheduleOutputsForDeletionProc implements Proc {
    private final String mySrcUrl;
    private final LocalFileSystem myFileSystem;
    @Nullable
    private VirtualFile myRootBeingDeleted;

    public ScheduleOutputsForDeletionProc(final String srcUrl) {
      mySrcUrl = srcUrl;
      myFileSystem = LocalFileSystem.getInstance();
    }

    public void setRootBeingDeleted(@Nullable VirtualFile rootBeingDeleted) {
      myRootBeingDeleted = rootBeingDeleted;
    }

    @Override
    public boolean execute(final int projectId, String outputPath) {
      final VirtualFile outFile = myFileSystem.findFileByPath(outputPath);
      if (outFile != null) { // not deleted yet
        if (myRootBeingDeleted != null && VfsUtil.isAncestor(myRootBeingDeleted, outFile, false)) {
          unmarkOutputPathForDeletion(projectId, outputPath);
        }
        else {
          final TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(outFile);
          final String classname = outputInfo != null ? outputInfo.getClassName() : null;
          markOutputPathForDeletion(projectId, outputPath, classname, mySrcUrl);
        }
      }
      return true;
    }
  }

  private void markOutputPathForDeletion(final int projectId, final String outputPath, final String classname, final String srcUrl) {
    final SourceUrlClassNamePair pair = new SourceUrlClassNamePair(srcUrl, classname);
    synchronized (myDataLock) {
      final Outputs outputs = myOutputsToDelete.get(projectId);
      try {
        outputs.put(outputPath, pair);
        if (LOG.isDebugEnabled() || DEBUG_MODE) {
          final String message = "ADD path to delete: " + outputPath + "; source: " + srcUrl;
          LOG.debug(message);
          if (DEBUG_MODE) {
            System.out.println(message);
          }
        }
      }
      finally {
        outputs.release();
      }
    }
  }

  public void unmarkOutputPathForDeletion(final int projectId, String outputPath) {
    synchronized (myDataLock) {
      final Outputs outputs = myOutputsToDelete.get(projectId);
      try {
        final SourceUrlClassNamePair val = outputs.remove(outputPath);
        if (val != null) {
          if (LOG.isDebugEnabled() || DEBUG_MODE) {
            final String message = "REMOVE path to delete: " + outputPath;
            LOG.debug(message);
            if (DEBUG_MODE) {
              System.out.println(message);
            }
          }
        }
      }
      finally {
        outputs.release();
      }
    }
  }

  public static int cacheFilePath(String filePath) {
    TranslatingCompilerFilesMonitorImpl monitor = (TranslatingCompilerFilesMonitorImpl)TranslatingCompilerFilesMonitor.getInstance();
    return monitor.cacheFilePath0(filePath);
  }

  public static String getFilePath(int id) {
    TranslatingCompilerFilesMonitorImpl monitor = (TranslatingCompilerFilesMonitorImpl)TranslatingCompilerFilesMonitor.getInstance();
    return monitor.getFilePath0(id);
  }

  private int cacheFilePath0(@Nonnull String filePath) {
    try {
      return myFilePathsEnumerator.enumerate(filePath);
    }
    catch (IOException e) {
      try {
        myFilePathsEnumerator.markCorrupted();
        dropCache();
      }
      catch (Exception ignored) {
      }
      LOG.warn(e);
      return -1;
    }
  }

  private String getFilePath0(int id) {
    try {
      return myFilePathsEnumerator.valueOf(id);
    }
    catch (IOException e) {
      try {
        myFilePathsEnumerator.markCorrupted();
        dropCache();
      }
      catch (Exception ignored) {
      }
      LOG.warn(e);
      return "";
    }
  }

  public static final class ProjectRef extends Ref<Project> {
    static class ProjectClosedException extends RuntimeException {
    }

    public ProjectRef(Project project) {
      super(project);
    }

    @Override
    public Project get() {
      final Project project = super.get();
      if (project != null && project.isDisposed()) {
        throw new ProjectClosedException();
      }
      return project;
    }
  }

  private static class Outputs {
    private boolean myIsDirty = false;
    @Nullable
    private final File myStoreFile;
    private final Map<String, SourceUrlClassNamePair> myMap;
    private final AtomicInteger myRefCount = new AtomicInteger(1);

    Outputs(@Nullable File storeFile, Map<String, SourceUrlClassNamePair> map) {
      myStoreFile = storeFile;
      myMap = map;
    }

    public Set<Map.Entry<String, SourceUrlClassNamePair>> getEntries() {
      return Collections.unmodifiableSet(myMap.entrySet());
    }

    public void put(String outputPath, SourceUrlClassNamePair pair) {
      if (myStoreFile == null) {
        return;
      }
      if (pair == null) {
        remove(outputPath);
      }
      else {
        myMap.put(outputPath, pair);
        myIsDirty = true;
      }
    }

    public SourceUrlClassNamePair remove(String outputPath) {
      if (myStoreFile == null) {
        return null;
      }
      final SourceUrlClassNamePair removed = myMap.remove(outputPath);
      myIsDirty |= removed != null;
      return removed;
    }

    void allocate() {
      myRefCount.incrementAndGet();
    }

    public void release() {
      if (myRefCount.decrementAndGet() == 0) {
        if (myIsDirty && myStoreFile != null) {
          savePathsToDelete(myStoreFile, myMap);
        }
      }
    }
  }

}
