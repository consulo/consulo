/*
 * Copyright 2013-2023 consulo.io
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
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.compiler.CompileDriver;
import consulo.compiler.*;
import consulo.compiler.generic.GenericCompiler;
import consulo.compiler.impl.internal.generic.GenericCompilerRunner;
import consulo.compiler.impl.internal.scope.CompositeScope;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.scope.FileSetCompileScope;
import consulo.compiler.util.CompilerUtil;
import consulo.compiler.util.ModuleCompilerUtil;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.Chunk;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2023-04-17
 */
@ExtensionImpl(order = "last")
public class BuiltInCompilerRunner implements CompilerRunner {
    private static final Logger LOG = Logger.getInstance(BuiltInCompilerRunner.class);

    private interface FileProcessingCompilerAdapterFactory {
        FileProcessingCompilerAdapter create(CompileContext context, FileProcessingCompiler compiler);
    }

    private static final FileProcessingCompilerAdapterFactory FILE_PROCESSING_COMPILER_ADAPTER_FACTORY = FileProcessingCompilerAdapter::new;
    private static final FileProcessingCompilerAdapterFactory FILE_PACKAGING_COMPILER_ADAPTER_FACTORY =
        (context, compiler) -> new PackagingCompilerAdapter(context, (PackagingCompiler) compiler);

    private static final Predicate<Compiler> SOURCE_PROCESSING_ONLY = compiler -> compiler instanceof SourceProcessingCompiler;

    private boolean ourDebugMode = false;

    @Nonnull
    private final Project myProject;
    private final CompilerManager myCompilerManager;

    @Inject
    public BuiltInCompilerRunner(@Nonnull Project project, CompilerManager compilerManager) {
        myProject = project;
        myCompilerManager = compilerManager;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("BuiltIn");
    }

