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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.compiler.CompileDriver;
import consulo.compiler.Compiler;
import consulo.compiler.*;
import consulo.compiler.generic.GenericCompiler;
import consulo.compiler.impl.internal.generic.GenericCompilerRunner;
import consulo.compiler.impl.internal.scope.CompositeScope;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.scope.FileSetCompileScope;
import consulo.compiler.util.CompilerUtil;
import consulo.compiler.util.ModuleCompilerUtil;
import consulo.component.ProcessCanceledException;
import consulo.compiler.TranslatingCompilerFilesMonitor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.DumbService;
import consulo.project.Project;
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
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17/04/2023
 */
@ExtensionImpl(order = "last")
public class BuiltInCompilerRunner implements CompilerRunner {
    private static final Logger LOG = Logger.getInstance(BuiltInCompilerRunner.class);

    private interface FileProcessingCompilerAdapterFactory {
        FileProcessingCompilerAdapter create(CompileContext context, FileProcessingCompiler compiler);
    }

    private static final FileProcessingCompilerAdapterFactory FILE_PROCESSING_COMPILER_ADAPTER_FACTORY = FileProcessingCompilerAdapter::new;
    private static final FileProcessingCompilerAdapterFactory FILE_PACKAGING_COMPILER_ADAPTER_FACTORY =
        (context, compiler) -> new PackagingCompilerAdapter(
            context,
            (PackagingCompiler)compiler
        );

    private static final Predicate<Compiler> SOURCE_PROCESSING_ONLY = compiler -> compiler instanceof SourceProcessingCompiler;

    private boolean ourDebugMode = false;

    private final Project myProject;
    private final CompilerManager myCompilerManager;

    @Inject
    public BuiltInCompilerRunner(Project project, CompilerManager compilerManager) {
        myProject = project;
        myCompilerManager = compilerManager;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("BuiltIn");
    }

    @Override
    public boolean isAvailable(CompileContextEx context) {
        return true;
    }

