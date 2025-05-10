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
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.Semaphore;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.build.ui.BuildContentManager;
import consulo.compiler.*;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.impl.internal.ArtifactImpl;
import consulo.compiler.impl.internal.artifact.ArtifactCompileScope;
import consulo.compiler.impl.internal.scope.CompositeScope;
import consulo.compiler.internal.AdditionalCompileScopeProvider;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.scope.FileIndexCompileScope;
import consulo.compiler.scope.FileSetCompileScope;
import consulo.compiler.util.CompilerUtil;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionPoint;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.PluginException;
import consulo.container.plugin.PluginId;
import consulo.content.ContentFolderTypeProvider;
import consulo.document.FileDocumentManager;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.psi.PsiDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.internal.ProjectRootManagerEx;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.collection.OrderedSet;
import consulo.util.collection.Sets;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Predicates;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Eugene Zhuravlev
 * @since 2003-01-17
 */
public class CompileDriver implements consulo.compiler.CompileDriver {
    public static final int DEPENDENCY_FORMAT_VERSION = 55;

    private static final Logger LOG = Logger.getInstance(CompileDriver.class);
    // to be used in tests only for debug output
    public static volatile boolean ourDebugMode = false;

    private final Project myProject;
    private final Map<Pair<IntermediateOutputCompiler, Module>, Couple<VirtualFile>> myGenerationCompilerModuleToOutputDirMap;
    // [IntermediateOutputCompiler, Module] -> [ProductionSources, TestSources]
    private final String myCachesDirectoryPath;
    private boolean myShouldClearOutputDirectory;

    private final Map<ContentFolderTypeProvider, Map<Module, String>> myOutputs = new HashMap<>(4);

    private static final String VERSION_FILE_NAME = "version.dat";
    private static final String LOCK_FILE_NAME = "in_progress.dat";

    private static final boolean GENERATE_CLASSPATH_INDEX = "true".equals(System.getProperty("generate.classpath.index"));
    private static final String PROP_PERFORM_INITIAL_REFRESH = "compiler.perform.outputs.refresh.on.start";
    private static final Key<Boolean> REFRESH_DONE_KEY = Key.create("_compiler.initial.refresh.done_");
    private static final Key<Boolean> COMPILATION_STARTED_AUTOMATICALLY = Key.create("compilation_started_automatically");

    private Predicate<Compiler> myCompilerFilter = Predicates.alwaysTrue();
    private static final Predicate<Compiler> SOURCE_PROCESSING_ONLY = compiler -> compiler instanceof SourceProcessingCompiler;
    private static final Predicate<Compiler> ALL_EXCEPT_SOURCE_PROCESSING = compiler -> !SOURCE_PROCESSING_ONLY.test(compiler);

    private Set<File> myAllOutputDirectories;
    private static final long ONE_MINUTE_MS = 60L /*sec*/ * 1000L /*millisec*/;

