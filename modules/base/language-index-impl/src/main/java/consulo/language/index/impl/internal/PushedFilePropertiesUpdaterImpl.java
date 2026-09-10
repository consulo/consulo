// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.internal.ProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.content.ContentIterator;
import consulo.language.impl.internal.file.FileManagerImpl;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.index.impl.internal.gist.GistManagerImpl;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.IndexableFilesDeduplicateFilter;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.ModuleIndexableFilesIteratorImpl;
import consulo.language.index.impl.internal.roots.ProjectIndexableFilesIteratorImpl;
import consulo.language.index.impl.internal.roots.kind.IndexableSetOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.gist.GistManager;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.FilePropertyPusher;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.PushedFilePropertiesUpdater;
import consulo.module.content.internal.ModuleRootEventImpl;
import consulo.module.content.internal.PushedFilePropertiesUpdaterInternal;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.internal.DebugStackTrace;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.project.ProjectManager;
import consulo.project.RootsChangeRescanningInfo;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public final class PushedFilePropertiesUpdaterImpl implements PushedFilePropertiesUpdaterInternal {
    private static final Logger LOG = Logger.getInstance(PushedFilePropertiesUpdater.class);

    private static final int SCANNING_PARALLELISM = UnindexedFilesUpdater.getNumberOfScanningThreads();

    @FunctionalInterface
    private interface TaskFactory {
        List<Runnable> getTasks();
    }

    private final Project myProject;

    private final Queue<TaskFactory> myTasks = new ConcurrentLinkedQueue<>();

    @Inject
    public PushedFilePropertiesUpdaterImpl(Project project) {
        myProject = project;
    }

    @Override
    public void processAfterVfsChanges(List<? extends VFileEvent> events) {
        List<TaskFactory> syncTasks = new ArrayList<>();
        List<TaskFactory> delayedTasks = new ArrayList<>();
        List<FilePropertyPusher<?>> filePushers = getFilePushers(myProject.getApplication());

        // this is useful for debugging. Especially in integration tests: it is often clear why large file sets have changed
        // (e.g. imported modules or jdk), but it is often unclear why small file sets change and what these files are.
        if (LOG.isDebugEnabled()) {
            LOG.debug(events.size() + " file(s) changed");
            if (events.size() < 20) {
                for (VFileEvent event : events) {
                    LOG.debug("File changed: " + event.getPath() + ".\nrequestor:" + event.getRequestor() + "\nevent:" + event);
                }
            }
        }

        for (VFileEvent event : events) {
            if (event instanceof VFileCreateEvent fileCreateEvent) {
                boolean isDirectory = fileCreateEvent.isDirectory();
                List<FilePropertyPusher<?>> pushers = isDirectory ? getAllPushers(myProject.getApplication()) : filePushers;

                if (!fileCreateEvent.isFromRefresh()) {
                    Runnable recursiveTask = createRecursivePushTask(fileCreateEvent, pushers);
                    if (recursiveTask != null) {
                        syncTasks.add(() -> List.of(recursiveTask));
                    }
                }
                else {
                    boolean isProjectOrWorkspaceFile =
                        VirtualFileUtil.findContainingDirectory(fileCreateEvent.getParent(), Project.DIRECTORY_STORE_FOLDER) != null;
                    if (!isProjectOrWorkspaceFile) {
                        Runnable recursiveTask = createRecursivePushTask(fileCreateEvent, pushers);
                        if (recursiveTask != null) {
                            delayedTasks.add(() -> List.of(recursiveTask));
                        }
                    }
                }
            }
            else if (event instanceof VFileMoveEvent || event instanceof VFileCopyEvent) {
                VirtualFile file = getFile(event);
                if (file == null) {
                    continue;
                }
                boolean isDirectory = file.isDirectory();
                List<FilePropertyPusher<?>> pushers = isDirectory ? getAllPushers(myProject.getApplication()) : filePushers;
                for (FilePropertyPusher<?> pusher : pushers) {
                    file.putUserData(pusher.getFileDataKey(), null);
                }
                Runnable recursiveTask = createRecursivePushTask(event, pushers);
                if (recursiveTask != null) {
                    syncTasks.add(() -> List.of(recursiveTask));
                }
            }
        }
        boolean pushingSomethingSynchronously =
            !syncTasks.isEmpty() && syncTasks.size() < FileBasedIndexProjectHandler.ourMinFilesToStartDumMode;
        if (pushingSomethingSynchronously) {
            // push synchronously to avoid entering dumb mode in the middle of a meaningful write action
            // when only a few files are created/moved
            for (TaskFactory task : syncTasks) {
                runConcurrentlyIfPossible(task.getTasks());
            }
        }
        else {
            delayedTasks.addAll(syncTasks);
        }
        if (!delayedTasks.isEmpty()) {
            queueTasks(delayedTasks, "Push on VFS changes");
        }
        if (pushingSomethingSynchronously) {
            Application app = myProject.getApplication();
            if (app.isDispatchThread()) {
                scheduleDumbModeReindexingIfNeeded();
            }
            else {
                app.invokeLater(this::scheduleDumbModeReindexingIfNeeded, myProject.getDisposed());
            }
        }
    }

    @Override
    public void runConcurrentlyIfPossible(List<? extends Runnable> tasks) {
        invokeConcurrentlyIfPossible(tasks);
    }

    @Override
    public void initializeProperties() {
        myProject.getMessageBus().connect().subscribe(
            ModuleRootListener.class,
            new ModuleRootListener() {
                @Override
                public void rootsChanged(ModuleRootEvent event) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(new Throwable("Processing roots changed event (caused by file type change: "
                            + event.isCausedByFileTypesChange() + ")"));
                    }
                    for (FilePropertyPusher<?> pusher : getAllPushers(myProject.getApplication())) {
                        pusher.afterRootsChanged(myProject);
                    }
                }
            }
        );

        myProject.getApplication().getExtensionPoint(FilePropertyPusher.class).forEachExtensionSafe(pusher -> pusher.initExtra(myProject));
    }

    private @Nullable Runnable createRecursivePushTask(VFileEvent event, List<? extends FilePropertyPusher<?>> pushers) {
        if (pushers.isEmpty()) {
            return null;
        }

        return () -> {
            // delay calling event.getFile() until background to avoid expensive VFileCreateEvent.getFile() in EDT
            VirtualFile dir = getFile(event);
            ProjectFileIndex fileIndex = ReadAction.compute(() -> ProjectFileIndex.getInstance(myProject));
            if (dir != null && ReadAction.compute(() -> fileIndex.isInContent(dir)) && !ProjectCoreUtil.isProjectOrWorkspaceFile(dir)) {
                doPushRecursively(pushers, new ProjectIndexableFilesIteratorImpl(dir));
            }
        };
    }

    private void doPushRecursively(List<? extends FilePropertyPusher<?>> pushers, IndexableFilesIterator indexableFilesIterator) {
        indexableFilesIterator.iterateFiles(myProject, fileOrDir -> {
            applyPushersToFile(fileOrDir, pushers, null);
            return true;
        }, IndexableFilesDeduplicateFilter.create());
    }

    private void queueTasks(List<? extends TaskFactory> actions, String reason) {
        actions.forEach(myTasks::offer);
        DumbModeTask task = new MyDumbModeTask(reason, this);
        myProject.getMessageBus().connect(task).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                for (RootsChangeRescanningInfo info : ((ModuleRootEventImpl) event).getInfos()) {
                    if (info == RootsChangeRescanningInfo.TOTAL_RESCAN) {
                        DumbService.getInstance(myProject).cancelTask(task);
                        return;
                    }
                }
            }
        });
        task.queue(myProject);
    }

    public void performDelayedPushTasks() {
        boolean hadTasks = false;
        while (true) {
            ProgressManager.checkCanceled();
            TaskFactory task = myTasks.poll();
            if (task == null) {
                break;
            }
            try {
                invokeConcurrentlyIfPossible(task.getTasks());
                hadTasks = true;
            }
            catch (Throwable e) {
                if (e instanceof ProcessCanceledException) {
                    // reschedule dumb mode and ensure the canceled task is enqueued again
                    queueTasks(List.of(task), "Rerun pushing tasks after process cancelled");
                }

                throw e;
            }
        }

        if (hadTasks) {
            scheduleDumbModeReindexingIfNeeded();
        }
    }

    private void scheduleDumbModeReindexingIfNeeded() {
        FileBasedIndexProjectHandler.scheduleReindexingInDumbMode(myProject);
    }

    @Override
    @RequiredReadAction
    public void filePropertiesChanged(VirtualFile fileOrDir, Predicate<? super VirtualFile> acceptFileCondition) {
        if (fileOrDir.isDirectory()) {
            for (VirtualFile child : fileOrDir.getRequiredChildren()) {
                if (!child.isDirectory() && acceptFileCondition.test(child)) {
                    filePropertiesChanged(child);
                }
            }
        }
        else if (acceptFileCondition.test(fileOrDir)) {
            filePropertiesChanged(fileOrDir);
        }
    }

    @Override
    public void pushAll(FilePropertyPusher<?>... pushers) {
        if (!UnindexedFilesScannerStartup.isFirstProjectScanningRequested(myProject)) {
            LOG.info("Ignoring push request, as project is not yet initialized");
            return;
        }
        queueTasks(List.of(() -> doPushAll(List.of(pushers))), "Push all on " + Arrays.toString(pushers));
    }

    private List<Runnable> doPushAll(List<? extends FilePropertyPusher<?>> pushers) {
        return generateScanTasks(myProject, module -> {
            Object[] moduleValues = getModuleImmediateValues(pushers, module);
            return fileOrDir -> {
                applyPushersToFile(fileOrDir, pushers, moduleValues);
                return true;
            };
        });
    }

    public void applyPushersToFile(VirtualFile fileOrDir, List<? extends FilePropertyPusher<?>> pushers, Object @Nullable [] moduleValues) {
        if (pushers.isEmpty()) {
            return;
        }
        if (fileOrDir.isDirectory()) {
            fileOrDir.getChildren(); // outside read action to avoid freezes
        }

        ReadAction.run(() -> {
            if (!fileOrDir.isValid() || !(fileOrDir instanceof VirtualFileWithId)) {
                return;
            }
            doApplyPushersToFile(fileOrDir, pushers, moduleValues);
        });
    }

    @RequiredReadAction
    private void doApplyPushersToFile(
        VirtualFile fileOrDir,
        List<? extends FilePropertyPusher<?>> pushers,
        Object @Nullable [] moduleValues
    ) {
        boolean isDir = fileOrDir.isDirectory();
        for (int i = 0; i < pushers.size(); i++) {
            @SuppressWarnings("unchecked")
            FilePropertyPusher<Object> pusher = (FilePropertyPusher<Object>) pushers.get(i);
            boolean notApplicable = isDir
                ? !pusher.acceptsDirectory(fileOrDir, myProject)
                : pusher.pushDirectoriesOnly() || !pusher.acceptsFile(fileOrDir, myProject);
            if (notApplicable) {
                continue;
            }
            Object value = moduleValues != null ? moduleValues[i] : null;
            findAndUpdateValue(fileOrDir, pusher, value);
        }
    }

    @Override
    public <T> void findAndUpdateValue(VirtualFile fileOrDir, FilePropertyPusher<T> pusher, @Nullable T moduleValue) {
        T newValue = findNewPusherValue(myProject, fileOrDir, pusher, moduleValue);
        updateValue(myProject, fileOrDir, newValue, pusher);
    }

    public static <T> void updateValue(Project project, VirtualFile fileOrDir, T value, FilePropertyPusher<T> pusher) {
        T oldValue = fileOrDir.getUserData(pusher.getFileDataKey());
        if (value != oldValue) {
            fileOrDir.putUserData(pusher.getFileDataKey(), value);
            try {
                pusher.persistAttribute(project, fileOrDir, value);
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    @Override
    @RequiredReadAction
    public void filePropertiesChanged(VirtualFile file) {
        Application.get().assertReadAccessAllowed();
        FileBasedIndex.getInstance().requestReindex(file);
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            reloadPsi(file, project);
        }
    }

    private static class MyDumbModeTask extends DumbModeTask {
        private final String myReason;
        private final PushedFilePropertiesUpdaterImpl myUpdater;

        private MyDumbModeTask(String reason, PushedFilePropertiesUpdaterImpl updater) {
            myReason = reason;
            myUpdater = updater;
        }

        @Override
        public void performInDumbMode(ProgressIndicator indicator, Exception trace) {
            indicator.setIndeterminate(true);
            indicator.setText(IndexingLocalize.progressIndexingScanning());
            ((GistManagerImpl) GistManager.getInstance()).runWithMergingDependentCacheInvalidations(myUpdater::performDelayedPushTasks);
        }

        @Override
        public @Nullable DumbModeTask tryMergeWith(DumbModeTask taskFromQueue) {
            if (taskFromQueue instanceof MyDumbModeTask task && task.myUpdater == myUpdater) {
                return this;
            }
            return null;
        }

        @Override
        public String toString() {
            return super.toString() + " (reason: " + myReason + ")";
        }
    }

    private static @Nullable VirtualFile getFile(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (event instanceof VFileCopyEvent fileCopyEvent) {
            file = fileCopyEvent.findCreatedFile();
        }
        return file;
    }

    private static <T> T findNewPusherValue(
        Project project,
        VirtualFile fileOrDir,
        FilePropertyPusher<? extends T> pusher,
        @Nullable T moduleValue
    ) {
        //Do not check fileOrDir.getUserData() as it may be outdated.
        T immediateValue = pusher.getImmediateValue(project, fileOrDir);
        if (immediateValue != null) {
            return immediateValue;
        }
        if (moduleValue != null) {
            return moduleValue;
        }
        return findNewPusherValueFromParent(project, fileOrDir, pusher);
    }

    private static <T> T findNewPusherValueFromParent(Project project, VirtualFile fileOrDir, FilePropertyPusher<? extends T> pusher) {
        VirtualFile parent = fileOrDir.getParent();
        if (parent != null && ProjectFileIndex.getInstance(project).isInContent(parent)) {
            T userValue = parent.getUserData(pusher.getFileDataKey());
            if (userValue != null) {
                return userValue;
            }
            return findNewPusherValue(project, parent, pusher, null);
        }
        T projectValue = pusher.getImmediateValue(project, null);
        return projectValue != null ? projectValue : pusher.getDefaultValue();
    }

    public static Object[] getModuleImmediateValues(List<? extends FilePropertyPusher<?>> pushers, Module module) {
        Object[] moduleValues = new Object[pushers.size()];
        for (int i = 0; i < moduleValues.length; i++) {
            moduleValues[i] = pushers.get(i).getImmediateValue(module);
        }
        return moduleValues;
    }

    public static Object[] getImmediateValuesEx(List<? extends FilePropertyPusherEx<?>> pushers, IndexableSetOrigin origin) {
        Object[] moduleValues = new Object[pushers.size()];
        for (int i = 0; i < moduleValues.length; i++) {
            moduleValues[i] = pushers.get(i).getImmediateValueEx(origin);
        }
        return moduleValues;
    }

    private static List<Runnable> generateScanTasks(Project project, Function<? super Module, ? extends ContentIterator> iteratorProducer) {
        IndexableFilesDeduplicateFilter indexableFilesDeduplicateFilter = IndexableFilesDeduplicateFilter.create();
        return ReadAction.nonBlocking(() -> {
            List<Runnable> runnables = new ArrayList<>();
            Set<VirtualFile> processedRoots = new HashSet<>();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                if (module.isDisposed()) {
                    continue;
                }
                for (ModuleIndexableFilesIteratorImpl iterator : ModuleIndexableFilesIteratorImpl.getModuleIterators(module)) {
                    if (processedRoots.addAll(iterator.getOrigin().getRoots())) {
                        ContentIterator contentIterator = iteratorProducer.apply(module);
                        runnables.add(() -> iterator.iterateFiles(project, contentIterator, indexableFilesDeduplicateFilter));
                    }
                }
            }
            return runnables;
        }).executeSynchronously();
    }

    public static void scanProject(Project project, Function<? super Module, ? extends ContentIterator> iteratorProducer) {
        List<Runnable> tasks = generateScanTasks(project, iteratorProducer);
        invokeConcurrentlyIfPossible(tasks);
    }

    public static void invokeConcurrentlyIfPossible(List<? extends Runnable> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        Application application = Application.get();
        boolean synchronous = tasks.size() == 1 || application.isWriteAccessAllowed();

        if (synchronous) {
            for (Runnable r : tasks) {
                r.run();
            }
            return;
        }

        Exception outerTrace = DebugStackTrace.getTrace();
        Exception trace = outerTrace != null ? outerTrace : new Exception();
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();

        Queue<Runnable> tasksQueue = new ConcurrentLinkedQueue<>(tasks);
        List<Future<?>> results = new ArrayList<>();
        int numThreads = Math.max(Math.min(SCANNING_PARALLELISM - 1, tasks.size() - 1), 1);

        for (int i = 0; i < numThreads; ++i) {
            results.add(application.executeOnPooledThread(() -> ProgressManager.getInstance().runProcess(
                () -> DebugStackTrace.with(
                    trace,
                    () -> {
                        Runnable runnable;
                        while ((runnable = tasksQueue.poll()) != null) {
                            runnable.run();
                        }
                    }
                ),
                ProgressWrapper.wrap(progress)
            )));
        }

        Runnable runnable;
        while ((runnable = tasksQueue.poll()) != null) {
            runnable.run();
        }

        for (Future<?> result : results) {
            try {
                result.get();
            }
            catch (InterruptedException ex) {
                throw new ProcessCanceledException(ex);
            }
            catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof ProcessCanceledException processCanceledException) {
                    throw processCanceledException;
                }

                LOG.error(ex);
            }
            catch (Exception ex) {
                LOG.error(ex);
            }
        }
    }

    @RequiredReadAction
    private static void reloadPsi(VirtualFile file, Project project) {
        FileManagerImpl fileManager = (FileManagerImpl) PsiManagerEx.getInstanceEx(project).getFileManager();
        if (fileManager.findCachedViewProvider(file) != null) {
            Runnable runnable = () -> WriteAction.run(() -> fileManager.forceReload(file));
            if (UIAccess.isUIThread()) {
                runnable.run();
            }
            else {
                project.getUIAccess().give(runnable);
            }
        }
    }

    static List<FilePropertyPusher<?>> getAllPushers(Application application) {
        return application.getExtensionPoint(FilePropertyPusher.class).collectMapped(pusher -> pusher);
    }

    private static List<FilePropertyPusher<?>> getFilePushers(Application application) {
        return application.getExtensionPoint(FilePropertyPusher.class)
            .collectMapped(pusher -> pusher.pushDirectoriesOnly() ? null : pusher);
    }
}
