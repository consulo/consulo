// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.roots.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.impl.internal.progress.ProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ClearableLazyValue;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionException;
import consulo.ide.impl.idea.openapi.project.CacheUpdateRunner;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.indexing.FileBasedIndexProjectHandler;
import consulo.language.impl.internal.file.FileManagerImpl;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.*;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.impl.internal.DebugStackTrace;
import consulo.project.*;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

@Singleton
@ServiceImpl
public final class PushedFilePropertiesUpdaterImpl extends PushedFilePropertiesUpdater {
    private static final Logger LOG = Logger.getInstance(PushedFilePropertiesUpdater.class);

    private final Project myProject;

    private final ClearableLazyValue<List<FilePropertyPusher>> myFilePushers = ClearableLazyValue.create(() -> {
        return ContainerUtil.findAll(FilePropertyPusher.EP_NAME.getExtensionList(), pusher -> !pusher.pushDirectoriesOnly());
    });

    private final Queue<Runnable> myTasks = new ConcurrentLinkedQueue<>();

    @Inject
    public PushedFilePropertiesUpdaterImpl(@Nonnull Project project) {
        myProject = project;
    }

    public void processAfterVfsChanges(@Nonnull List<? extends VFileEvent> events) {
        List<Runnable> syncTasks = new ArrayList<>();
        List<Runnable> delayedTasks = new ArrayList<>();
        for (VFileEvent event : events) {
            if (event instanceof VFileCreateEvent) {
                boolean isDirectory = ((VFileCreateEvent)event).isDirectory();
                List<FilePropertyPusher> pushers = isDirectory ? FilePropertyPusher.EP_NAME.getExtensionList() : myFilePushers.getValue();

                if (!event.isFromRefresh()) {
                    ContainerUtil.addIfNotNull(syncTasks, createRecursivePushTask(event, pushers));
                }
                else {
                    boolean isProjectOrWorkspaceFile =
                        VfsUtilCore.findContainingDirectory(((VFileCreateEvent)event).getParent(), Project.DIRECTORY_STORE_FOLDER) != null;
                    if (!isProjectOrWorkspaceFile) {
                        ContainerUtil.addIfNotNull(delayedTasks, createRecursivePushTask(event, pushers));
                    }
                }
            }
            else if (event instanceof VFileMoveEvent || event instanceof VFileCopyEvent) {
                VirtualFile file = getFile(event);
                if (file == null) {
                    continue;
                }
                boolean isDirectory = file.isDirectory();
                List<FilePropertyPusher> pushers = isDirectory ? FilePropertyPusher.EP_NAME.getExtensionList() : myFilePushers.getValue();
                for (FilePropertyPusher<?> pusher : pushers) {
                    file.putUserData(pusher.getFileDataKey(), null);
                }
                ContainerUtil.addIfNotNull(syncTasks, createRecursivePushTask(event, pushers));
            }
        }
        boolean pushingSomethingSynchronously =
            !syncTasks.isEmpty() && syncTasks.size() < FileBasedIndexProjectHandler.ourMinFilesToStartDumMode;
        if (pushingSomethingSynchronously) {
            // push synchronously to avoid entering dumb mode in the middle of a meaningful write action
            // when only a few files are created/moved
            syncTasks.forEach(Runnable::run);
        }
        else {
            delayedTasks.addAll(syncTasks);
        }
        if (!delayedTasks.isEmpty()) {
            queueTasks(delayedTasks);
        }
        if (pushingSomethingSynchronously) {
            GuiUtils.invokeLaterIfNeeded(() -> scheduleDumbModeReindexingIfNeeded(), myProject.getApplication().getDefaultModalityState());
        }
    }

    private static VirtualFile getFile(@Nonnull VFileEvent event) {
        VirtualFile file = event.getFile();
        if (event instanceof VFileCopyEvent) {
            file = ((VFileCopyEvent)event).getNewParent().findChild(((VFileCopyEvent)event).getNewChildName());
        }
        return file;
    }

    @Override
    public void initializeProperties() {
        myProject.getMessageBus().connect().subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(@Nonnull ModuleRootEvent event) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(new Throwable("Processing roots changed event (caused by file type change: " + event.isCausedByFileTypesChange() + ")"));
                }

