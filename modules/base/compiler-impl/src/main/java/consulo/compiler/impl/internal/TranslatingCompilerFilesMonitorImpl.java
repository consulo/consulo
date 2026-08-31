/*
 * Copyright 2013-2026 consulo.io
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
import consulo.application.AccessRule;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.compiler.*;
import consulo.compiler.impl.internal.state.OutputSourceInfo;
import consulo.compiler.impl.internal.state.ProjectCompilerState;
import consulo.compiler.util.CompilerUtil;
import consulo.component.extension.ExtensionPoint;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
public class TranslatingCompilerFilesMonitorImpl extends TranslatingCompilerFilesMonitor {
    private static final Logger LOG = Logger.getInstance(TranslatingCompilerFilesMonitorImpl.class);

    private final Object myDataLock = new Object();
    private final Set<Project> mySuspendedProjects = new HashSet<>();

    private final Map<Project, Integer> myInitInProgress = new HashMap<>();
    private final Object myAsyncScanLock = new Object();

    @Inject
    public TranslatingCompilerFilesMonitorImpl() {
    }

    public static String normalizePath(String path) {
        return FileUtil.toSystemIndependentName(path);
    }

    public static String pathOf(VirtualFile file) {
        return normalizePath(file.getPath());
    }

    @Override
    public void suspendProject(Project project) {
        synchronized (myDataLock) {
            if (!mySuspendedProjects.add(project)) {
                return;
            }
            FileUtil.createIfDoesntExist(CompilerPaths.getRebuildMarkerFile(project));

            ProjectCompilerState.getInstance(project).clearDirty();

            TranslationCompilerProjectMonitor.getInstance(project).removeCompileOutputInfoFile();
        }
    }

    public File getGeneratedPath(Project project) {
        return CompilerPaths.getGeneratedDataDirectory(project);
    }

    @Override
    public void watchProject(Project project) {
        synchronized (myDataLock) {
            mySuspendedProjects.remove(project);
        }
    }

    @Override
    public boolean isSuspended(Project project) {
        synchronized (myDataLock) {
            return mySuspendedProjects.contains(project);
        }
    }

    @Override
    public void collectFiles(
        CompileContext context,
        TranslatingCompiler compiler,
        Iterator<Path> scopeSrcIterator,
        boolean forceCompile,
        boolean isRebuild,
        Collection<Path> toCompile,
        Collection<Trinity<File, String, Boolean>> toDelete
    ) {
        Project project = context.getProject();
        ProjectCompilerState state = ProjectCompilerState.getInstance(project);
        CompilerManager configuration = CompilerManager.getInstance(project);
        boolean force = forceCompile || isRebuild;
        Set<String> selectedForRecompilation = Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);

        if (force || state.hasDirtyPaths()) {
            while (scopeSrcIterator.hasNext()) {
                Path file = scopeSrcIterator.next();
                String path = normalizePath(file.toString());
                boolean dirty = state.isDirty(path);
                if (force || dirty) {
                    if (compiler.isCompilableFile(file, context) && !configuration.isExcludedFromCompilation(file)) {
                        toCompile.add(file);
                        selectedForRecompilation.add(path);
                        if (!dirty) {
                            state.markSourceDirty(path);
                        }
                    }
                }
            }
        }

        // it is important that files to delete are collected after the files to compile (see what happens if forceCompile == true)
        if (!isRebuild) {
            List<String> zombieEntries = new ArrayList<>();
            for (Pair<String, OutputSourceInfo> entry : state.getOutputsToDelete()) {
                String outputPath = entry.getFirst();
                OutputSourceInfo info = entry.getSecond();
                String sourcePath = info.sourcePath();

                boolean sourcePresent = Files.exists(Path.of(sourcePath));
                if (sourcePresent) {
                    if (!compiler.isCompilableFile(Path.of(sourcePath), context)) {
                        continue; // do not collect files that were compiled by another compiler
                    }
                    if (!selectedForRecompilation.contains(sourcePath)) {
                        if (!state.isDirty(sourcePath)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found zombie entry (output is marked, but source is present and up-to-date): " + outputPath);
                            }
                            zombieEntries.add(outputPath);
                        }
                        continue;
                    }
                }
                if (Files.exists(Path.of(outputPath))) {
                    toDelete.add(new Trinity<>(new File(outputPath), info.className(), sourcePresent));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found file to delete: " + outputPath);
                    }
                }
                else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found zombie entry marked for deletion: " + outputPath);
                    }
                    zombieEntries.add(outputPath);
                }
            }
            for (String path : zombieEntries) {
                state.unscheduleOutputDeletion(path);
            }
        }
    }

    @Override
    public void update(
        CompileContext context,
        @Nullable String outputRoot,
        Collection<TranslatingCompiler.OutputItem> successfullyCompiled,
        Collection<Path> filesToRecompile
    ) throws IOException {
        Project project = context.getProject();
        ProjectCompilerState state = ProjectCompilerState.getInstance(project);

        if (!successfullyCompiled.isEmpty()) {
            DependencyCache dependencyCache = ((CompileContextEx) context).getDependencyCache();
            String normalizedOutputRoot = outputRoot == null ? null : normalizePath(outputRoot);

            Map<String, Map<String, String>> perSourceOutputs = new LinkedHashMap<>();
            Map<String, Path> sourceFiles = new HashMap<>();
            Set<String> forceRecompile = new HashSet<>();

            for (TranslatingCompiler.OutputItem item : successfullyCompiled) {
                Path sourceFile = item.sourceFile();
                boolean isSourceValid = Files.exists(sourceFile);
                String sourcePath = normalizePath(sourceFile.toString());

                if (isSourceValid && !perSourceOutputs.containsKey(sourcePath)) {
                    perSourceOutputs.put(sourcePath, new LinkedHashMap<>());
                    sourceFiles.put(sourcePath, sourceFile);
                }

                String rawOutputPath = item.outputPath();
                if (rawOutputPath != null) { // can be null for package-info
                    String outputPath = normalizePath(rawOutputPath);
                    if (!FileUtil.pathsEqual(outputPath, sourcePath)) {
                        String className = normalizedOutputRoot == null
                            ? null
                            : dependencyCache.relativePathToQName(outputPath.substring(normalizedOutputRoot.length()), '/');
                        if (isSourceValid) {
                            if (Files.exists(Path.of(outputPath))) {
                                perSourceOutputs.get(sourcePath).put(outputPath, className);
                            }
                            else {
                                LOG.warn("TranslatingCompilerFilesMonitor.update(): output file does not exist \"" + outputPath + "\"");
                                forceRecompile.add(sourcePath);
                            }
                        }
                        else {
                            state.scheduleOutputDeletion(outputPath, sourcePath, className);
                        }
                    }
                }
            }

            long compilationStartStamp = ((CompileContextEx) context).getStartCompilationStamp();
            for (Map.Entry<String, Map<String, String>> entry : perSourceOutputs.entrySet()) {
                String sourcePath = entry.getKey();
                Path sourceFile = sourceFiles.get(sourcePath);

                long fileStamp = CompilerUtil.lastModified(sourceFile);
                state.setCompiled(sourcePath, fileStamp, entry.getValue());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unschedule recompilation (successfully compiled) " + sourcePath);
                }
                if (fileStamp > compilationStartStamp && !((CompileContextEx) context).isGenerated(sourceFile)
                    || forceRecompile.contains(sourcePath)) {
                    // changes were made during compilation, need to re-schedule compilation
                    state.markSourceDirty(sourcePath);
                }
            }
        }

        for (Path file : filesToRecompile) {
            if (Files.exists(file)) {
                state.markSourceDirty(normalizePath(file.toString()));
            }
        }
    }

    @RequiredReadAction
    public void updateOutputRootsLayout(Project project) {
        TranslationCompilerProjectMonitor.getInstance(project).updateCompileOutputInfoFile();
    }

    @Override
    public List<String> getCompiledClassNames(Path srcFile, Project project) {
        return ProjectCompilerState.getInstance(project).getClassNames(normalizePath(srcFile.toString()));
    }

    public void scanSourceContent(
        ProjectRef projectRef,
        Collection<Path> roots,
        int totalRootCount,
        boolean isNewRoots
    ) {
        if (roots.isEmpty()) {
            return;
        }
        Project project = projectRef.get();
        ProjectCompilerState state = ProjectCompilerState.getInstance(project);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Scanning source content for project url=" + project.getPresentableUrl());
        }

        Set<String> excludedPaths = isNewRoots ? AccessRule.read(() -> CompilerPathsEx.getExcludedPaths(projectRef.get())) : Set.of();

        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        int processed = 0;
        for (Path srcRoot : roots) {
            projectRef.get();
            if (indicator != null) {
                indicator.setText2(LocalizeValue.of(srcRoot.toString()));
                indicator.setFraction(++processed / (double) totalRootCount);
            }
            if (isNewRoots) {
                scanRoot(state, srcRoot, excludedPaths);
            }
            else {
                String rootPrefix = normalizePath(srcRoot.toString());
                List<String> knownSources = new ArrayList<>();
                state.processSourcePaths(path -> {
                    if (FileUtil.startsWith(path, rootPrefix)) {
                        knownSources.add(path);
                    }
                    return true;
                });
                for (String path : knownSources) {
                    state.markSourceDirty(path);
                }
            }
        }
    }

    private static void scanRoot(ProjectCompilerState state, Path root, Set<String> excludedPaths) {
        if (!Files.isDirectory(root)) {
            return;
        }
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path name = dir.getFileName();
                    if (name != null && fileTypeManager.isFileIgnored(name.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (excludedPaths.contains(normalizePath(dir.toString()))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (fileTypeManager.isFileIgnored(file.getFileName().toString())) {
                        return FileVisitResult.CONTINUE;
                    }
                    processScannedFile(state, normalizePath(file.toString()), attrs.lastModifiedTime().toMillis());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            LOG.warn("Failed to scan root " + root, e);
        }
    }

    private static void processScannedFile(ProjectCompilerState state, String path, long diskStamp) {
        if (state.isDirty(path)) {
            return;
        }
        if (state.getStamp(path) != diskStamp) {
            state.markSourceDirty(path);
            return;
        }
        for (String output : state.getOutputs(path)) {
            if (!Files.exists(Path.of(output))) {
                state.markSourceDirty(path);
                return;
            }
        }
    }

    private static void detectDeletedSources(ProjectCompilerState state) {
        List<String> deleted = new ArrayList<>();
        state.processSourcePaths(path -> {
            if (!Files.exists(Path.of(path))) {
                deleted.add(path);
            }
            return true;
        });
        for (String path : deleted) {
            state.removeSource(path, null);
        }
    }

    public void ensureInitializationCompleted(Project project, ProgressIndicator indicator) {
        synchronized (myAsyncScanLock) {
            while (myInitInProgress.containsKey(project)) {
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

    protected void markOldOutputRoots(ProjectRef projRef, Map<String, Couple<String>> currentLayout) {
        Project project = projRef.get();
        ProjectCompilerState state = ProjectCompilerState.getInstance(project);

        Set<String> rootsToMark = new LinkedHashSet<>();

        Map<String, Couple<String>> lastOutputRootsLayout =
            TranslationCompilerProjectMonitor.getInstance(project).getLastOutputRootsLayout();

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

        for (String outputRoot : rootsToMark) {
            processOldOutputRoot(state, outputRoot);
        }
    }

    private static void addIfDirectoryExists(@Nullable String url, Set<String> rootsToMark) {
        if (url == null) {
            return;
        }
        String path = normalizePath(VirtualFileUtil.urlToPath(url));
        if (Files.isDirectory(Path.of(path))) {
            rootsToMark.add(path);
        }
    }

    private static void processOldOutputRoot(ProjectCompilerState state, String outputRootPath) {
        try {
            Files.walkFileTree(Path.of(outputRootPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    OutputSourceInfo info = state.getOutputInfo(normalizePath(file.toString()));
                    if (info != null) {
                        state.markSourceDirty(info.sourcePath());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            LOG.warn("Failed to scan old output root " + outputRootPath, e);
        }
    }

    public void runScan(ProgressIndicator indicator, Project project) {
        if (isSuspended(project)) {
            terminateAsyncScan(project, false);
            return;
        }

        indicator.setIndeterminate(false);

        ProjectRef projRef = new ProjectRef(project);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial sources scan for project url=" + projRef.get().getPresentableUrl());
        }
        try {
            IntermediateOutputCompiler[] compilers =
                CompilerManager.getInstance(projRef.get()).getCompilers(IntermediateOutputCompiler.class);

            Set<Path> intermediateRoots = new LinkedHashSet<>();
            if (compilers.length > 0) {
                Module[] modules = AccessRule.read(() -> ModuleManager.getInstance(projRef.get()).getModules());
                for (IntermediateOutputCompiler compiler : compilers) {
                    for (Module module : modules) {
                        if (module.isDisposed() || module.getModuleDirUrl() == null) {
                            continue;
                        }
                        intermediateRoots.add(Path.of(CompilerPaths.getGenerationOutputPath(compiler, module, false)));
                        intermediateRoots.add(Path.of(CompilerPaths.getGenerationOutputPath(compiler, module, true)));
                    }
                }
            }

            List<Path> projectRoots = AccessRule.read(() -> getRootsForScan(projRef.get()));
            int totalRootsCount = projectRoots.size() + intermediateRoots.size();
            scanSourceContent(projRef, projectRoots, totalRootsCount, true);

            if (!intermediateRoots.isEmpty()) {
                ProjectCompilerState state = ProjectCompilerState.getInstance(projRef.get());
                int processed = projectRoots.size();
                for (Path root : intermediateRoots) {
                    projRef.get();
                    indicator.setText2(LocalizeValue.of(root.toString()));
                    indicator.setFraction(++processed / (double) totalRootsCount);

                    scanRoot(state, root, Set.of());
                }
            }

            detectDeletedSources(ProjectCompilerState.getInstance(projRef.get()));

            markOldOutputRoots(projRef, AccessRule.read(
                () -> TranslationCompilerProjectMonitor.getInstance(projRef.get()).buildOutputRootsLayout()));
        }
        catch (ProjectRef.ProjectClosedException ignored) {
        }
        finally {
            terminateAsyncScan(project, false);
        }
    }

    protected void terminateAsyncScan(Project project, boolean clearCounter) {
        synchronized (myAsyncScanLock) {
            Integer removed = myInitInProgress.remove(project);
            int counter = removed == null ? 0 : removed;

            if (clearCounter) {
                myAsyncScanLock.notifyAll();
            }
            else {
                if (--counter > 0) {
                    myInitInProgress.put(project, counter);
                }
                else {
                    myAsyncScanLock.notifyAll();
                }
            }
        }
    }

    public void startAsyncScan(Project project) {
        synchronized (myAsyncScanLock) {
            int counter = myInitInProgress.getOrDefault(project, 0);
            counter = (counter > 0) ? counter + 1 : 1;
            myInitInProgress.put(project, counter);
            myAsyncScanLock.notifyAll();
        }
    }

    @RequiredReadAction
    protected List<Path> getRootsForScan(Project project) {
        List<Path> list = new ArrayList<>();
        Module[] modules = ModuleManager.getInstance(project).getModules();
        ExtensionPoint<TranslatingCompilerFilesMonitorHelper> extensionPoint =
            project.getApplication().getExtensionPoint(TranslatingCompilerFilesMonitorHelper.class);
        for (Module module : modules) {
            extensionPoint.forEach(extension -> {
                Path[] rootsForModule = extension.getRootsForModule(module);
                if (rootsForModule != null) {
                    Collections.addAll(list, rootsForModule);
                }
            });

            for (String url : ModuleRootManager.getInstance(module).getContentFolderUrls(LanguageContentFolderScopes.all(false))) {
                list.add(Path.of(VirtualFileUtil.urlToPath(url)));
            }
        }
        return list;
    }

    @Override
    public boolean isMarkedForCompilation(Project project, Path file) {
        return ProjectCompilerState.getInstance(project).isDirty(normalizePath(file.toString()));
    }
}
