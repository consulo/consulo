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
package consulo.compiler.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.compiler.*;
import consulo.content.ContentIterator;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.index.io.data.DataOutputStream;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SLRUCache;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>
 * A source file is scheduled for recompilation if
 * 1. its timestamp has changed
 * 2. one of its corresponding output files was deleted
 * 3. output root of containing module has changed
 * <p>
 * An output file is scheduled for deletion if:
 * 1. corresponding source file has been scheduled for recompilation (see above)
 * 2. corresponding source file has been deleted
 *
 * @author Eugene Zhuravlev
 * @since 2008-06-03
 */
@Singleton
@ServiceImpl
public class TranslatingCompilerFilesMonitorImpl extends TranslatingCompilerFilesMonitor implements Disposable {

    private static final Logger LOG = Logger.getInstance(TranslatingCompilerFilesMonitorImpl.class);

    public static final boolean DEBUG_MODE = false;

    private static final Key<Map<String, VirtualFile>> SOURCE_FILES_CACHE = Key.create("_source_url_to_vfile_cache_");

    protected final Object myDataLock = new Object();

    private final Set<Integer> mySuspendedProjects = new HashSet<>(); // projectId for all projects that should not be monitored

    protected final IntObjectMap<IntSet> mySourcesToRecompile = IntMaps.newIntObjectHashMap();