    @Override
    public void cleanUp(CompileDriver compileDriver, CompileContextEx context) {
        CompilerManager compilerManager = CompilerManager.getInstance(myProject);

        final VirtualFile[] allSources = compilerManager.createProjectCompileScope().getFiles(null);

        for (final Compiler compiler : compilerManager.getCompilers(Compiler.class)) {
            try {
                if (compiler instanceof GeneratingCompiler) {
                    final StateCache<ValidityState> cache = getGeneratingCompilerCache((GeneratingCompiler)compiler);
                    final Iterator<File> fileIterator = cache.getFilesIterator();
                    while (fileIterator.hasNext()) {
                        context.getProgressIndicator().checkCanceled();
                        compileDriver.deleteFile(fileIterator.next());
                    }
                }
                else if (compiler instanceof TranslatingCompiler) {
                    final ArrayList<Trinity<File, String, Boolean>> toDelete = new ArrayList<>();
                    ApplicationManager.getApplication().runReadAction(() -> {
                        TranslatingCompilerFilesMonitor.getInstance()
                            .collectFiles(context,
                                (TranslatingCompiler)compiler,
                                Arrays.<VirtualFile>asList(allSources).iterator(),
                                true /*pass true to make sure that every source in scope file is processed*/,
                                false /*important! should pass false to enable collection of files to delete*/,
                                new ArrayList<>(),
                                toDelete
                            );
                    });
                    for (Trinity<File, String, Boolean> trinity : toDelete) {
                        context.getProgressIndicator().checkCanceled();
                        final File file = trinity.getFirst();
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

        boolean didSomething = false;

        didSomething |= generateSources(compileDriver, compilerManager, context, forceCompile, onlyCheckStatus);

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

        final CompileScope intermediateSources =
            compileDriver.attachIntermediateOutputDirectories(new CompositeScope(CompileScope.EMPTY_ARRAY) {
                @Override
                @Nonnull
                public Module[] getAffectedModules() {
                    return context.getCompileScope().getAffectedModules();
                }
            }, SOURCE_PROCESSING_ONLY);
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
        final CompilerManager compilerManager,
        CompileContextEx context,
        final boolean forceCompile,
        final boolean onlyCheckStatus
    ) throws ExitException {
        boolean didSomething = false;

        final SourceGeneratingCompiler[] sourceGenerators =
            compilerManager.getCompilers(SourceGeneratingCompiler.class, compileDriver.getCompilerFilter());
        for (final SourceGeneratingCompiler sourceGenerator : sourceGenerators) {
            if (context.getProgressIndicator().isCanceled()) {
                throw new ExitException(ExitStatus.CANCELLED);
            }

            final boolean generatedSomething = generateOutput(compileDriver, context, sourceGenerator, forceCompile, onlyCheckStatus);

            if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                throw new ExitException(ExitStatus.ERRORS);
            }
            didSomething |= generatedSomething;
        }
        return didSomething;
    }

    private boolean translate(
        CompileDriver compileDriver,
        final CompileContextEx context,
        final CompilerManager compilerManager,
        final boolean forceCompile,
        boolean isRebuild,
        final boolean onlyCheckStatus
    ) throws ExitException {
        boolean didSomething = false;

        final TranslatingCompiler[] original = compilerManager.getCompilers(TranslatingCompiler.class, compileDriver.getCompilerFilter());

        final List<Chunk<Module>> sortedChunks =
            Collections.unmodifiableList(ApplicationManager.getApplication().runReadAction((Supplier<List<Chunk<Module>>>)() -> {
                final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
                return ModuleCompilerUtil.getSortedModuleChunks(myProject, Arrays.asList(moduleManager.getModules()));
            }));

        final DumbService dumbService = DumbService.getInstance(myProject);
        try {
            final Set<Module> processedModules = new HashSet<>();
            VirtualFile[] snapshot = null;
            final Map<Chunk<Module>, Collection<VirtualFile>> chunkMap = new HashMap<>();
            int total = 0;
            int processed = 0;
            for (final Chunk<Module> currentChunk : sortedChunks) {
                TranslatingCompiler[] translators = original.clone();
                for (CompilerSorter compilerSorter : CompilerSorter.EP_NAME.getExtensionList()) {
                    compilerSorter.sort(currentChunk, translators, TranslatingCompiler.class);
                }
                final TranslatorsOutputSink sink = new TranslatorsOutputSink(context, translators);
                final Set<FileType> generatedTypes = new HashSet<>();
                Collection<VirtualFile> chunkFiles = chunkMap.get(currentChunk);
                final Set<VirtualFile> filesToRecompile = new HashSet<>();
                final Set<VirtualFile> allDependent = new HashSet<>();
                try {
                    int round = 0;
                    boolean compiledSomethingForThisChunk = false;
                    Collection<VirtualFile> dependentFiles = Collections.emptyList();
                    final Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> dependencyFilter =
                        new DependentClassesCumulativeFilter();

                    do {
                        for (
                            int currentCompiler = 0, translatorsLength = translators.length;
                            currentCompiler < translatorsLength;
                            currentCompiler++
                        ) {
                            sink.setCurrentCompilerIndex(currentCompiler);
                            final TranslatingCompiler compiler = translators[currentCompiler];
                            if (context.getProgressIndicator().isCanceled()) {
                                throw new ExitException(ExitStatus.CANCELLED);
                            }

                            dumbService.waitForSmartMode();

                            if (snapshot == null
                                || ContainerUtil.intersects(generatedTypes, compilerManager.getRegisteredInputTypes(compiler))) {
                                // rescan snapshot if previously generated files may influence the input of this compiler
                                final Collection<VirtualFile> prevSnapshot = round > 0 && snapshot != null
                                    ? Arrays.asList(snapshot)
                                    : Collections.<VirtualFile>emptySet();
                                snapshot = ApplicationManager.getApplication().runReadAction((Supplier<VirtualFile[]>)() -> {
                                    return context.getCompileScope().getFiles(null);
                                });
                                recalculateChunkToFilesMap(context, sortedChunks, snapshot, chunkMap);
                                if (round == 0) {
                                    chunkFiles = chunkMap.get(currentChunk);
                                }
                                else {
                                    final Set<VirtualFile> newFiles = new HashSet<>(chunkMap.get(currentChunk));
                                    newFiles.removeAll(prevSnapshot);
                                    newFiles.removeAll(chunkFiles);
                                    if (!newFiles.isEmpty()) {
                                        final ArrayList<VirtualFile> merged = new ArrayList<>(chunkFiles.size() + newFiles.size());
                                        merged.addAll(chunkFiles);
                                        merged.addAll(newFiles);
                                        chunkFiles = merged;
                                    }
                                }
                                total = snapshot.length * translatorsLength;
                            }

                            final CompileContextEx _context;
                            if (compiler instanceof IntermediateOutputCompiler) {
                                // wrap compile context so that output goes into intermediate directories
                                final IntermediateOutputCompiler _compiler = (IntermediateOutputCompiler)compiler;
                                _context = new CompileContextExDelegate(context) {
                                    @Override
                                    public VirtualFile getModuleOutputDirectory(final Module module) {
                                        return getGenerationOutputDir(compileDriver, _compiler, module, false);
                                    }

                                    @Override
                                    public VirtualFile getModuleOutputDirectoryForTests(final Module module) {
                                        return getGenerationOutputDir(compileDriver, _compiler, module, true);
                                    }
                                };
                            }
                            else {
                                _context = context;
                            }
                            final boolean compiledSomething = compileSources(
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
                            _context.getProgressIndicator().setFraction(((double)processed) / total);

                            if (compiledSomething) {
                                generatedTypes.addAll(compilerManager.getRegisteredOutputTypes(compiler));
                            }

                            didSomething |= compiledSomething;
                            compiledSomethingForThisChunk |= didSomething;

                            if (_context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                                break; // break the loop over compilers
                            }
                        }

                        final boolean hasUnprocessedTraverseRoots = context.getDependencyCache().hasUnprocessedTraverseRoots();
                        if (!isRebuild && (compiledSomethingForThisChunk || hasUnprocessedTraverseRoots)) {
                            final Set<VirtualFile> compiledWithErrors = CacheUtils.getFilesCompiledWithErrors(context);
                            filesToRecompile.removeAll(sink.getCompiledSources());
                            filesToRecompile.addAll(compiledWithErrors);

                            dependentFiles = CacheUtils.findDependentFiles(context, compiledWithErrors, dependencyFilter);
                            if (!processedModules.isEmpty()) {
                                for (Iterator<VirtualFile> it = dependentFiles.iterator(); it.hasNext(); ) {
                                    final VirtualFile next = it.next();
                                    final Module module = context.getModuleByFile(next);
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
                                final List<VirtualFile> filesInScope = getFilesInScope(context, currentChunk, dependentFiles);
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
                            final ProgressIndicator indicator = context.getProgressIndicator();
                            final DependencyCache cache = context.getDependencyCache();

                            indicator.pushState();
                            indicator.setText(CompilerBundle.message("progress.updating.caches"));
                            indicator.setText2("");

                            cache.update();

                            indicator.setText(CompilerBundle.message("progress.saving.caches"));
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
                    final int errorCount = context.getMessageCount(CompilerMessageCategory.ERROR);
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
                    final Collection<VirtualFile> deps = CacheUtils.findDependentFiles(context, Collections.<VirtualFile>emptySet(), null);
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


    private static List<VirtualFile> getFilesInScope(
        final CompileContextEx context,
        final Chunk<Module> chunk,
        final Collection<VirtualFile> files
    ) {
        final List<VirtualFile> filesInScope = new ArrayList<>(files.size());
        ApplicationManager.getApplication().runReadAction(() -> {
            for (VirtualFile file : files) {
                if (context.getCompileScope().belongs(file.getUrl())) {
                    final Module module = context.getModuleByFile(file);
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
        final Map<Module, List<VirtualFile>> moduleToFilesMap = CompilerUtil.buildModuleToFilesMap(context, snapshot);
        for (Chunk<Module> moduleChunk : allChunks) {
            List<VirtualFile> files = Collections.emptyList();
            for (Module module : moduleChunk.getNodes()) {
                final List<VirtualFile> moduleFiles = moduleToFilesMap.get(module);
                if (moduleFiles != null) {
                    files = ContainerUtil.concat(files, moduleFiles);
                }
            }
            chunkMap.put(moduleChunk, files);
        }
    }

    private boolean compileSources(
        CompileDriver compileDriver, final CompileContextEx context,
        final Chunk<Module> moduleChunk,
        final TranslatingCompiler compiler,
        final Collection<VirtualFile> srcSnapshot,
        final boolean forceCompile,
        final boolean isRebuild,
        final boolean onlyCheckStatus,
        TranslatingCompiler.OutputSink sink
    ) throws ExitException {
        final Set<VirtualFile> toCompile = new HashSet<>();
        final List<Trinity<File, String, Boolean>> toDelete = new ArrayList<>();
        context.getProgressIndicator().pushState();

        final boolean[] wereFilesDeleted = {false};
        try {
            ApplicationManager.getApplication().runReadAction(() -> {
                TranslatingCompilerFilesMonitor.getInstance().collectFiles(
                    context,
                    compiler,
                    srcSnapshot.iterator(),
                    forceCompile,
                    isRebuild,
                    toCompile,
                    toDelete
                );
            });

            if (onlyCheckStatus) {
                if (toDelete.isEmpty() && toCompile.isEmpty()) {
                    return false;
                }
                if (LOG.isDebugEnabled() || ourDebugMode) {
                    if (!toDelete.isEmpty()) {
                        final StringBuilder message = new StringBuilder();
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
                        final String message = "Found items to compile, compiler " + compiler.getDescription();
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
        final CompileContextEx context,
        final Collection<Trinity<File, String, Boolean>> toDelete
    ) throws CacheCorruptedException {
        final DependencyCache dependencyCache = context.getDependencyCache();
        final boolean isTestMode = ApplicationManager.getApplication().isUnitTestMode();

        final List<File> filesToRefresh = new ArrayList<>();
        final boolean[] wereFilesDeleted = {false};
        CompilerUtil.runInContext(context, CompilerBundle.message("progress.synchronizing.output.directory"), () -> {
            final long start = System.currentTimeMillis();
            try {
                for (final Trinity<File, String, Boolean> trinity : toDelete) {
                    final File outputPath = trinity.getFirst();
                    context.getProgressIndicator().checkCanceled();
                    context.getProgressIndicator().setText2(outputPath.getPath());
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
        });
        return wereFilesDeleted[0];
    }

    private boolean invokeFileProcessingCompilers(
        CompileDriver compileDriver, final CompilerManager compilerManager,
        CompileContextEx context,
        Class<? extends FileProcessingCompiler> fileProcessingCompilerClass,
        FileProcessingCompilerAdapterFactory factory,
        boolean forceCompile,
        final boolean checkScope,
        final boolean onlyCheckStatus
    ) throws ExitException {
        boolean didSomething = false;
        final FileProcessingCompiler[] compilers =
            compilerManager.getCompilers(fileProcessingCompilerClass, compileDriver.getCompilerFilter());
        if (compilers.length > 0) {
            try {
                CacheDeferredUpdater cacheUpdater = new CacheDeferredUpdater();
                try {
                    for (final FileProcessingCompiler compiler : compilers) {
                        if (context.getProgressIndicator().isCanceled()) {
                            throw new ExitException(ExitStatus.CANCELLED);
                        }

                        CompileContextEx _context = context;
                        if (compiler instanceof IntermediateOutputCompiler) {
                            final IntermediateOutputCompiler _compiler = (IntermediateOutputCompiler)compiler;
                            _context = new CompileContextExDelegate(context) {
                                @Override
                                public VirtualFile getModuleOutputDirectory(final Module module) {
                                    return getGenerationOutputDir(compileDriver, _compiler, module, false);
                                }

                                @Override
                                public VirtualFile getModuleOutputDirectoryForTests(final Module module) {
                                    return getGenerationOutputDir(compileDriver, _compiler, module, true);
                                }
                            };
                        }

                        final boolean processedSomething =
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
                    CompilerBundle.message("compiler.error.exception", e.getMessage()),
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
        final CompileContextEx context,
        final GeneratingCompiler compiler,
        final boolean forceGenerate,
        final boolean onlyCheckStatus
    ) throws ExitException {
        final GeneratingCompiler.GenerationItem[] allItems = compiler.getGenerationItems(context);
        final List<GeneratingCompiler.GenerationItem> toGenerate = new ArrayList<>();
        final List<File> filesToRefresh = new ArrayList<>();
        final List<File> generatedFiles = new ArrayList<>();
        final List<Module> affectedModules = new ArrayList<>();
        try {
            final StateCache<ValidityState> cache = getGeneratingCompilerCache(compiler);
            final Set<File> pathsToRemove = new HashSet<>(cache.getFiles());

            final Map<GeneratingCompiler.GenerationItem, File> itemToOutputPathMap = new HashMap<>();
            final IOException[] ex = {null};
            ApplicationManager.getApplication().runReadAction(() -> {
                for (final GeneratingCompiler.GenerationItem item : allItems) {
                    final Module itemModule = item.getModule();
                    final String outputDirPath = CompilerPaths.getGenerationOutputPath(compiler, itemModule, item.isTestSource());
                    final File outputPath = new File(outputDirPath, item.getPath());
                    itemToOutputPathMap.put(item, outputPath);

                    try {
                        final ValidityState savedState = cache.getState(outputPath);

                        if (forceGenerate || savedState == null || !savedState.equalsTo(item.getValidityState())) {
                            final String outputPathUrl = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, outputPath.getPath());
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
                CompilerUtil.runInContext(context, CompilerBundle.message("progress.synchronizing.output.directory"), () -> {
                    for (final File file : pathsToRemove) {
                        boolean result = compileDriver.deleteFile(file);
                        final boolean deleted = result;
                        if (deleted) {
                            cache.remove(file);
                            filesToRefresh.add(file);
                        }
                    }
                });
            }

            final Map<Module, Set<GeneratingCompiler.GenerationItem>> moduleToItemMap = buildModuleToGenerationItemMap(
                toGenerate.toArray(new GeneratingCompiler.GenerationItem[toGenerate.size()]));
            List<Module> modules = new ArrayList<>(moduleToItemMap.size());
            for (final Module module : moduleToItemMap.keySet()) {
                modules.add(module);
            }
            ModuleCompilerUtil.sortModules(myProject, modules);

            for (final Module module : modules) {
                CompilerUtil.runInContext(context, "Generating output from " + compiler.getDescription(), () -> {
                    final Set<GeneratingCompiler.GenerationItem> items = moduleToItemMap.get(module);
                    if (items != null && !items.isEmpty()) {
                        final GeneratingCompiler.GenerationItem[][] productionAndTestItems = splitGenerationItems(items);
                        for (GeneratingCompiler.GenerationItem[] _items : productionAndTestItems) {
                            if (_items.length == 0) {
                                continue;
                            }
                            final VirtualFile outputDir = getGenerationOutputDir(compileDriver, compiler, module, _items[0].isTestSource());
                            final GeneratingCompiler.GenerationItem[] successfullyGenerated = compiler.generate(context, _items, outputDir);

                            CompilerUtil.runInContext(context, CompilerBundle.message("progress.updating.caches"), () -> {
                                if (successfullyGenerated.length > 0) {
                                    affectedModules.add(module);
                                }
                                for (final GeneratingCompiler.GenerationItem item : successfullyGenerated) {
                                    final File file = itemToOutputPathMap.get(item);
                                    cache.update(file, item.getValidityState());
                                    filesToRefresh.add(file);
                                    generatedFiles.add(file);
                                    context.getProgressIndicator().setText2(file.getPath());
                                }
                            });
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
                    final ArrayList<VirtualFile> vFiles1 = new ArrayList<>(generatedFiles.size());
                    for (File generatedFile : generatedFiles) {
                        final VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(generatedFile);
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
        final IntermediateOutputCompiler compiler,
        final Module module,
        final boolean forTestSources
    ) {
        return compileDriver.getGenerationOutputDir(compiler, module, forTestSources);
    }

    private static GeneratingCompiler.GenerationItem[][] splitGenerationItems(final Set<GeneratingCompiler.GenerationItem> items) {
        final List<GeneratingCompiler.GenerationItem> production = new ArrayList<>();
        final List<GeneratingCompiler.GenerationItem> tests = new ArrayList<>();
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

    private StateCache<ValidityState> getGeneratingCompilerCache(final GeneratingCompiler compiler) throws IOException {
        return CompilerCacheManager.getInstance(myProject).getGeneratingCompilerCache(compiler);
    }

    private boolean processFiles(
        final FileProcessingCompilerAdapter adapter,
        final boolean forceCompile,
        final boolean checkScope,
        final boolean onlyCheckStatus,
        final CacheDeferredUpdater cacheUpdater
    ) throws ExitException, IOException {
        final CompileContextEx context = (CompileContextEx)adapter.getCompileContext();
        final FileProcessingCompilerStateCache cache = getFileProcessingCompilerCache(adapter.getCompiler());
        final FileProcessingCompiler.ProcessingItem[] items = adapter.getProcessingItems();
        if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
            return false;
        }
        if (LOG.isDebugEnabled() && items.length > 0) {
            LOG.debug("Start processing files by " + adapter.getCompiler().getDescription());
        }
        final CompileScope scope = context.getCompileScope();
        final List<FileProcessingCompiler.ProcessingItem> toProcess = new ArrayList<>();
        final Set<File> allFiles = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
        final IOException[] ex = {null};
        DumbService.getInstance(myProject).runReadActionInSmartMode(() -> {
            try {
                for (FileProcessingCompiler.ProcessingItem item : items) {
                    final File file = item.getFile();
                    allFiles.add(file);
                    if (!forceCompile && cache.getTimestamp(file) == file.lastModified()) {
                        final ValidityState state = cache.getExtState(file);
                        final ValidityState itemState = item.getValidityState();
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

        final Collection<File> files = cache.getFiles();
        final List<File> urlsToRemove = new ArrayList<>();
        if (!files.isEmpty()) {
            CompilerUtil.runInContext(context, CompilerBundle.message("progress.processing.outdated.files"), () -> {
                ApplicationManager.getApplication().runReadAction(() -> {
                    for (final File file : files) {
                        if (!allFiles.contains(file)) {
                            String fileUrl = VirtualFileUtil.urlToPath(file.getPath());
                            if (!checkScope || scope.belongs(fileUrl)) {
                                urlsToRemove.add(file);
                            }
                        }
                    }
                });
                if (!onlyCheckStatus && !urlsToRemove.isEmpty()) {
                    for (final File file : urlsToRemove) {
                        adapter.processOutdatedItem(context, file, cache.getExtState(file));
                        cache.remove(file);
                    }
                }
            });
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

        final FileProcessingCompiler.ProcessingItem[] processed =
            adapter.process(toProcess.toArray(new FileProcessingCompiler.ProcessingItem[toProcess.size()]));

        if (processed.length == 0) {
            return true;
        }
        CompilerUtil.runInContext(context, CompilerBundle.message("progress.updating.caches"), () -> {
            //final List<File> vFiles = new ArrayList<>(processed.length);
            for (FileProcessingCompiler.ProcessingItem aProcessed : processed) {
                final File file = aProcessed.getFile();
                //vFiles.add(file);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("\tFile processed " + file.getPath() + "; ts=" + file.lastModified());
                }

                //final String path = file.getPath();
                //final String outputDir = myOutputFinder.lookupOutputPath(path);
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
        });
        return true;
    }

    private static Map<Module, Set<GeneratingCompiler.GenerationItem>> buildModuleToGenerationItemMap(GeneratingCompiler.GenerationItem[] items) {
        final Map<Module, Set<GeneratingCompiler.GenerationItem>> map = new HashMap<>();
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