    @RequiredReadAction
    public CompileDriver(Project project) {
        myProject = project;
        myCachesDirectoryPath = CompilerPaths.getCacheStoreDirectory(myProject).getPath().replace('/', File.separatorChar);
        myShouldClearOutputDirectory = CompilerWorkspaceConfiguration.getInstance(myProject).CLEAR_OUTPUT_DIRECTORY;

        myGenerationCompilerModuleToOutputDirMap = new HashMap<>();

        LocalFileSystem lfs = LocalFileSystem.getInstance();
        IntermediateOutputCompiler[] generatingCompilers =
            CompilerManager.getInstance(myProject).getCompilers(IntermediateOutputCompiler.class, myCompilerFilter);
        Module[] allModules = ModuleManager.getInstance(myProject).getModules();

        for (Module module : allModules) {
            for (IntermediateOutputCompiler compiler : generatingCompilers) {
                VirtualFile productionOutput = lookupVFile(lfs, CompilerPaths.getGenerationOutputPath(compiler, module, false));
                VirtualFile testOutput = lookupVFile(lfs, CompilerPaths.getGenerationOutputPath(compiler, module, true));
                Pair<IntermediateOutputCompiler, Module> pair = Pair.create(compiler, module);
                Couple<VirtualFile> outputs = new Couple<>(productionOutput, testOutput);
                myGenerationCompilerModuleToOutputDirMap.put(pair, outputs);
            }

            var point = module.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class);
            point.forEachExtensionSafe(provider -> {
                for (ModuleAdditionalOutputDirectory directory : provider.getOutputDirectories()) {
                    lookupVFile(lfs, directory.path());
                }
            });
        }
    }

    public void setCompilerFilter(Predicate<Compiler> compilerFilter) {
        myCompilerFilter = compilerFilter == null ? Predicates.<Compiler>alwaysTrue() : compilerFilter;
    }

    @RequiredUIAccess
    public void rebuild(CompileStatusNotification callback) {
        CompileScope compileScope;
        CompileScope projectScope = CompilerManager.getInstance(myProject).createProjectCompileScope();
        CompileScope scopeWithArtifacts =
            ArtifactCompileScope.createScopeWithArtifacts(projectScope, ArtifactUtil.getArtifactWithOutputPaths(myProject), false);
        compileScope = addAdditionalRoots(scopeWithArtifacts, ALL_EXCEPT_SOURCE_PROCESSING);
        doRebuild(callback, null, true, compileScope);
    }

    @RequiredUIAccess
    public void make(CompileScope scope, CompileStatusNotification callback) {
        scope = addAdditionalRoots(scope, ALL_EXCEPT_SOURCE_PROCESSING);

        if (validateCompilerConfiguration(scope, false)) {
            startup(scope, false, false, callback, null, true);
        }
        else {
            callback.finished(true, 0, 0, DummyCompileContext.getInstance());
        }
    }

    public boolean isUpToDate(CompileScope scope) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("isUpToDate operation started");
        }
        scope = addAdditionalRoots(scope, ALL_EXCEPT_SOURCE_PROCESSING);

        CompilerTask task = new CompilerTask(myProject, "Classes up-to-date check", false, isCompilationStartedAutomatically(scope));
        CompositeDependencyCache cache = createDependencyCache();
        CompileContextImpl compileContext = new CompileContextImpl(myProject, task, scope, cache, true, false);

        checkCachesVersion(compileContext, ManagingFS.getInstance().getCreationTimestamp());
        if (compileContext.isRebuildRequested()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Rebuild requested, up-to-date=false");
            }
            return false;
        }

        for (Map.Entry<Pair<IntermediateOutputCompiler, Module>, Couple<VirtualFile>> entry
            : myGenerationCompilerModuleToOutputDirMap.entrySet()) {
            Couple<VirtualFile> outputs = entry.getValue();
            Pair<IntermediateOutputCompiler, Module> key = entry.getKey();
            Module module = key.getSecond();
            compileContext.assignModule(outputs.getFirst(), module, false, key.getFirst());
            compileContext.assignModule(outputs.getSecond(), module, true, key.getFirst());
        }

        SimpleReference<ExitStatus> result = new SimpleReference<>();

        @RequiredReadAction
        Runnable compileWork = () -> {
            try {
                myAllOutputDirectories = getAllOutputDirectories(compileContext);
                // need this for updating zip archives experiment, uncomment if the feature is turned on
                //myOutputFinder = new OutputPathFinder(myAllOutputDirectories);
                ExitStatus status = doCompile(compileContext, false, false, true);

                result.set(status);

                task.setEndCompilationStamp(status, System.currentTimeMillis());
            }
            finally {
                CompilerCacheManager.getInstance(myProject).flushCaches();
            }
        };
        task.start(compileWork);

        if (LOG.isDebugEnabled()) {
            LOG.debug("isUpToDate operation finished");
        }

        return ExitStatus.UP_TO_DATE.equals(result.get());
    }

    @Nonnull
    private CompositeDependencyCache createDependencyCache() {
        return new CompositeDependencyCache(myProject, myCachesDirectoryPath);
    }

    @RequiredUIAccess
    public void compile(CompileScope scope, CompileStatusNotification callback, boolean clearingOutputDirsPossible) {
        myShouldClearOutputDirectory &= clearingOutputDirsPossible;
        if (containsFileIndexScopes(scope)) {
            scope = addAdditionalRoots(scope, ALL_EXCEPT_SOURCE_PROCESSING);
        }
        if (validateCompilerConfiguration(scope, false)) {
            startup(scope, false, true, callback, null, true);
        }
        else {
            callback.finished(true, 0, 0, DummyCompileContext.getInstance());
        }
    }

    private static boolean containsFileIndexScopes(CompileScope scope) {
        if (scope instanceof CompositeScope) {
            for (CompileScope childScope : ((CompositeScope)scope).getScopes()) {
                if (containsFileIndexScopes(childScope)) {
                    return true;
                }
            }
        }
        return scope instanceof FileIndexCompileScope;
    }

    private static class CompileStatus {
        final int CACHE_FORMAT_VERSION;
        final boolean COMPILATION_IN_PROGRESS;
        final long VFS_CREATION_STAMP;

        private CompileStatus(int cacheVersion, boolean isCompilationInProgress, long vfsStamp) {
            CACHE_FORMAT_VERSION = cacheVersion;
            COMPILATION_IN_PROGRESS = isCompilationInProgress;
            VFS_CREATION_STAMP = vfsStamp;
        }
    }

    private CompileStatus readStatus() {
        boolean isInProgress = getLockFile().exists();
        int version = -1;
        long vfsStamp = -1L;
        try {
            File versionFile = new File(myCachesDirectoryPath, VERSION_FILE_NAME);
            try (DataInputStream in = new DataInputStream(new FileInputStream(versionFile))) {
                version = in.readInt();
                try {
                    vfsStamp = in.readLong();
                }
                catch (IOException ignored) {
                }
            }
        }
        catch (FileNotFoundException e) {
            // ignore
        }
        catch (IOException e) {
            LOG.info(e);  // may happen in case of IDEA crashed and the file is not written properly
            return null;
        }
        return new CompileStatus(version, isInProgress, vfsStamp);
    }

    private void writeStatus(CompileStatus status, CompileContextEx context) {
        File statusFile = new File(myCachesDirectoryPath, VERSION_FILE_NAME);

        File lockFile = getLockFile();
        try {
            FileUtil.createIfDoesntExist(statusFile);
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(statusFile))) {
                out.writeInt(status.CACHE_FORMAT_VERSION);
                out.writeLong(status.VFS_CREATION_STAMP);
            }
            if (status.COMPILATION_IN_PROGRESS) {
                FileUtil.createIfDoesntExist(lockFile);
            }
            else {
                deleteFile(lockFile);
            }
        }
        catch (IOException e) {
            context.addMessage(
                CompilerMessageCategory.ERROR,
                CompilerLocalize.compilerErrorException(e.getMessage()).get(),
                null,
                -1,
                -1
            );
        }
    }

    private File getLockFile() {
        return new File(CompilerPaths.getCompilerSystemDirectory(myProject), LOCK_FILE_NAME);
    }

    @RequiredUIAccess
    private void doRebuild(
        CompileStatusNotification callback,
        CompilerMessage message,
        boolean checkCachesVersion,
        CompileScope compileScope
    ) {
        if (validateCompilerConfiguration(compileScope, true)) {
            startup(compileScope, true, false, callback, message, checkCachesVersion);
        }
        else {
            callback.finished(true, 0, 0, DummyCompileContext.getInstance());
        }
    }

    private CompileScope addAdditionalRoots(CompileScope originalScope, Predicate<Compiler> filter) {
        CompileScope scope = attachIntermediateOutputDirectories(originalScope, filter);

        CompileScope baseScope = scope;

        SimpleReference<CompileScope> scopeRef = SimpleReference.create(scope);

        myProject.getApplication().getExtensionPoint(AdditionalCompileScopeProvider.class).forEachExtensionSafe(scopeProvider -> {
            CompileScope additionalScope = scopeProvider.getAdditionalScope(baseScope, filter, myProject);
            if (additionalScope != null) {
                scopeRef.set(new CompositeScope(scopeRef.get(), additionalScope));
            }
        });
        return scopeRef.get();
    }

    @Override
    public CompileScope attachIntermediateOutputDirectories(CompileScope originalScope, Predicate<Compiler> filter) {
        CompileScope scope = originalScope;
        Set<Module> affected = new HashSet<>(Arrays.asList(originalScope.getAffectedModules()));
        for (Map.Entry<Pair<IntermediateOutputCompiler, Module>, Couple<VirtualFile>> entry
            : myGenerationCompilerModuleToOutputDirMap.entrySet()) {
            Module module = entry.getKey().getSecond();
            if (affected.contains(module) && filter.test(entry.getKey().getFirst())) {
                Couple<VirtualFile> outputs = entry.getValue();
                scope = new CompositeScope(
                    scope,
                    new FileSetCompileScope(Arrays.asList(outputs.getFirst(), outputs.getSecond()), new Module[]{module})
                );
            }
        }
        return scope;
    }

    public static void setCompilationStartedAutomatically(CompileScope scope) {
        //todo[nik] pass this option as a parameter to compile/make methods instead
        scope.putUserData(COMPILATION_STARTED_AUTOMATICALLY, Boolean.TRUE);
    }

    private static boolean isCompilationStartedAutomatically(CompileScope scope) {
        return Boolean.TRUE.equals(scope.getUserData(COMPILATION_STARTED_AUTOMATICALLY));
    }

    private void attachAnnotationProcessorsOutputDirectories(CompileContextEx context) {
        LocalFileSystem lfs = LocalFileSystem.getInstance();
        Set<Module> affected = new HashSet<>(Arrays.asList(context.getCompileScope().getAffectedModules()));
        for (Module module : affected) {
            ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();

            var point = module.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class);

            point.forEachExtensionSafe(provider -> {
                for (ModuleAdditionalOutputDirectory directory : provider.getOutputDirectories()) {
                    VirtualFile vFile = lfs.findFileByPath(directory.path());
                    if (vFile == null) {
                        continue;
                    }
                    if (fileIndex.isInSourceContent(vFile)) {
                        // no need to add, is already marked as source
                        continue;
                    }

                    context.addScope(new FileSetCompileScope(
                        Collections.singletonList(vFile),
                        new Module[]{module},
                        directory.testScope()
                    ));
                    context.assignModule(vFile, module, false, null);
                }
            });
        }
    }

    @RequiredUIAccess
    private void startup(
        CompileScope scope,
        boolean isRebuild,
        boolean forceCompile,
        CompileStatusNotification callback,
        CompilerMessage message,
        boolean checkCachesVersion
    ) {
        UIAccess.assertIsUIThread();

        ProblemsView.getInstance(myProject).clearOldMessages();

        LocalizeValue contentName =
            forceCompile ? CompilerLocalize.compilerContentNameCompile() : CompilerLocalize.compilerContentNameMake();
        Application application = Application.get();
        boolean isUnitTestMode = application.isUnitTestMode();
        CompilerTask compileTask = new CompilerTask(myProject, contentName.get(), true, isCompilationStartedAutomatically(scope));

        StatusBar.Info.set("", myProject, "Compiler");
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        FileDocumentManager.getInstance().saveAllDocuments();

        CompositeDependencyCache dependencyCache = createDependencyCache();
        CompileContextImpl compileContext =
            new CompileContextImpl(myProject, compileTask, scope, dependencyCache, !isRebuild && !forceCompile, isRebuild);

        for (Map.Entry<Pair<IntermediateOutputCompiler, Module>, Couple<VirtualFile>> entry
            : myGenerationCompilerModuleToOutputDirMap.entrySet()) {
            Couple<VirtualFile> outputs = entry.getValue();
            Pair<IntermediateOutputCompiler, Module> key = entry.getKey();
            Module module = key.getSecond();
            compileContext.assignModule(outputs.getFirst(), module, false, key.getFirst());
            compileContext.assignModule(outputs.getSecond(), module, true, key.getFirst());
        }
        attachAnnotationProcessorsOutputDirectories(compileContext);

        @RequiredReadAction
        Runnable compileWork = () -> {
            if (compileContext.getProgressIndicator().isCanceled()) {
                if (callback != null) {
                    callback.finished(true, 0, 0, compileContext);
                }
                return;
            }
            try {
                if (myProject.isDisposed()) {
                    return;
                }
                LOG.info("COMPILATION STARTED");
                if (message != null) {
                    compileContext.addMessage(message);
                }
                else if (!isUnitTestMode) {
                    //FIXME [VISTALL] notifyDeprecatedImplementation();
                }

                TranslatingCompilerFilesMonitor.getInstance()
                    .ensureInitializationCompleted(myProject, compileContext.getProgressIndicator());

                ExitStatus status = doCompile(compileContext, isRebuild, forceCompile, callback, checkCachesVersion);

                compileTask.setEndCompilationStamp(status, System.currentTimeMillis());
            }
            finally {
                FileUtil.delete(CompilerPaths.getRebuildMarkerFile(myProject));
            }
        };

        compileTask.start(compileWork);
    }

    @RequiredReadAction
    private ExitStatus doCompile(
        CompileContextImpl compileContext,
        boolean isRebuild,
        boolean forceCompile,
        CompileStatusNotification callback,
        boolean checkCachesVersion
    ) {
        ExitStatus status = ExitStatus.ERRORS;
        boolean wereExceptions = false;
        long vfsTimestamp = (ManagingFS.getInstance()).getCreationTimestamp();
        try {
            if (checkCachesVersion) {
                checkCachesVersion(compileContext, vfsTimestamp);
                if (compileContext.isRebuildRequested()) {
                    return status;
                }
            }
            writeStatus(new CompileStatus(DEPENDENCY_FORMAT_VERSION, true, vfsTimestamp), compileContext);
            if (compileContext.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                return status;
            }

            myAllOutputDirectories = getAllOutputDirectories(compileContext);
            status = doCompile(compileContext, isRebuild, forceCompile, false);
        }
        catch (Throwable ex) {
            if (Application.get().isUnitTestMode()) {
                throw new RuntimeException(ex);
            }

            wereExceptions = true;
            PluginId pluginId = PluginExceptionUtil.findFirstPluginId(ex);

            StringBuilder message = new StringBuilder();
            message.append("Internal error");
            if (pluginId != null) {
                message.append(" (Plugin: ").append(pluginId).append(")");
            }
            message.append(": ").append(ex.getMessage());
            compileContext.addMessage(CompilerMessageCategory.ERROR, message.toString(), null, -1, -1);

            if (pluginId != null) {
                throw new PluginException(ex, pluginId);
            }
            throw new RuntimeException(ex);
        }
        finally {
            dropDependencyCache(compileContext);
            CompilerCacheManager.getInstance(myProject).flushCaches();
            if (compileContext.isRebuildRequested()) {
                Application.get().invokeLater(
                    () -> {
                        CompilerMessageImpl msg =
                            new CompilerMessageImpl(myProject, CompilerMessageCategory.INFORMATION, compileContext.getRebuildReason());
                        doRebuild(callback, msg, false, compileContext.getCompileScope());
                    },
                    ModalityState.nonModal()
                );
            }
            else {
                if (!myProject.isDisposed()) {
                    writeStatus(new CompileStatus(DEPENDENCY_FORMAT_VERSION, wereExceptions, vfsTimestamp), compileContext);
                }
                long duration = notifyCompilationCompleted(compileContext, callback, status, false);
                CompilerUtil.logDuration(
                    "\tCOMPILATION FINISHED; Errors: " + compileContext.getMessageCount(CompilerMessageCategory.ERROR) +
                        "; warnings: " + compileContext.getMessageCount(CompilerMessageCategory.WARNING),
                    duration
                );
            }
        }

        return status;
    }

    /**
     * @noinspection SSBasedInspection
     */
    private long notifyCompilationCompleted(
        CompileContextImpl compileContext,
        CompileStatusNotification callback,
        ExitStatus _status,
        boolean refreshOutputRoots
    ) {
        long duration = System.currentTimeMillis() - compileContext.getStartCompilationStamp();
        if (refreshOutputRoots) {
            // refresh on output roots is required in order for the order enumerator to see all roots via VFS
            Set<File> outputs = new HashSet<>();
            Module[] affectedModules = compileContext.getCompileScope().getAffectedModules();
            for (String path : CompilerPaths.getOutputPaths(affectedModules)) {
                outputs.add(new File(path));
            }
            LocalFileSystem lfs = LocalFileSystem.getInstance();
            if (!outputs.isEmpty()) {
                ProgressIndicator indicator = compileContext.getProgressIndicator();
                indicator.setText("Synchronizing output directories...");
                lfs.refreshIoFiles(outputs, _status == ExitStatus.CANCELLED, false, null);
                indicator.setTextValue(LocalizeValue.empty());
            }

            Set<File> genSourceRoots = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
            for (Module affectedModule : affectedModules) {
                ExtensionPoint<ModuleAdditionalOutputDirectoriesProvider> point =
                    affectedModule.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class);

                point.forEachExtensionSafe(provider -> {
                    for (ModuleAdditionalOutputDirectory directory : provider.getOutputDirectories()) {
                        genSourceRoots.add(new File(directory.path()));
                    }
                });
            }

            if (!genSourceRoots.isEmpty()) {
                // refresh generates source roots asynchronously; needed for error highlighting update
                lfs.refreshIoFiles(genSourceRoots, true, true, null);
            }
        }
        myProject.getUIAccess().give(() -> {
            int errorCount = 0;
            int warningCount = 0;
            try {
                errorCount = compileContext.getMessageCount(CompilerMessageCategory.ERROR);
                warningCount = compileContext.getMessageCount(CompilerMessageCategory.WARNING);
                if (!myProject.isDisposed()) {
                    String statusMessage = createStatusMessage(_status, warningCount, errorCount, duration);
                    NotificationType messageType =
                        errorCount > 0 ? NotificationType.ERROR : warningCount > 0 ? NotificationType.WARNING : NotificationType.INFORMATION;
                    if (duration > ONE_MINUTE_MS) {
                        ToolWindowManager.getInstance(myProject)
                            .notifyByBalloon(BuildContentManager.TOOL_WINDOW_ID, messageType.toUI(), statusMessage);
                    }
                    CompilerManager.NOTIFICATION_GROUP.createNotification(statusMessage, messageType).notify(myProject);
                    if (_status != ExitStatus.UP_TO_DATE && compileContext.getMessageCount(null) > 0) {
                        compileContext.addMessage(CompilerMessageCategory.INFORMATION, statusMessage, null, -1, -1);
                    }
                }
            }
            finally {
                if (callback != null) {
                    callback.finished(_status == ExitStatus.CANCELLED, errorCount, warningCount, compileContext);
                }
            }
        });
        return duration;
    }

    private void checkCachesVersion(CompileContextImpl compileContext, long currentVFSTimestamp) {
        if (CompilerPaths.getRebuildMarkerFile(compileContext.getProject()).exists()) {
            compileContext.requestRebuildNextTime("Compiler caches are out of date, project rebuild is required");
            return;
        }
        CompileStatus compileStatus = readStatus();
        if (compileStatus == null) {
            compileContext.requestRebuildNextTime(CompilerLocalize.errorCompilerCachesCorrupted().get());
        }
        else if (compileStatus.CACHE_FORMAT_VERSION != -1 && compileStatus.CACHE_FORMAT_VERSION != DEPENDENCY_FORMAT_VERSION) {
            compileContext.requestRebuildNextTime(CompilerLocalize.errorCachesOldFormat().get());
        }
        else if (compileStatus.COMPILATION_IN_PROGRESS) {
            compileContext.requestRebuildNextTime(CompilerLocalize.errorPreviousCompilationFailed().get());
        }
        else if (compileStatus.VFS_CREATION_STAMP >= 0L) {
            if (currentVFSTimestamp != compileStatus.VFS_CREATION_STAMP) {
                compileContext.requestRebuildNextTime(CompilerLocalize.errorVfsWasRebuilt().get());
            }
        }
    }

    private static String createStatusMessage(ExitStatus status, int warningCount, int errorCount, long duration) {
        String message;
        if (status == ExitStatus.CANCELLED) {
            message = CompilerLocalize.statusCompilationAborted().get();
        }
        else if (status == ExitStatus.UP_TO_DATE) {
            message = CompilerLocalize.statusAllUpToDate().get();
        }
        else {
            if (status == ExitStatus.SUCCESS) {
                message = warningCount > 0
                    ? CompilerLocalize.statusCompilationCompletedSuccessfullyWithWarnings(warningCount).get()
                    : CompilerLocalize.statusCompilationCompletedSuccessfully().get();
            }
            else {
                message = CompilerLocalize.statusCompilationCompletedSuccessfullyWithWarningsAndErrors(errorCount, warningCount).get();
            }
            message = message + " in " + StringUtil.formatDuration(duration);
        }
        return message;
    }

    @RequiredReadAction
    private ExitStatus doCompile(
        CompileContextEx context,
        boolean isRebuild,
        boolean forceCompile,
        boolean onlyCheckStatus
    ) {
        try {
            if (isRebuild) {
                deleteAll(context);
            }
            else if (forceCompile) {
                if (myShouldClearOutputDirectory) {
                    clearAffectedOutputPathsIfPossible(context);
                }
            }
            if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                if (LOG.isDebugEnabled()) {
                    logErrorMessages(context);
                }
                return ExitStatus.ERRORS;
            }

            if (!onlyCheckStatus) {
                if (!executeCompileTasks(context, true)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Compilation cancelled");
                    }
                    return ExitStatus.CANCELLED;
                }
            }

            if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                if (LOG.isDebugEnabled()) {
                    logErrorMessages(context);
                }
                return ExitStatus.ERRORS;
            }

            boolean needRecalcOutputDirs = false;
            if (Registry.is(PROP_PERFORM_INITIAL_REFRESH) || !REFRESH_DONE_KEY.get(myProject, Boolean.FALSE)) {
                REFRESH_DONE_KEY.set(myProject, Boolean.TRUE);
                long refreshStart = System.currentTimeMillis();

                //need this to make sure the VFS is built
                List<VirtualFile> outputsToRefresh = new ArrayList<>();

                VirtualFile[] all = context.getAllOutputDirectories();

                ProgressIndicator progressIndicator = context.getProgressIndicator();

                //int totalCount = all.length + myGenerationCompilerModuleToOutputDirMap.size() * 2;
                progressIndicator.pushState();
                progressIndicator.setText("Inspecting output directories...");
                try {
                    for (VirtualFile output : all) {
                        if (output.isValid()) {
                            walkChildren(output, context);
                        }
                        else {
                            needRecalcOutputDirs = true;
                            File file = new File(output.getPath());
                            if (!file.exists()) {
                                boolean created = file.mkdirs();
                                if (!created) {
                                    context.addMessage(
                                        CompilerMessageCategory.ERROR,
                                        "Failed to create output directory " + file.getPath(),
                                        null,
                                        0,
                                        0
                                    );
                                    return ExitStatus.ERRORS;
                                }
                            }
                            output = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                            if (output == null) {
                                context.addMessage(
                                    CompilerMessageCategory.ERROR,
                                    "Failed to locate output directory " + file.getPath(),
                                    null,
                                    0,
                                    0
                                );
                                return ExitStatus.ERRORS;
                            }
                        }
                        outputsToRefresh.add(output);
                    }
                    for (Pair<IntermediateOutputCompiler, Module> pair : myGenerationCompilerModuleToOutputDirMap.keySet()) {
                        Couple<VirtualFile> generated = myGenerationCompilerModuleToOutputDirMap.get(pair);
                        walkChildren(generated.getFirst(), context);
                        outputsToRefresh.add(generated.getFirst());
                        walkChildren(generated.getSecond(), context);
                        outputsToRefresh.add(generated.getSecond());
                    }

                    RefreshQueue.getInstance().refresh(false, true, null, outputsToRefresh);
                    if (progressIndicator.isCanceled()) {
                        return ExitStatus.CANCELLED;
                    }
                }
                finally {
                    progressIndicator.popState();
                }

                long initialRefreshTime = System.currentTimeMillis() - refreshStart;
                CompilerUtil.logDuration("Initial VFS refresh", initialRefreshTime);
            }

            //DumbService.getInstance(myProject).waitForSmartMode();
            Semaphore semaphore = new Semaphore();
            semaphore.down();
            DumbService.getInstance(myProject).runWhenSmart(semaphore::up);
            while (!semaphore.waitFor(500)) {
                if (context.getProgressIndicator().isCanceled()) {
                    return ExitStatus.CANCELLED;
                }
            }

            if (needRecalcOutputDirs) {
                context.recalculateOutputDirs();
            }

            boolean didSomething = false;

            try {
                for (CompilerRunner compilerRunner : myProject.getExtensionList(CompilerRunner.class)) {
                    if (compilerRunner.isAvailable()) {
                        didSomething = compilerRunner.build(this, context, isRebuild, forceCompile, onlyCheckStatus);
                        break;
                    }
                }
            }
            catch (ExitException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(e);
                    logErrorMessages(context);
                }
                return e.getExitStatus();
            }
            finally {
                // drop in case it has not been dropped yet.
                dropDependencyCache(context);
                VirtualFile[] allOutputDirs = context.getAllOutputDirectories();

                if (didSomething && GENERATE_CLASSPATH_INDEX) {
                    CompilerUtil.runInContext(context, "Generating classpath index...", () -> {
                        int count = 0;
                        for (VirtualFile file : allOutputDirs) {
                            context.getProgressIndicator().setFraction((double)++count / allOutputDirs.length);
                            createClasspathIndex(file);
                        }
                    });
                }

            }

            if (!onlyCheckStatus) {
                if (!executeCompileTasks(context, false)) {
                    return ExitStatus.CANCELLED;
                }
                //FIXME [VISTALL] int constantSearchesCount = ChangedConstantsDependencyProcessor.getConstantSearchesCount(context);
                //FIXME [VISTALL] LOG.debug("Constants searches: " + constantSearchesCount);
            }

            if (context.getMessageCount(CompilerMessageCategory.ERROR) > 0) {
                if (LOG.isDebugEnabled()) {
                    logErrorMessages(context);
                }
                return ExitStatus.ERRORS;
            }
            if (!didSomething) {
                return ExitStatus.UP_TO_DATE;
            }
            return ExitStatus.SUCCESS;
        }
        catch (ProcessCanceledException e) {
            return ExitStatus.CANCELLED;
        }
    }

    @RequiredReadAction
    private void clearAffectedOutputPathsIfPossible(CompileContextEx context) {
        @RequiredReadAction
        ThrowableComputable<List<File>, RuntimeException> action = () -> {
            MultiMap<File, Module> outputToModulesMap = new MultiMap<>();
            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(module);
                for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.filter(LanguageContentFolderScopes.productionAndTest())) {
                    String outputPathUrl = moduleCompilerPathsManager.getCompilerOutputUrl(contentFolderTypeProvider);
                    if (outputPathUrl != null) {
                        String path = VirtualFileManager.extractPath(outputPathUrl);
                        outputToModulesMap.putValue(new File(path), module);
                    }
                }
            }
            Set<Module> affectedModules = new HashSet<>(Arrays.asList(context.getCompileScope().getAffectedModules()));
            List<File> result = new ArrayList<>(affectedModules.size() * 2);
            for (File output : outputToModulesMap.keySet()) {
                if (affectedModules.containsAll(outputToModulesMap.get(output))) {
                    result.add(output);
                }
            }

            Set<Artifact> artifactsToBuild = ArtifactCompileScope.getArtifactsToBuild(myProject, context.getCompileScope(), true);
            for (Artifact artifact : artifactsToBuild) {
                String outputFilePath = ((ArtifactImpl)artifact).getOutputDirectoryPathToCleanOnRebuild();
                if (outputFilePath != null) {
                    result.add(new File(FileUtil.toSystemDependentName(outputFilePath)));
                }
            }
            return result;
        };
        List<File> scopeOutputs = AccessRule.read(action);
        if (!scopeOutputs.isEmpty()) {
            CompilerUtil.runInContext(
                context,
                CompilerLocalize.progressClearingOutput().get(),
                () -> CompilerUtil.clearOutputDirectories(scopeOutputs)
            );
        }
    }

    private static void logErrorMessages(CompileContext context) {
        CompilerMessage[] errors = context.getMessages(CompilerMessageCategory.ERROR);
        if (errors.length > 0) {
            LOG.debug("Errors reported: ");
            for (CompilerMessage error : errors) {
                LOG.debug("\t" + error.getMessage());
            }
        }
    }

    private static void walkChildren(VirtualFile from, CompileContext context) {
        VirtualFileUtil.visitChildrenRecursively(from, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (file.isDirectory()) {
                    context.getProgressIndicator().checkCanceled();
                    context.getProgressIndicator().setText2(file.getPresentableUrl());
                }
                return true;
            }
        });
    }

    private static void createClasspathIndex(VirtualFile file) {
        try (BufferedWriter writer =
                 new BufferedWriter(new FileWriter(new File(VirtualFileUtil.virtualToIoFile(file), "classpath.index")))) {
            writeIndex(writer, file, file);
        }
        catch (IOException e) {
            // Ignore. Failed to create optional classpath index
        }
    }

    private static void writeIndex(BufferedWriter writer, VirtualFile root, VirtualFile file) throws IOException {
        VirtualFileUtil.visitChildrenRecursively(file,
            new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@Nonnull VirtualFile file) {
                    try {
                        writer.write(VirtualFileUtil.getRelativePath(file, root, '/'));
                        writer.write('\n');
                        return true;
                    }
                    catch (IOException e) {
                        throw new VisitorException(e);
                    }
                }
            }, IOException.class
        );
    }

    @Override
    public void dropDependencyCache(CompileContextEx context) {
        CompilerUtil.runInContext(context, CompilerLocalize.progressSavingCaches().get(), () -> context.getDependencyCache().resetState());
    }

    private void deleteAll(CompileContextEx context) {
        CompilerUtil.runInContext(
            context,
            CompilerLocalize.progressClearingOutput().get(),
            () -> {
                if (myShouldClearOutputDirectory) {
                    CompilerUtil.clearOutputDirectories(myAllOutputDirectories);
                }
                else { // refresh is still required
                    try {
                        for (CompilerRunner compilerRunner : myProject.getExtensionList(CompilerRunner.class)) {
                            if (compilerRunner.isAvailable()) {
                                compilerRunner.cleanUp(this, context);
                                break;
                            }
                        }

                        // to avoid too much files deleted events
                        pruneEmptyDirectories(context, context.getProgressIndicator(), myAllOutputDirectories);
                    }
                    finally {
                        CompilerUtil.refreshIODirectories(myAllOutputDirectories);
                    }
                }
                dropScopesCaches();

                clearCompilerSystemDirectory(context);
            }
        );
    }

    private void dropScopesCaches() {
        // hack to be sure the classpath will include the output directories
        Application.get()
            .runReadAction(() -> ((ProjectRootManagerEx)ProjectRootManager.getInstance(myProject)).clearScopesCachesForModules());
    }

    private void pruneEmptyDirectories(CompileContextEx context, ProgressIndicator progress, Set<File> directories) {
        for (File directory : directories) {
            doPrune(context, progress, directory, directories);
        }
    }

    private boolean doPrune(CompileContextEx context, ProgressIndicator progress, File directory, Set<File> outPutDirectories) {
        progress.checkCanceled();
        File[] files = directory.listFiles();
        boolean isEmpty = true;
        if (files != null) {
            for (File file : files) {
                if (!outPutDirectories.contains(file)) {
                    if (doPrune(context, progress, file, outPutDirectories)) {
                        deleteFile(file);
                    }
                    else {
                        isEmpty = false;
                    }
                }
                else {
                    isEmpty = false;
                }
            }
        }
        else {
            isEmpty = false;
        }

        return isEmpty;
    }

    @RequiredReadAction
    private Set<File> getAllOutputDirectories(CompileContext context) {
        Set<File> outputDirs = new OrderedSet<>();
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        for (String path : CompilerPaths.getOutputPaths(modules)) {
            outputDirs.add(new File(path));
        }
        for (Pair<IntermediateOutputCompiler, Module> pair : myGenerationCompilerModuleToOutputDirMap.keySet()) {
            outputDirs.add(new File(CompilerPaths.getGenerationOutputPath(pair.getFirst(), pair.getSecond(), false)));
            outputDirs.add(new File(CompilerPaths.getGenerationOutputPath(pair.getFirst(), pair.getSecond(), true)));
        }

        for (Module module : modules) {
            ExtensionPoint<ModuleAdditionalOutputDirectoriesProvider> point =
                module.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class);

            point.forEachExtensionSafe(provider -> {
                for (ModuleAdditionalOutputDirectory outputDirectory : provider.getOutputDirectories()) {
                    outputDirs.add(new File(outputDirectory.path()));
                }
            });
        }

        for (Artifact artifact : ArtifactManager.getInstance(myProject).getArtifacts()) {
            String path = ((ArtifactImpl)artifact).getOutputDirectoryPathToCleanOnRebuild();
            if (path != null) {
                outputDirs.add(new File(FileUtil.toSystemDependentName(path)));
            }
        }
        return outputDirs;
    }

    private void clearCompilerSystemDirectory(CompileContextEx context) {
        CompilerCacheManager.getInstance(myProject).clearCaches(context);
        FileUtil.delete(CompilerPathsEx.getZipStoreDirectory(myProject));
        dropDependencyCache(context);

        for (Pair<IntermediateOutputCompiler, Module> pair : myGenerationCompilerModuleToOutputDirMap.keySet()) {
            File[] outputs = {
                new File(CompilerPaths.getGenerationOutputPath(pair.getFirst(), pair.getSecond(), false)),
                new File(CompilerPaths.getGenerationOutputPath(pair.getFirst(), pair.getSecond(), true))
            };
            for (File output : outputs) {
                File[] files = output.listFiles();
                if (files != null) {
                    for (File file : files) {
                        boolean deleteOk = deleteFile(file);
                        if (!deleteOk) {
                            context.addMessage(
                                CompilerMessageCategory.ERROR,
                                CompilerLocalize.compilerErrorFailedToDelete(file.getPath()).get(),
                                null,
                                -1,
                                -1
                            );
                        }
                    }
                }
            }
        }
    }

    // [mike] performance optimization - this method is accessed > 15,000 times in Aurora
    private String getModuleOutputPath(Module module, ContentFolderTypeProvider contentFolderType) {
        Map<Module, String> map = myOutputs.get(contentFolderType);
        if (map == null) {
            myOutputs.put(contentFolderType, map = new HashMap<>());
        }

        String path = map.get(module);
        if (path == null) {
            path = CompilerPaths.getModuleOutputPath(module, contentFolderType);
            map.put(module, path);
        }

        return path;
    }

    /**
     * @param file a file to delete
     * @return true if and only if the file existed and was successfully deleted
     * Note: the behaviour is different from FileUtil.delete() which returns true if the file absent on the disk
     */
    @Override
    public boolean deleteFile(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File file1 : files) {
                deleteFile(file1);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (file.delete()) {
                return true;
            }
            if (!file.exists()) {
                return false;
            }
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException ignored) {
            }
        }
        return false;
    }

    public void executeCompileTask(
        CompileTask compileTask,
        CompileScope scope,
        String contentName,
        Runnable onTaskFinished
    ) {
        CompilerTask task = new CompilerTask(myProject, contentName, true, isCompilationStartedAutomatically(scope));
        CompileContextImpl compileContext = new CompileContextImpl(myProject, task, scope, null, false, false);

        FileDocumentManager.getInstance().saveAllDocuments();

        task.start(() -> {
            try {
                compileTask.execute(compileContext);
            }
            catch (ProcessCanceledException ex) {
                // suppressed
            }
            finally {
                if (onTaskFinished != null) {
                    onTaskFinished.run();
                }

                task.setEndCompilationStamp(
                    compileContext.getMessageCount(CompilerMessageCategory.ERROR) > 0 ? ExitStatus.ERRORS : ExitStatus.SUCCESS,
                    System.currentTimeMillis()
                );
            }
        });
    }

    private boolean executeCompileTasks(CompileContext context, boolean beforeTasks) {
        CompilerManager manager = CompilerManager.getInstance(myProject);
        ProgressIndicator progressIndicator = context.getProgressIndicator();
        progressIndicator.pushState();
        try {
            List<? extends CompileTask> tasks = beforeTasks ? manager.getBeforeTasks() : manager.getAfterTasks();
            if (!tasks.isEmpty()) {
                progressIndicator.setTextValue(
                    beforeTasks ? CompilerLocalize.progressExecutingPrecompileTasks() : CompilerLocalize.progressExecutingPostcompileTasks()
                );
                for (CompileTask task : tasks) {
                    if (!task.execute(context)) {
                        return false;
                    }
                }
            }
        }
        finally {
            progressIndicator.popState();
            WindowManager.getInstance().getStatusBar(myProject).setInfo("");
        }
        return true;
    }

    @RequiredUIAccess
    private boolean validateCompilerConfiguration(CompileScope scope, boolean checkOutputAndSourceIntersection) {
        try {
            Module[] scopeModules = scope.getAffectedModules()/*ModuleManager.getInstance(myProject).getModules()*/;
            List<String> modulesWithoutOutputPathSpecified = new ArrayList<>();
            boolean isProjectCompilePathSpecified = true;
            Set<File> nonExistingOutputPaths = new HashSet<>();
            CompilerManager compilerManager = CompilerManager.getInstance(myProject);
            for (Module module : scopeModules) {
                boolean isEmpty = true;
                for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(LanguageContentFolderScopes.productionAndTest())) {
                    if (hasContent(module, contentFolderType)) {
                        isEmpty = false;
                        break;
                    }
                }

                if (isEmpty) {
                    continue;
                }

                for (ContentFolderTypeProvider contentFolderType : ContentFolderTypeProvider.filter(LanguageContentFolderScopes.productionAndTest())) {
                    if (hasContent(module, contentFolderType)) {

                        String outputPath = getModuleOutputPath(module, contentFolderType);
                        if (outputPath != null) {
                            File file = new File(FileUtil.toSystemDependentName(outputPath));
                            if (!file.exists()) {
                                nonExistingOutputPaths.add(file);
                            }
                        }
                        else {
                            modulesWithoutOutputPathSpecified.add(module.getName());
                        }
                    }
                }

                var point = module.getExtensionPoint(ModuleAdditionalOutputDirectoriesProvider.class);
                point.forEachExtensionSafe(provider -> {
                    for (ModuleAdditionalOutputDirectory directory : provider.getOutputDirectories()) {
                        File file = new File(directory.path());
                        if (!file.exists()) {
                            nonExistingOutputPaths.add(file);
                        }
                    }
                });
            }

            if (!isProjectCompilePathSpecified) {
                LocalizeValue message = CompilerLocalize.errorProjectOutputNotSpecified();
                if (Application.get().isUnitTestMode()) {
                    LOG.error(message.get());
                }

                Messages.showMessageDialog(myProject, message.get(), CommonLocalize.titleError().get(), UIUtil.getErrorIcon());
                //FIXME [VISTALL] ProjectSettingsService.getInstance(myProject).openProjectSettings();
                return false;
            }

            if (!modulesWithoutOutputPathSpecified.isEmpty()) {
                showNotSpecifiedError(modulesWithoutOutputPathSpecified, null/*ContentEntriesEditor.NAME*/);
                return false;
            }

            if (!nonExistingOutputPaths.isEmpty()) {
                for (File file : nonExistingOutputPaths) {
                    boolean succeeded = file.mkdirs();
                    if (!succeeded) {
                        if (file.exists()) {
                            // for overlapping paths, this one might have been created as an intermediate path on a previous iteration
                            continue;
                        }
                        Messages.showMessageDialog(
                            myProject,
                            CompilerLocalize.errorFailedToCreateDirectory(file.getPath()).get(),
                            CommonLocalize.titleError().get(),
                            UIUtil.getErrorIcon()
                        );
                        return false;
                    }
                }
                Boolean refreshSuccess = WriteAction.compute(() -> {
                    LocalFileSystem.getInstance().refreshIoFiles(nonExistingOutputPaths);
                    Boolean res = Boolean.TRUE;
                    for (File file : nonExistingOutputPaths) {
                        if (LocalFileSystem.getInstance().findFileByIoFile(file) == null) {
                            res = Boolean.FALSE;
                            break;
                        }
                    }
                    return res;
                });

                if (!refreshSuccess) {
                    return false;
                }
                dropScopesCaches();
            }

            if (checkOutputAndSourceIntersection && myShouldClearOutputDirectory) {
                if (!validateOutputAndSourcePathsIntersection()) {
                    return false;
                }
                // myShouldClearOutputDirectory may change in validateOutputAndSourcePathsIntersection()
                CompilerPathsEx.CLEAR_ALL_OUTPUTS_KEY.set(scope, myShouldClearOutputDirectory);
            }
            else {
                CompilerPathsEx.CLEAR_ALL_OUTPUTS_KEY.set(scope, false);
            }

            Compiler[] allCompilers = compilerManager.getCompilers(Compiler.class);
            for (Compiler compiler : allCompilers) {
                if (!compiler.validateConfiguration(scope)) {
                    LOG.info("Validation with compiler " + compiler.getDescription() + " is failed.");
                    return false;
                }
            }
            return true;
        }
        catch (Throwable e) {
            LOG.info(e);
            return false;
        }
    }

    private static boolean hasContent(Module module, ContentFolderTypeProvider c) {
        ContentEntry[] contentEntries = ModuleRootManager.getInstance(module).getContentEntries();
        for (ContentEntry contentEntry : contentEntries) {
            ContentFolder[] sourceFolders = contentEntry.getFolders(LanguageContentFolderScopes.of(c));
            if (sourceFolders.length > 0) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void showNotSpecifiedError(List<String> modules, String editorNameToSelect) {
        String nameToSelect = null;
        StringBuilder names = new StringBuilder();
        int maxModulesToShow = 10;
        for (String name : modules.size() > maxModulesToShow ? modules.subList(0, maxModulesToShow) : modules) {
            if (nameToSelect == null) {
                nameToSelect = name;
            }
            if (names.length() > 0) {
                names.append(",\n");
            }
            names.append("\"").append(name).append("\"");
        }
        if (modules.size() > maxModulesToShow) {
            names.append(",\n...");
        }
        LocalizeValue message = CompilerLocalize.errorOutputNotSpecified(modules.size(), names.toString());

        LOG.warn(message.get());

        Messages.showMessageDialog(myProject, message.get(), CommonLocalize.titleError().get(), UIUtil.getErrorIcon());
        showConfigurationDialog(nameToSelect, editorNameToSelect);
    }

    @RequiredReadAction
    private boolean validateOutputAndSourcePathsIntersection() {
        Module[] allModules = ModuleManager.getInstance(myProject).getModules();
        List<VirtualFile> allOutputs = new ArrayList<>();
        ContainerUtil.addAll(allOutputs, CompilerPaths.getOutputDirectories(allModules));
        for (Artifact artifact : ArtifactManager.getInstance(myProject).getArtifacts()) {
            ContainerUtil.addIfNotNull(allOutputs, artifact.getOutputFile());
        }
        Set<VirtualFile> affectedOutputPaths = new HashSet<>();
        CompilerUtil.computeIntersectingPaths(myProject, allOutputs, affectedOutputPaths);
        affectedOutputPaths.addAll(ArtifactCompilerUtil.getArtifactOutputsContainingSourceFiles(myProject));

        if (!affectedOutputPaths.isEmpty()) {
            if (CompilerUtil.askUserToContinueWithNoClearing(myProject, affectedOutputPaths)) {
                myShouldClearOutputDirectory = false;
                return true;
            }
            else {
                return false;
            }
        }
        return true;
    }

    @Override
    public Predicate<Compiler> getCompilerFilter() {
        return myCompilerFilter;
    }

    @Override
    public VirtualFile getGenerationOutputDir(IntermediateOutputCompiler compiler, Module module, boolean forTestSources) {
        Couple<VirtualFile> outputs = myGenerationCompilerModuleToOutputDirMap.get(Pair.create(compiler, module));
        return forTestSources ? outputs.getSecond() : outputs.getFirst();
    }

    private void showConfigurationDialog(String moduleNameToSelect, String tabNameToSelect) {
        //FIXME [VISTALL] ProjectSettingsService.getInstance(myProject).showModuleConfigurationDialog(moduleNameToSelect, tabNameToSelect);
    }

    private static VirtualFile lookupVFile(LocalFileSystem lfs, String path) {
        File file = new File(path);

        VirtualFile vFile = lfs.findFileByIoFile(file);
        if (vFile != null) {
            return vFile;
        }

        boolean justCreated = file.mkdirs();
        vFile = lfs.refreshAndFindFileByIoFile(file);

        if (vFile == null) {
            assert false :
                "Virtual file not found for " + file.getPath() + "; mkdirs() exit code is " + justCreated + "; file exists()? " + file.exists();
        }

        return vFile;
    }
}