    // Map: projectId -> Map{output path -> [sourceUrl; className]}
    protected final SLRUCache<Integer, Outputs> myOutputsToDelete = new SLRUCache<Integer, Outputs>(3, 3) {
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
        public Outputs createValue(Integer projectId) {
            VirtualFile projectDir = VirtualFileManager.getInstance().findFileById(projectId);
            if (projectDir == null) {
                return new Outputs(null, new LinkedHashMap<>());
            }

            Project project = null;
            for (Project temp : ProjectManager.getInstance().getOpenProjects()) {
                if (projectDir.equals(temp.getBaseDir())) {
                    project = temp;
                    break;
                }
            }
            if (project == null) {
                return new Outputs(null, new LinkedHashMap<>());
            }

            File compilerCacheDir = CompilerPaths.getCacheStoreDirectory(project);
            File storeFile = compilerCacheDir.exists() ? new File(compilerCacheDir, "paths_to_delete.dat") : null;
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

    private final Map<Integer, Integer> myInitInProgress = new HashMap<>(); // projectId for successfully initialized projects
    private final Object myAsyncScanLock = new Object();

    protected boolean myForceCompiling;

    @Inject
    public TranslatingCompilerFilesMonitorImpl() {
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

            TranslationCompilerProjectMonitor.getInstance(project).removeCompileOutputInfoFile();
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
            return outputFileInfo.getSourceFile();
        }
        return null;
    }

    @Override
    public void collectFiles(
        CompileContext context,
        final TranslatingCompiler compiler,
        Iterator<VirtualFile> scopeSrcIterator,
        boolean forceCompile,
        final boolean isRebuild,
        Collection<VirtualFile> toCompile,
        Collection<Trinity<File, String, Boolean>> toDelete
    ) {
        final Project project = context.getProject();
        final int projectId = getProjectId(project);
        final CompilerManager configuration = CompilerManager.getInstance(project);
        final boolean _forceCompile = forceCompile || isRebuild || myForceCompiling;
        final Set<VirtualFile> selectedForRecompilation = new HashSet<>();
        synchronized (myDataLock) {
            final IntSet pathsToRecompile = mySourcesToRecompile.get(projectId);
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
                                        final String message =
                                            "Found zombie entry (output is marked, but source is present and up-to-date): " + outputPath;
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

    @Override
    public void update(
        final CompileContext context,
        @Nullable final String outputRoot,
        final Collection<TranslatingCompiler.OutputItem> successfullyCompiled,
        final VirtualFile[] filesToRecompile
    ) throws IOException {
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
                                        final String className =
                                            outputRoot == null ? null : dependencyCache.relativePathToQName(
                                                outputPath.substring(outputRoot.length()),
                                                '/'
                                            );
                                        if (isSourceValid) {
                                            srcInfo.addOutputPath(projectId, outputFile);
                                            TranslationOutputFileInfo.saveOutputInfo(
                                                outputFile,
                                                new TranslationOutputFileInfo(sourceFile, className)
                                            );
                                        }
                                        else {
                                            markOutputPathForDeletion(projectId, outputFile, className, sourceFile.getUrl());
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
                            if (fileStamp > compilationStartStamp && !((CompileContextEx)context).isGenerated(file)
                                || forceRecompile.contains(file)) {
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
    @RequiredReadAction
    public void updateOutputRootsLayout(Project project) {
        TranslationCompilerProjectMonitor.getInstance(project).updateCompileOutputInfoFile();
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

    private void close() {
    }

    protected void invalidate() {
    }

    @Override
    public void dispose() {
        synchronized (myDataLock) {
            myOutputsToDelete.clear();
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
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            throw new IllegalArgumentException("there no base directory: " + project.getBasePath());
        }

        return FileBasedIndex.getFileId(baseDir);
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
            public boolean execute(int projectId, VirtualFile clsFile) {
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
    public void scanSourceContent(
        final ProjectRef projRef,
        final Collection<VirtualFile> roots,
        final int totalRootCount,
        final boolean isNewRoots
    ) {
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
                VirtualFileUtil.visitChildrenRecursively(srcRoot, new VirtualFileVisitor() {
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

    protected void markOldOutputRoots(final ProjectRef projRef, final Map<String, Couple<String>> currentLayout) {
        final int projectId = getProjectId(projRef.get());

        Set<VirtualFile> rootsToMark = new HashSet<>();

        Map<String, Couple<String>> lastOutputRootsLayout =
            TranslationCompilerProjectMonitor.getInstance(projRef.get()).getLastOutputRootsLayout();

        for (Map.Entry<String, Couple<String>> last : lastOutputRootsLayout.entrySet()) {
            Couple<String> current = currentLayout.get(last.getKey());

            // module was removed, renamed, etc - remove old source info
            if (current == null) {
                addIfDirectoryExists(last.getValue().getFirst(), rootsToMark);
                addIfDirectoryExists(last.getValue().getSecond(), rootsToMark);
            }
            else {
                String lastOutput = last.getValue().getFirst();
                if (lastOutput != null && !lastOutput.equals(current.getFirst())) {
                    addIfDirectoryExists(lastOutput, rootsToMark);
                }

                String lastTestOutput = last.getValue().getSecond();
                if (lastTestOutput != null && !lastTestOutput.equals(current.getSecond())) {
                    addIfDirectoryExists(lastTestOutput, rootsToMark);
                }
            }
        }

        for (VirtualFile outputRoot : rootsToMark) {
            processOldOutputRoot(projectId, outputRoot);
        }
    }

    private static void addIfDirectoryExists(String url, Set<VirtualFile> rootsToRemove) {
        if (url == null) {
            return;
        }

        VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(url);
        if (fileByUrl != null) {
            rootsToRemove.add(fileByUrl);
        }
    }

    private void processOldOutputRoot(final int projectId, VirtualFile outputRoot) {
        // recursively mark all corresponding sources for recompilation
        VirtualFileUtil.visitChildrenRecursively(outputRoot, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (!file.isDirectory()) {
                    // todo: possible optimization - process only those outputs that are not marked for deletion yet
                    final TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(file);
                    if (outputInfo != null) {
                        final VirtualFile srcFile = outputInfo.getSourceFile();
                        if (srcFile != null) {
                            loadInfoAndAddSourceForRecompilation(projectId, srcFile);
                        }
                    }
                }
                return true;
            }
        });
    }

    public void runScan(ProgressIndicator indicator, Project project) {
        int projectId = getProjectId(project);

        if (isSuspended(projectId)) {
            return;
        }

        indicator.setIndeterminate(false);


        final ProjectRef projRef = new ProjectRef(project);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial sources scan for project hash=" + projectId + "; url=" + projRef.get().getPresentableUrl());
        }
        try {
            final IntermediateOutputCompiler[] compilers =
                CompilerManager.getInstance(projRef.get()).getCompilers(IntermediateOutputCompiler.class);

            final Set<VirtualFile> intermediateRoots = new HashSet<>();
            if (compilers.length > 0) {
                final Module[] modules = ReadAction.compute(() -> ModuleManager.getInstance(projRef.get()).getModules());
                for (IntermediateOutputCompiler compiler : compilers) {
                    for (Module module : modules) {
                        if (module.isDisposed() || module.getModuleDirUrl() == null) {
                            continue;
                        }
                        final VirtualFile outputRoot =
                            LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(CompilerPaths.getGenerationOutputPath(compiler, module, false));
                        if (outputRoot != null) {
                            intermediateRoots.add(outputRoot);
                        }
                        final VirtualFile testsOutputRoot =
                            LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(CompilerPaths.getGenerationOutputPath(compiler, module, true));
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

            markOldOutputRoots(projRef, TranslationCompilerProjectMonitor.getInstance(projRef.get()).buildOutputRootsLayout());
        }
        catch (ProjectRef.ProjectClosedException ignored) {
        }
        finally {
            terminateAsyncScan(projectId, false);
        }
    }

    protected void terminateAsyncScan(int projectId, final boolean clearCounter) {
        synchronized (myAsyncScanLock) {
            Integer counter = ObjectUtil.notNull(myInitInProgress.remove(projectId), 0);

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

    public void startAsyncScan(Project project) {
        startAsyncScan(getProjectId(project));
    }

    protected void startAsyncScan(final int projectId) {
        synchronized (myAsyncScanLock) {
            int counter = myInitInProgress.getOrDefault(projectId, 0);
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
        boolean isInContent = ApplicationManager.getApplication().runReadAction(new Supplier<Boolean>() {
            @Override
            public Boolean get() {
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

    public void addSourceForRecompilation(
        final int projectId,
        final VirtualFile srcFile,
        @Nullable final TranslationSourceFileInfo srcInfo
    ) {
        final boolean alreadyMarked;
        synchronized (myDataLock) {
            IntSet set = mySourcesToRecompile.get(projectId);
            if (set == null) {
                set = IntSets.newHashSet();
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
            IntSet set = mySourcesToRecompile.get(projectId);
            if (set != null) {
                set.remove(srcId);
                if (set.isEmpty()) {
                    mySourcesToRecompile.remove(projectId);
                }
            }
        }
    }

    protected VirtualFile[] getRootsForScan(Project project) {
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

            VirtualFile[] contentFolderFiles =
                ModuleRootManager.getInstance(module).getContentFolderFiles(LanguageContentFolderScopes.all(false));
            Collections.addAll(list, contentFolderFiles);
        }
        return VirtualFileUtil.toVirtualFileArray(list);
    }

    @Override
    public boolean isMarkedForCompilation(Project project, VirtualFile file) {
        return isMarkedForRecompilation(getProjectId(project), getFileId(file));
    }

    private boolean isMarkedForRecompilation(int projectId, final int srcId) {
        synchronized (myDataLock) {
            final IntSet set = mySourcesToRecompile.get(projectId);
            return set != null && set.contains(srcId);
        }
    }

    @FunctionalInterface
    public interface Proc {
        boolean execute(final int projectId, VirtualFile outputPath);
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
        public boolean execute(final int projectId, VirtualFile outFile) {
            if (outFile != null) { // not deleted yet
                if (myRootBeingDeleted != null && VirtualFileUtil.isAncestor(myRootBeingDeleted, outFile, false)) {
                    unmarkOutputPathForDeletion(projectId, outFile.getPath());
                }
                else {
                    final TranslationOutputFileInfo outputInfo = TranslationOutputFileInfo.loadOutputInfo(outFile);
                    final String classname = outputInfo != null ? outputInfo.getClassName() : null;
                    markOutputPathForDeletion(projectId, outFile, classname, mySrcUrl);
                }
            }
            return true;
        }
    }

    private void markOutputPathForDeletion(final int projectId, final VirtualFile outputPath, final String classname, final String srcUrl) {
        final SourceUrlClassNamePair pair = new SourceUrlClassNamePair(srcUrl, classname);
        synchronized (myDataLock) {
            final Outputs outputs = myOutputsToDelete.get(projectId);
            try {
                outputs.put(outputPath.getPath(), pair);
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