    @Nonnull
    @Override
    public Image getBuildIcon() {
        return PlatformIconGroup.actionsCompile();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    @RequiredReadAction
    public void cleanUp(CompileDriver compileDriver, CompileContextEx context) {
        CompilerManager compilerManager = CompilerManager.getInstance(myProject);

        VirtualFile[] allSources = compilerManager.createProjectCompileScope().getFiles(null);

        for (Compiler compiler : compilerManager.getCompilers(Compiler.class)) {
            try {
                if (compiler instanceof GeneratingCompiler generatingCompiler) {
                    StateCache<ValidityState> cache = getGeneratingCompilerCache(generatingCompiler);
                    Iterator<File> fileIterator = cache.getFilesIterator();
                    while (fileIterator.hasNext()) {
                        context.getProgressIndicator().checkCanceled();
                        compileDriver.deleteFile(fileIterator.next());
                    }
                }
                else if (compiler instanceof TranslatingCompiler translatingCompiler) {
                    List<Trinity<File, String, Boolean>> toDelete = new ArrayList<>();
                    Application.get().runReadAction(() -> TranslatingCompilerFilesMonitor.getInstance().collectFiles(context,
                        translatingCompiler,
                        Arrays.<VirtualFile>asList(allSources).iterator(),
                        true /*pass true to make sure that every source in scope file is processed*/,
                        false /*important! should pass false to enable collection of files to delete*/,
                        new ArrayList<>(),
                        toDelete
                    ));
                    for (Trinity<File, String, Boolean> trinity : toDelete) {
                        context.getProgressIndicator().checkCanceled();
                        File file = trinity.getFirst();
                        compileDriver.deleteFile(file);
                    }
                }
            }
            catch (IOException e) {
                LOG.info(e);
            }
        }
    }

    @Override
    public boolean build(
        CompileDriver compileDriver,
        CompileContextEx context,
        boolean isRebuild,
        boolean forceCompile,
        boolean onlyCheckStatus
    ) throws ExitException {
        Predicate<Compiler> compilerFilter = compileDriver.getCompilerFilter();

        CompilerManager compilerManager = myCompilerManager;
        GenericCompilerRunner runner = new GenericCompilerRunner(
            context,
            isRebuild,
            onlyCheckStatus,
            compilerManager.getCompilers(GenericCompiler.class, compilerFilter)
        );

        boolean didSomething = generateSources(compileDriver, compilerManager, context, forceCompile, onlyCheckStatus);

        didSomething |= invokeFileProcessingCompilers(
            compileDriver,
            compilerManager,
            context,
            SourceInstrumentingCompiler.class,
            FILE_PROCESSING_COMPILER_ADAPTER_FACTORY,
            forceCompile,
            true,
            onlyCheckStatus
        );

        didSomething |= invokeFileProcessingCompilers(
            compileDriver,
            compilerManager,
            context,
            SourceProcessingCompiler.class,
            FILE_PROCESSING_COMPILER_ADAPTER_FACTORY,
            forceCompile,
            true,
            onlyCheckStatus
        );

        CompileScope intermediateSources = compileDriver.attachIntermediateOutputDirectories(
            new CompositeScope(CompileScope.EMPTY_ARRAY) {
                @Override
                @Nonnull
                public Module[] getAffectedModules() {
                    return context.getCompileScope().getAffectedModules();
                }
            },
            SOURCE_PROCESSING_ONLY
        );
        context.addScope(intermediateSources);

        didSomething |= translate(compileDriver, context, compilerManager, forceCompile, isRebuild, onlyCheckStatus);

        didSomething |= invokeFileProcessingCompilers(compileDriver, compilerManager,
            context,
            ClassInstrumentingCompiler.class,
            FILE_PROCESSING_COMPILER_ADAPTER_FACTORY,
            isRebuild,
            false,
            onlyCheckStatus
        );
        didSomething |= runner.invokeCompilers(GenericCompiler.CompileOrderPlace.CLASS_INSTRUMENTING);

        // explicitly passing forceCompile = false because in scopes that is narrower than ProjectScope it is impossible
        // to understand whether the class to be processed is in scope or not. Otherwise compiler may process its items even if
        // there were changes in completely independent files.
        didSomething |= invokeFileProcessingCompilers(compileDriver, compilerManager,
            context,
            ClassPostProcessingCompiler.class,
            FILE_PROCESSING_COMPILER_ADAPTER_FACTORY,
            isRebuild,
            false,
            onlyCheckStatus
        );
        didSomething |= runner.invokeCompilers(GenericCompiler.CompileOrderPlace.CLASS_POST_PROCESSING);

        didSomething |= invokeFileProcessingCompilers(compileDriver, compilerManager,
            context,
            PackagingCompiler.class,
            FILE_PACKAGING_COMPILER_ADAPTER_FACTORY,
            isRebuild,
            false,
            onlyCheckStatus
        );
        didSomething |= runner.invokeCompilers(GenericCompiler.CompileOrderPlace.PACKAGING);

        didSomething |= invokeFileProcessingCompilers(
            compileDriver,
            compilerManager,
            context,
            Validator.class,
            FILE_PROCESSING_COMPILER_ADAPTER_FACTORY,
            forceCompile,
            true,
            onlyCheckStatus
        );
        didSomething |= runner.invokeCompilers(GenericCompiler.CompileOrderPlace.VALIDATING);
        return didSomething;
    }


    private boolean generateSources(
        CompileDriver compileDriver,
        CompilerManager compilerManager,
        CompileContextEx context,
        boolean forceCompile,
        boolean onlyCheckStatus
    ) throws ExitException {
        boolean didSomething = false;

        SourceGeneratingCompiler[] sourceGenerators =
            compilerManager.getCompilers(SourceGeneratingCompiler.class, compileDriver.getCompilerFilter());
        for (SourceGeneratingCompiler sourceGenerator : sourceGenerators) {
            if (context.getProgressIndicator().isCanceled()) {
                throw new ExitException(ExitStatus.CANCELLED);
            }

            boolean generatedSomething = generateOutput(compileDriver, context, sourceGenerator, forceCompile, onlyCheckStatus);

            if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                throw new ExitException(ExitStatus.ERRORS);
            }
            didSomething |= generatedSomething;
        }
        return didSomething;
    }