                myProject.getApplication().getExtensionPoint(FilePropertyPusher.class).forEachExtensionSafe(filePropertyPusher -> {
                    filePropertyPusher.afterRootsChanged(myProject);
                });
            }
        });

        myProject.getApplication().getExtensionPoint(FilePropertyPusher.class).forEachExtensionSafe(pusher -> pusher.initExtra(myProject));
    }

    @Override
    public void pushAllPropertiesNow() {
        performPushTasks();
        doPushAll(FilePropertyPusher.EP_NAME.getExtensionList());
    }

    @Nullable
    private Runnable createRecursivePushTask(@Nonnull VFileEvent event, @Nonnull List<? extends FilePropertyPusher> pushers) {
        if (pushers.isEmpty()) {
            return null;
        }

        return () -> {
            // delay calling event.getFile() until background to avoid expensive VFileCreateEvent.getFile() in EDT
            VirtualFile dir = getFile(event);
            final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
            if (dir != null && ReadAction.compute(() -> fileIndex.isInContent(dir)) && !ProjectCoreUtil.isProjectOrWorkspaceFile(dir)) {
                doPushRecursively(dir, pushers, fileIndex);
            }
        };
    }

    private void doPushRecursively(VirtualFile dir, @Nonnull List<? extends FilePropertyPusher> pushers, ProjectFileIndex fileIndex) {
        fileIndex.iterateContentUnderDirectory(dir, fileOrDir -> {
            applyPushersToFile(fileOrDir, pushers, null);
            return true;
        });
    }

    private void queueTasks(@Nonnull List<? extends Runnable> actions) {
        actions.forEach(myTasks::offer);
        DumbModeTask task = new DumbModeTask(this) {
            @Override
            public void performInDumbMode(@Nonnull ProgressIndicator indicator, Exception trace) {
                performPushTasks();
            }
        };
        myProject.getMessageBus().connect(task).subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(@Nonnull ModuleRootEvent event) {
                DumbService.getInstance(myProject).cancelTask(task);
            }
        });
        DumbService.getInstance(myProject).queueTask(task);
    }

    private void performPushTasks() {
        boolean hadTasks = false;
        while (true) {
            Runnable task = myTasks.poll();
            if (task == null) {
                break;
            }
            try {
                task.run();
                hadTasks = true;
            }
            catch (ProcessCanceledException e) {
                queueTasks(Collections.singletonList(task)); // reschedule dumb mode and ensure the canceled task is enqueued again
                throw e;
            }
        }

        if (hadTasks) {
            scheduleDumbModeReindexingIfNeeded();
        }
    }

    private void scheduleDumbModeReindexingIfNeeded() {
        if (myProject.isDisposed()) {
            return;
        }

        DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(myProject);
        if (task != null) {
            DumbService.getInstance(myProject).queueTask(task);
        }
    }

    @Override
    public void filePropertiesChanged(@Nonnull VirtualFile fileOrDir, @Nonnull Predicate<? super VirtualFile> acceptFileCondition) {
        if (fileOrDir.isDirectory()) {
            for (VirtualFile child : fileOrDir.getChildren()) {
                if (!child.isDirectory() && acceptFileCondition.test(child)) {
                    filePropertiesChanged(child);
                }
            }
        }
        else if (acceptFileCondition.test(fileOrDir)) {
            filePropertiesChanged(fileOrDir);
        }
    }

    private static <T> T findPusherValuesUpwards(Project project, VirtualFile dir, FilePropertyPusher<? extends T> pusher, T moduleValue) {
        final T value = pusher.getImmediateValue(project, dir);
        if (value != null) {
            return value;
        }
        if (moduleValue != null) {
            return moduleValue;
        }
        return findPusherValuesFromParent(project, dir, pusher);
    }

    private static <T> T findPusherValuesUpwards(Project project, VirtualFile dir, FilePropertyPusher<? extends T> pusher) {
        final T userValue = dir.getUserData(pusher.getFileDataKey());
        if (userValue != null) {
            return userValue;
        }
        final T value = pusher.getImmediateValue(project, dir);
        if (value != null) {
            return value;
        }
        return findPusherValuesFromParent(project, dir, pusher);
    }

    private static <T> T findPusherValuesFromParent(Project project, VirtualFile dir, FilePropertyPusher<? extends T> pusher) {
        final VirtualFile parent = dir.getParent();
        if (parent != null && ProjectFileIndex.getInstance(project).isInContent(parent)) {
            return findPusherValuesUpwards(project, parent, pusher);
        }
        T projectValue = pusher.getImmediateValue(project, null);
        return projectValue != null ? projectValue : pusher.getDefaultValue();
    }

    @Override
    public void pushAll(@Nonnull FilePropertyPusher<?>... pushers) {
        queueTasks(Collections.singletonList(() -> doPushAll(Arrays.asList(pushers))));
    }

    private void doPushAll(@Nonnull List<? extends FilePropertyPusher> pushers) {
        Module[] modules = ReadAction.compute(() -> ModuleManager.getInstance(myProject).getModules());

        List<Runnable> tasks = new ArrayList<>();

        for (final Module module : modules) {
            Runnable iteration = ReadAction.<Runnable, RuntimeException>compute(() -> {
                if (module.isDisposed()) {
                    return EmptyRunnable.INSTANCE;
                }
                ProgressManager.checkCanceled();

                final Object[] moduleValues = new Object[pushers.size()];
                for (int i = 0; i < moduleValues.length; i++) {
                    moduleValues[i] = pushers.get(i).getImmediateValue(module);
                }

                final ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
                return () -> fileIndex.iterateContent(fileOrDir -> {
                    applyPushersToFile(fileOrDir, pushers, moduleValues);
                    return true;
                });
            });
            tasks.add(iteration);
        }

        invokeConcurrentlyIfPossible(new Exception(), myProject.getApplication(), tasks);
    }

    public static void invokeConcurrentlyIfPossible(Exception exception, Application application, List<? extends Runnable> tasks) {
        if (tasks.size() == 1 || application.isWriteAccessAllowed()) {
            for (Runnable r : tasks) r.run();
            return;
        }

        final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();

        final ConcurrentLinkedQueue<Runnable> tasksQueue = new ConcurrentLinkedQueue<>(tasks);
        List<Future<?>> results = new ArrayList<>();
        if (tasks.size() > 1) {
            int numThreads = Math.max(Math.min(CacheUpdateRunner.indexingThreadCount() - 1, tasks.size() - 1), 1);

            for (int i = 0; i < numThreads; ++i) {
                results.add(application.executeOnPooledThread(() -> ProgressManager.getInstance().runProcess(() -> {
                    DebugStackTrace.with(exception, () -> {
                        Runnable runnable;
                        while ((runnable = tasksQueue.poll()) != null) runnable.run();
                    });
                }, ProgressWrapper.wrap(progress))));
            }
        }

        Runnable runnable;
        while ((runnable = tasksQueue.poll()) != null) runnable.run();

        for (Future<?> result : results) {
            try {
                result.get();
            }
            catch (InterruptedException ex) {
                throw new ProcessCanceledException(ex);
            }
            catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof ProcessCanceledException) {
                    throw (ProcessCanceledException)cause;
                }

                LOG.error(ex);
            }
            catch (Exception ex) {
                LOG.error(ex);
            }
        }
    }

    private void applyPushersToFile(
        final VirtualFile fileOrDir,
        @Nonnull List<? extends FilePropertyPusher> pushers,
        final Object[] moduleValues
    ) {
        ApplicationManager.getApplication().runReadAction(() -> {
            ProgressManager.checkCanceled();
            if (!fileOrDir.isValid()) {
                return;
            }
            doApplyPushersToFile(fileOrDir, pushers, moduleValues);
        });
    }

    private void doApplyPushersToFile(
        @Nonnull VirtualFile fileOrDir,
        @Nonnull List<? extends FilePropertyPusher> pushers,
        Object[] moduleValues
    ) {
        FilePropertyPusher<Object> pusher = null;
        try {
            final boolean isDir = fileOrDir.isDirectory();
            for (int i = 0; i < pushers.size(); i++) {
                //noinspection unchecked
                pusher = (FilePropertyPusher<Object>)pushers.get(i);
                if (isDir ? !pusher.acceptsDirectory(fileOrDir, myProject) : pusher.pushDirectoriesOnly() || !pusher.acceptsFile(
                    fileOrDir,
                    myProject
                )) {
                    continue;
                }
                Object value = moduleValues != null ? moduleValues[i] : null;
                findAndUpdateValue(fileOrDir, pusher, value);
            }
        }
        catch (AbstractMethodError ame) { // acceptsDirectory is missed
            if (pusher != null) {
                throw new ExtensionException(pusher.getClass());
            }
            throw ame;
        }
    }

    @Override
    public <T> void findAndUpdateValue(final VirtualFile fileOrDir, final FilePropertyPusher<T> pusher, final T moduleValue) {
        final T value = findPusherValuesUpwards(myProject, fileOrDir, pusher, moduleValue);
        updateValue(myProject, fileOrDir, value, pusher);
    }

    public static <T> void updateValue(
        final Project project,
        final VirtualFile fileOrDir,
        final T value,
        final FilePropertyPusher<T> pusher
    ) {
        final T oldValue = fileOrDir.getUserData(pusher.getFileDataKey());
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
    public void filePropertiesChanged(@Nonnull final VirtualFile file) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        FileBasedIndex.getInstance().requestReindex(file);
        for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
            reloadPsi(file, project);
        }
    }

    private static void reloadPsi(final VirtualFile file, final Project project) {
        final FileManagerImpl fileManager = (FileManagerImpl)PsiManagerEx.getInstanceEx(project).getFileManager();
        if (fileManager.findCachedViewProvider(file) != null) {
            Runnable runnable = () -> WriteAction.run(() -> fileManager.forceReload(file));
            if (ApplicationManager.getApplication().isDispatchThread()) {
                runnable.run();
            }
            else {
                project.getUIAccess().give(runnable);
            }
        }
    }
}