    private boolean translate(
        CompileDriver compileDriver,
        CompileContextEx context,
        CompilerManager compilerManager,
        boolean forceCompile,
        boolean isRebuild,
        boolean onlyCheckStatus
    ) throws ExitException {
        boolean didSomething = false;

        TranslatingCompiler[] original = compilerManager.getCompilers(TranslatingCompiler.class, compileDriver.getCompilerFilter());

        List<Chunk<Module>> sortedChunks =
            Collections.unmodifiableList(Application.get().runReadAction((Supplier<List<Chunk<Module>>>) () -> {
                ModuleManager moduleManager = ModuleManager.getInstance(myProject);
                return ModuleCompilerUtil.getSortedModuleChunks(myProject, Arrays.asList(moduleManager.getModules()));
            }));

        DumbService dumbService = DumbService.getInstance(myProject);
        try {
            Set<Module> processedModules = new HashSet<>();
            VirtualFile[] snapshot = null;
            Map<Chunk<Module>, Collection<VirtualFile>> chunkMap = new HashMap<>();
            int total = 0;
            int processed = 0;
            for (Chunk<Module> currentChunk : sortedChunks) {
                TranslatingCompiler[] translators = original.clone();
                myProject.getApplication().getExtensionPoint(CompilerSorter.class)
                    .forEach(compilerSorter -> compilerSorter.sort(currentChunk, translators, TranslatingCompiler.class));
                TranslatorsOutputSink sink = new TranslatorsOutputSink(context, translators);
                Set<FileType> generatedTypes = new HashSet<>();
                Collection<VirtualFile> chunkFiles = chunkMap.get(currentChunk);
                Set<VirtualFile> filesToRecompile = new HashSet<>();
                Set<VirtualFile> allDependent = new HashSet<>();
                try {
                    int round = 0;
                    boolean compiledSomethingForThisChunk = false;
                    Collection<VirtualFile> dependentFiles = Collections.emptyList();
                    Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> dependencyFilter =
                        new DependentClassesCumulativeFilter();

                    do {
                        for (
                            int currentCompiler = 0, translatorsLength = translators.length;
                            currentCompiler < translatorsLength;
                            currentCompiler++
                        ) {
                            sink.setCurrentCompilerIndex(currentCompiler);
                            TranslatingCompiler compiler = translators[currentCompiler];
                            if (context.getProgressIndicator().isCanceled()) {
                                throw new ExitException(ExitStatus.CANCELLED);
                            }

                            dumbService.waitForSmartMode();

                            if (snapshot == null
                                || ContainerUtil.intersects(generatedTypes, compilerManager.getRegisteredInputTypes(compiler))) {
                                // rescan snapshot if previously generated files may influence the input of this compiler
                                Collection<VirtualFile> prevSnapshot = round > 0 && snapshot != null
                                    ? Arrays.asList(snapshot)
                                    : Collections.<VirtualFile>emptySet();
                                snapshot = Application.get().runReadAction(
                                    (Supplier<VirtualFile[]>) () -> context.getCompileScope().getFiles(null)
                                );
                                recalculateChunkToFilesMap(context, sortedChunks, snapshot, chunkMap);
                                if (round == 0) {
                                    chunkFiles = chunkMap.get(currentChunk);
                                }
                                else {
                                    Set<VirtualFile> newFiles = new HashSet<>(chunkMap.get(currentChunk));
                                    newFiles.removeAll(prevSnapshot);
                                    newFiles.removeAll(chunkFiles);
                                    if (!newFiles.isEmpty()) {
                                        List<VirtualFile> merged = new ArrayList<>(chunkFiles.size() + newFiles.size());
                                        merged.addAll(chunkFiles);
                                        merged.addAll(newFiles);
                                        chunkFiles = merged;
                                    }
                                }
                                total = snapshot.length * translatorsLength;
                            }

                            CompileContextEx _context;
                            if (compiler instanceof IntermediateOutputCompiler _compiler) {
                                // wrap compile context so that output goes into intermediate directories
                                _context = new CompileContextExDelegate(context) {
                                    @Override
                                    public VirtualFile getModuleOutputDirectory(Module module) {
                                        return getGenerationOutputDir(compileDriver, _compiler, module, false);
                                    }

                                    @Override
                                    public VirtualFile getModuleOutputDirectoryForTests(Module module) {
                                        return getGenerationOutputDir(compileDriver, _compiler, module, true);
                                    }
                                };
                            }
                            else {
                                _context = context;
                            }
                            boolean compiledSomething = compileSources(
                                compileDriver,
                                _context,
                                currentChunk,
                                compiler,
                                chunkFiles,
                                round != 0 || forceCompile,
                                isRebuild,
                                onlyCheckStatus,
                                sink
                            );

                            processed += chunkFiles.size();
                            _context.getProgressIndicator().setFraction(((double) processed) / total);

                            if (compiledSomething) {
                                generatedTypes.addAll(compilerManager.getRegisteredOutputTypes(compiler));
                            }

                            didSomething |= compiledSomething;
                            compiledSomethingForThisChunk |= didSomething;

                            if (_context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                                break; // break the loop over compilers
                            }
                        }

                        boolean hasUnprocessedTraverseRoots = context.getDependencyCache().hasUnprocessedTraverseRoots();
                        if (!isRebuild && (compiledSomethingForThisChunk || hasUnprocessedTraverseRoots)) {
                            Set<VirtualFile> compiledWithErrors = CacheUtils.getFilesCompiledWithErrors(context);
                            filesToRecompile.removeAll(sink.getCompiledSources());
                            filesToRecompile.addAll(compiledWithErrors);

                            dependentFiles = CacheUtils.findDependentFiles(context, compiledWithErrors, dependencyFilter);
                            if (!processedModules.isEmpty()) {
                                for (Iterator<VirtualFile> it = dependentFiles.iterator(); it.hasNext(); ) {
                                    VirtualFile next = it.next();
                                    Module module = context.getModuleByFile(next);
                                    if (module != null && processedModules.contains(module)) {
                                        it.remove();
                                    }
                                }
                            }

                            if (ourDebugMode) {
                                if (!dependentFiles.isEmpty()) {
                                    for (VirtualFile dependentFile : dependentFiles) {
                                        System.out.println("FOUND TO RECOMPILE: " + dependentFile.getPresentableUrl());
                                    }
                                }
                                else {
                                    System.out.println("NO FILES TO RECOMPILE");
                                }
                            }

                            if (!dependentFiles.isEmpty()) {
                                filesToRecompile.addAll(dependentFiles);
                                allDependent.addAll(dependentFiles);
                                if (context.getProgressIndicator().isCanceled()
                                    || context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                                    break;
                                }
                                List<VirtualFile> filesInScope = getFilesInScope(context, currentChunk, dependentFiles);
                                if (filesInScope.isEmpty()) {
                                    break;
                                }
                                context.getDependencyCache().clearTraverseRoots();
                                chunkFiles = filesInScope;
                                total += chunkFiles.size() * translators.length;
                            }

                            didSomething |= (hasUnprocessedTraverseRoots != context.getDependencyCache().hasUnprocessedTraverseRoots());
                        }

                        round++;
                    }
                    while (!dependentFiles.isEmpty() && context.getMessageCount(CompilerMessageCategory.ERROR) == 0);

                    if (CompilerManager.MAKE_ENABLED) {
                        if (!context.getProgressIndicator().isCanceled()) {
                            // when cancelled pretend nothing was compiled and next compile will compile everything from the scratch
                            ProgressIndicator indicator = context.getProgressIndicator();
                            DependencyCache cache = context.getDependencyCache();

                            indicator.pushState();
                            indicator.setTextValue(CompilerLocalize.progressUpdatingCaches());
                            indicator.setText2Value(LocalizeValue.empty());

                            cache.update();

                            indicator.setTextValue(CompilerLocalize.progressSavingCaches());
                            cache.resetState();
                            processedModules.addAll(currentChunk.getNodes());
                            indicator.popState();
                        }
                    }

                    if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                        throw new ExitException(ExitStatus.ERRORS);
                    }

                }
                catch (CacheCorruptedException e) {
                    LOG.info(e);
                    context.requestRebuildNextTime(e.getMessage());
                }
                finally {
                    int errorCount = context.getMessageCount(CompilerMessageCategory.ERROR);
                    if (errorCount != 0) {
                        filesToRecompile.addAll(allDependent);
                    }
                    if (filesToRecompile.size() > 0) {
                        sink.add(
                            null,
                            Collections.<TranslatingCompiler.OutputItem>emptyList(),
                            VirtualFileUtil.toVirtualFileArray(filesToRecompile)
                        );
                    }
                    if (errorCount == 0) {
                        // perform update only if there were no errors, so it is guaranteed that the file was processd by all neccesary compilers
                        sink.flushPostponedItems();
                    }
                }
            }
        }
        catch (ProcessCanceledException e) {
            ProgressManager.getInstance().executeNonCancelableSection(() -> {
                try {
                    Collection<VirtualFile> deps = CacheUtils.findDependentFiles(context, Collections.<VirtualFile>emptySet(), null);
                    if (deps.size() > 0) {
                        TranslatingCompilerFilesMonitor.getInstance().update(
                            context,
                            null,
                            Collections.<TranslatingCompiler.OutputItem>emptyList(),
                            VirtualFileUtil.toVirtualFileArray(deps)
                        );
                    }
                }
                catch (IOException | CacheCorruptedException | ExitException ignored) {
                    LOG.info(ignored);
                }
            });
            throw e;
        }
        finally {
            compileDriver.dropDependencyCache(context);
            if (didSomething) {
                TranslatingCompilerFilesMonitor.getInstance().updateOutputRootsLayout(myProject);
            }
        }
        return didSomething;
    }


    private static List<VirtualFile> getFilesInScope(CompileContextEx context, Chunk<Module> chunk, Collection<VirtualFile> files) {
        List<VirtualFile> filesInScope = new ArrayList<>(files.size());
        Application.get().runReadAction(() -> {
            for (VirtualFile file : files) {
                if (context.getCompileScope().belongs(file.getUrl())) {
                    Module module = context.getModuleByFile(file);
                    if (chunk.getNodes().contains(module)) {
                        filesInScope.add(file);
                    }
                }
            }
        });
        return filesInScope;
    }

    private static void recalculateChunkToFilesMap(
        CompileContextEx context,
        List<Chunk<Module>> allChunks,
        VirtualFile[] snapshot,
        Map<Chunk<Module>, Collection<VirtualFile>> chunkMap
    ) {
        Map<Module, List<VirtualFile>> moduleToFilesMap = CompilerUtil.buildModuleToFilesMap(context, snapshot);
        for (Chunk<Module> moduleChunk : allChunks) {
            List<VirtualFile> files = Collections.emptyList();
            for (Module module : moduleChunk.getNodes()) {
                List<VirtualFile> moduleFiles = moduleToFilesMap.get(module);
                if (moduleFiles != null) {
                    files = ContainerUtil.concat(files, moduleFiles);
                }
            }
            chunkMap.put(moduleChunk, files);
        }
    }

    private boolean compileSources(
        CompileDriver compileDriver,
        CompileContextEx context,
        Chunk<Module> moduleChunk,
        TranslatingCompiler compiler,
        Collection<VirtualFile> srcSnapshot,
        boolean forceCompile,
        boolean isRebuild,
        boolean onlyCheckStatus,
        TranslatingCompiler.OutputSink sink
    ) throws ExitException {
        Set<VirtualFile> toCompile = new HashSet<>();
        List<Trinity<File, String, Boolean>> toDelete = new ArrayList<>();
        context.getProgressIndicator().pushState();

        boolean[] wereFilesDeleted = {false};
        try {
            Application.get().runReadAction(() -> TranslatingCompilerFilesMonitor.getInstance().collectFiles(
                context,
                compiler,
                srcSnapshot.iterator(),
                forceCompile,
                isRebuild,
                toCompile,
                toDelete
            ));

            if (onlyCheckStatus) {
                if (toDelete.isEmpty() && toCompile.isEmpty()) {
                    return false;
                }
                if (LOG.isDebugEnabled() || ourDebugMode) {
                    if (!toDelete.isEmpty()) {
                        StringBuilder message = new StringBuilder();
                        message.append("Found items to delete, compiler ").append(compiler.getDescription());
                        for (Trinity<File, String, Boolean> trinity : toDelete) {
                            message.append("\n").append(trinity.getFirst());
                        }
                        LOG.debug(message.toString());
                        if (ourDebugMode) {
                            System.out.println(message);
                        }
                    }
                    if (!toCompile.isEmpty()) {
                        String message = "Found items to compile, compiler " + compiler.getDescription();
                        LOG.debug(message);
                        if (ourDebugMode) {
                            System.out.println(message);
                        }
                    }
                }
                throw new ExitException(ExitStatus.CANCELLED);
            }

            if (!toDelete.isEmpty()) {
                try {
                    wereFilesDeleted[0] = syncOutputDir(compileDriver, context, toDelete);
                }
                catch (CacheCorruptedException e) {
                    LOG.info(e);
                    context.requestRebuildNextTime(e.getMessage());
                }
            }

            if ((wereFilesDeleted[0] || !toCompile.isEmpty()) && context.getMessageCount(CompilerMessageCategory.ERROR) == 0) {
                compiler.compile(context, moduleChunk, VirtualFileUtil.toVirtualFileArray(toCompile), sink);
            }
        }
        finally {
            context.getProgressIndicator().popState();
        }
        return !toCompile.isEmpty() || wereFilesDeleted[0];
    }


    private static boolean syncOutputDir(
        CompileDriver compileDriver,
        CompileContextEx context,
        Collection<Trinity<File, String, Boolean>> toDelete
    ) throws CacheCorruptedException {
        DependencyCache dependencyCache = context.getDependencyCache();
        boolean isTestMode = Application.get().isUnitTestMode();

        List<File> filesToRefresh = new ArrayList<>();
        boolean[] wereFilesDeleted = {false};
        CompilerUtil.runInContext(
            context,
            CompilerLocalize.progressSynchronizingOutputDirectory().get(),
            () -> {
                long start = System.currentTimeMillis();
                try {
                    for (Trinity<File, String, Boolean> trinity : toDelete) {
                        File outputPath = trinity.getFirst();
                        context.getProgressIndicator().checkCanceled();
                        context.getProgressIndicator().setText2Value(LocalizeValue.ofNullable(outputPath.getPath()));
                        filesToRefresh.add(outputPath);
                        if (isTestMode) {
                            LOG.assertTrue(outputPath.exists());
                        }
                        boolean result = compileDriver.deleteFile(outputPath);
                        if (!result) {
                            continue;
                        }
                        wereFilesDeleted[0] = true;

                        dependencyCache.syncOutDir(trinity);
                    }
                }
                finally {
                    CompilerUtil.logDuration("Sync output directory", System.currentTimeMillis() - start);
                    CompilerUtil.refreshIOFiles(filesToRefresh);
                }
            }
        );
        return wereFilesDeleted[0];
    }

    private boolean invokeFileProcessingCompilers(
        CompileDriver compileDriver,
        CompilerManager compilerManager,
        CompileContextEx context,
        Class<? extends FileProcessingCompiler> fileProcessingCompilerClass,
        FileProcessingCompilerAdapterFactory factory,
        boolean forceCompile,
        boolean checkScope,
        boolean onlyCheckStatus
    ) throws ExitException {
        boolean didSomething = false;
        FileProcessingCompiler[] compilers =
            compilerManager.getCompilers(fileProcessingCompilerClass, compileDriver.getCompilerFilter());
        if (compilers.length > 0) {
            try {
                CacheDeferredUpdater cacheUpdater = new CacheDeferredUpdater();
                try {
                    for (FileProcessingCompiler compiler : compilers) {
                        if (context.getProgressIndicator().isCanceled()) {
                            throw new ExitException(ExitStatus.CANCELLED);
                        }

                        CompileContextEx _context = context;
                        if (compiler instanceof IntermediateOutputCompiler _compiler) {
                            _context = new CompileContextExDelegate(context) {
                                @Override
                                public VirtualFile getModuleOutputDirectory(Module module) {
                                    return getGenerationOutputDir(compileDriver, _compiler, module, false);
                                }

                                @Override
                                public VirtualFile getModuleOutputDirectoryForTests(Module module) {
                                    return getGenerationOutputDir(compileDriver, _compiler, module, true);
                                }
                            };
                        }

                        boolean processedSomething =
                            processFiles(factory.create(_context, compiler), forceCompile, checkScope, onlyCheckStatus, cacheUpdater);

                        if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                            throw new ExitException(ExitStatus.ERRORS);
                        }

                        didSomething |= processedSomething;
                    }
                }
                finally {
                    cacheUpdater.doUpdate();
                }
            }
            catch (IOException e) {
                LOG.info(e);
                context.requestRebuildNextTime(e.getMessage());
                throw new ExitException(ExitStatus.ERRORS);
            }
            catch (ProcessCanceledException | ExitException e) {
                throw e;
            }
            catch (Exception e) {
                context.addMessage(
                    CompilerMessageCategory.ERROR,
                    CompilerLocalize.compilerErrorException(e.getMessage()).get(),
                    null,
                    -1,
                    -1
                );
                LOG.error(e);
            }
        }

        return didSomething;
    }


    private boolean generateOutput(
        CompileDriver compileDriver,
        CompileContextEx context,
        GeneratingCompiler compiler,
        boolean forceGenerate,
        boolean onlyCheckStatus
    ) throws ExitException {
        GeneratingCompiler.GenerationItem[] allItems = compiler.getGenerationItems(context);
        List<GeneratingCompiler.GenerationItem> toGenerate = new ArrayList<>();
        List<File> filesToRefresh = new ArrayList<>();
        List<File> generatedFiles = new ArrayList<>();
        List<Module> affectedModules = new ArrayList<>();
        try {
            StateCache<ValidityState> cache = getGeneratingCompilerCache(compiler);
            Set<File> pathsToRemove = new HashSet<>(cache.getFiles());

            Map<GeneratingCompiler.GenerationItem, File> itemToOutputPathMap = new HashMap<>();
            IOException[] ex = {null};
            Application.get().runReadAction(() -> {
                for (GeneratingCompiler.GenerationItem item : allItems) {
                    Module itemModule = item.getModule();
                    String outputDirPath = CompilerPaths.getGenerationOutputPath(compiler, itemModule, item.isTestSource());
                    File outputPath = new File(outputDirPath, item.getPath());
                    itemToOutputPathMap.put(item, outputPath);

                    try {
                        ValidityState savedState = cache.getState(outputPath);

                        if (forceGenerate || savedState == null || !savedState.equalsTo(item.getValidityState())) {
                            String outputPathUrl = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, outputPath.getPath());
                            if (context.getCompileScope().belongs(outputPathUrl)) {
                                toGenerate.add(item);
                            }
                            else {
                                pathsToRemove.remove(outputPath);
                            }
                        }
                        else {
                            pathsToRemove.remove(outputPath);
                        }
                    }
                    catch (IOException e) {
                        ex[0] = e;
                    }
                }
            });
            if (ex[0] != null) {
                throw ex[0];
            }

            if (onlyCheckStatus) {
                if (toGenerate.isEmpty() && pathsToRemove.isEmpty()) {
                    return false;
                }
                if (LOG.isDebugEnabled()) {
                    if (!toGenerate.isEmpty()) {
                        LOG.debug("Found items to generate, compiler " + compiler.getDescription());
                    }
                    if (!pathsToRemove.isEmpty()) {
                        LOG.debug("Found paths to remove, compiler " + compiler.getDescription());
                    }
                }
                throw new ExitException(ExitStatus.CANCELLED);
            }

            if (!pathsToRemove.isEmpty()) {
                CompilerUtil.runInContext(
                    context,
                    CompilerLocalize.progressSynchronizingOutputDirectory().get(),
                    () -> {
                        for (File file : pathsToRemove) {
                            boolean result = compileDriver.deleteFile(file);
                            boolean deleted = result;
                            if (deleted) {
                                cache.remove(file);
                                filesToRefresh.add(file);
                            }
                        }
                    }
                );
            }

            Map<Module, Set<GeneratingCompiler.GenerationItem>> moduleToItemMap = buildModuleToGenerationItemMap(
                toGenerate.toArray(new GeneratingCompiler.GenerationItem[toGenerate.size()]));
            List<Module> modules = new ArrayList<>(moduleToItemMap.size());
            for (Module module : moduleToItemMap.keySet()) {
                modules.add(module);
            }
            ModuleCompilerUtil.sortModules(myProject, modules);

            for (Module module : modules) {
                CompilerUtil.runInContext(context, "Generating output from " + compiler.getDescription(), () -> {
                    Set<GeneratingCompiler.GenerationItem> items = moduleToItemMap.get(module);
                    if (items != null && !items.isEmpty()) {
                        GeneratingCompiler.GenerationItem[][] productionAndTestItems = splitGenerationItems(items);
                        for (GeneratingCompiler.GenerationItem[] _items : productionAndTestItems) {
                            if (_items.length == 0) {
                                continue;
                            }
                            VirtualFile outputDir = getGenerationOutputDir(compileDriver, compiler, module, _items[0].isTestSource());
                            GeneratingCompiler.GenerationItem[] successfullyGenerated = compiler.generate(context, _items, outputDir);

                            CompilerUtil.runInContext(
                                context,
                                CompilerLocalize.progressUpdatingCaches().get(),
                                () -> {
                                    if (successfullyGenerated.length > 0) {
                                        affectedModules.add(module);
                                    }
                                    for (GeneratingCompiler.GenerationItem item : successfullyGenerated) {
                                        File file = itemToOutputPathMap.get(item);
                                        cache.update(file, item.getValidityState());
                                        filesToRefresh.add(file);
                                        generatedFiles.add(file);
                                        context.getProgressIndicator().setText2Value(LocalizeValue.ofNullable(file.getPath()));
                                    }
                                }
                            );
                        }
                    }
                });
            }
        }
        catch (IOException e) {
            LOG.info(e);
            context.requestRebuildNextTime(e.getMessage());
            throw new ExitException(ExitStatus.ERRORS);
        }
        finally {
            if (!generatedFiles.isEmpty()) {
                List<VirtualFile> vFiles = DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
                    ArrayList<VirtualFile> vFiles1 = new ArrayList<>(generatedFiles.size());
                    for (File generatedFile : generatedFiles) {
                        VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(generatedFile);
                        if (vFile != null) {
                            vFiles1.add(vFile);
                        }
                    }
                    return vFiles1;
                });
                if (forceGenerate) {
                    context.addScope(new FileSetCompileScope(vFiles, affectedModules.toArray(new Module[affectedModules.size()])));
                }
                context.markGenerated(vFiles);
            }
        }
        return !toGenerate.isEmpty() || !filesToRefresh.isEmpty();
    }

    private VirtualFile getGenerationOutputDir(
        CompileDriver compileDriver,
        IntermediateOutputCompiler compiler,
        Module module,
        boolean forTestSources
    ) {
        return compileDriver.getGenerationOutputDir(compiler, module, forTestSources);
    }

    private static GeneratingCompiler.GenerationItem[][] splitGenerationItems(Set<GeneratingCompiler.GenerationItem> items) {
        List<GeneratingCompiler.GenerationItem> production = new ArrayList<>();
        List<GeneratingCompiler.GenerationItem> tests = new ArrayList<>();
        for (GeneratingCompiler.GenerationItem item : items) {
            if (item.isTestSource()) {
                tests.add(item);
            }
            else {
                production.add(item);
            }
        }
        return new GeneratingCompiler.GenerationItem[][]{production.toArray(new GeneratingCompiler.GenerationItem[production.size()]),
            tests.toArray(new GeneratingCompiler.GenerationItem[tests.size()])};
    }


    private FileProcessingCompilerStateCache getFileProcessingCompilerCache(FileProcessingCompiler compiler) throws IOException {
        return CompilerCacheManager.getInstance(myProject).getFileProcessingCompilerCache(compiler);
    }

    private StateCache<ValidityState> getGeneratingCompilerCache(GeneratingCompiler compiler) throws IOException {
        return CompilerCacheManager.getInstance(myProject).getGeneratingCompilerCache(compiler);
    }

    private boolean processFiles(
        FileProcessingCompilerAdapter adapter,
        boolean forceCompile,
        boolean checkScope,
        boolean onlyCheckStatus,
        CacheDeferredUpdater cacheUpdater
    ) throws ExitException, IOException {
        CompileContextEx context = (CompileContextEx) adapter.getCompileContext();
        FileProcessingCompilerStateCache cache = getFileProcessingCompilerCache(adapter.getCompiler());
        FileProcessingCompiler.ProcessingItem[] items = adapter.getProcessingItems();
        if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
            return false;
        }
        if (LOG.isDebugEnabled() && items.length > 0) {
            LOG.debug("Start processing files by " + adapter.getCompiler().getDescription());
        }
        CompileScope scope = context.getCompileScope();
        List<FileProcessingCompiler.ProcessingItem> toProcess = new ArrayList<>();
        Set<File> allFiles = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
        IOException[] ex = {null};
        DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
            try {
                for (FileProcessingCompiler.ProcessingItem item : items) {
                    File file = item.getFile();
                    allFiles.add(file);
                    if (!forceCompile && cache.getTimestamp(file) == file.lastModified()) {
                        ValidityState state = cache.getExtState(file);
                        ValidityState itemState = item.getValidityState();
                        if (state != null ? state.equalsTo(itemState) : itemState == null) {
                            continue;
                        }
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Adding item to process: " + file.getPath() + "; saved ts= " + cache.getTimestamp(file) + "; VFS ts=" + file.lastModified());
                    }
                    toProcess.add(item);
                }
            }
            catch (IOException e) {
                ex[0] = e;
            }
        });

        if (ex[0] != null) {
            throw ex[0];
        }

        Collection<File> files = cache.getFiles();
        List<File> urlsToRemove = new ArrayList<>();
        if (!files.isEmpty()) {
            CompilerUtil.runInContext(
                context,
                CompilerLocalize.progressProcessingOutdatedFiles().get(),
                () -> {
                    Application.get().runReadAction(() -> {
                        for (File file : files) {
                            if (!allFiles.contains(file)) {
                                String fileUrl = VirtualFileUtil.urlToPath(file.getPath());
                                if (!checkScope || scope.belongs(fileUrl)) {
                                    urlsToRemove.add(file);
                                }
                            }
                        }
                    });
                    if (!onlyCheckStatus && !urlsToRemove.isEmpty()) {
                        for (File file : urlsToRemove) {
                            adapter.processOutdatedItem(context, file, cache.getExtState(file));
                            cache.remove(file);
                        }
                    }
                }
            );
        }

        if (onlyCheckStatus) {
            if (urlsToRemove.isEmpty() && toProcess.isEmpty()) {
                return false;
            }
            if (LOG.isDebugEnabled()) {
                if (!urlsToRemove.isEmpty()) {
                    LOG.debug("Found urls to remove, compiler " + adapter.getCompiler().getDescription());
                    for (File file : urlsToRemove) {
                        LOG.debug("\t" + file.getPath());
                    }
                }
                if (!toProcess.isEmpty()) {
                    LOG.debug("Found items to compile, compiler " + adapter.getCompiler().getDescription());
                    for (FileProcessingCompiler.ProcessingItem item : toProcess) {
                        LOG.debug("\t" + item.getFile().getPath());
                    }
                }
            }
            throw new ExitException(ExitStatus.CANCELLED);
        }

        if (toProcess.isEmpty()) {
            return false;
        }

        FileProcessingCompiler.ProcessingItem[] processed =
            adapter.process(toProcess.toArray(new FileProcessingCompiler.ProcessingItem[toProcess.size()]));

        if (processed.length == 0) {
            return true;
        }
        CompilerUtil.runInContext(
            context,
            CompilerLocalize.progressUpdatingCaches().get(),
            () -> {
                //List<File> vFiles = new ArrayList<>(processed.length);
                for (FileProcessingCompiler.ProcessingItem aProcessed : processed) {
                    File file = aProcessed.getFile();
                    //vFiles.add(file);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("\tFile processed " + file.getPath() + "; ts=" + file.lastModified());
                    }

                    //String path = file.getPath();
                    //String outputDir = myOutputFinder.lookupOutputPath(path);
                    //if (outputDir != null) {
                    //  context.updateZippedOuput(outputDir, path.substring(outputDir.length() + 1));
                    //}
                }
      /*LocalFileSystem.getInstance().refreshFiles(vFiles);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Files after VFS refresh:");
        for (File file : vFiles) {
          LOG.debug("\t" + file.getPath() + "; ts=" + file.lastModified());
        }
      } */
                for (FileProcessingCompiler.ProcessingItem item : processed) {
                    cacheUpdater.addFileForUpdate(item, cache);
                }
            }
        );
        return true;
    }

    private static Map<Module, Set<GeneratingCompiler.GenerationItem>> buildModuleToGenerationItemMap(GeneratingCompiler.GenerationItem[] items) {
        Map<Module, Set<GeneratingCompiler.GenerationItem>> map = new HashMap<>();
        for (GeneratingCompiler.GenerationItem item : items) {
            Module module = item.getModule();
            LOG.assertTrue(module != null);
            Set<GeneratingCompiler.GenerationItem> itemSet = map.get(module);
            if (itemSet == null) {
                itemSet = new HashSet<>();
                map.put(module, itemSet);
            }
            itemSet.add(item);
        }
        return map;
    }
}
