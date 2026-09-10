// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Please, don't use this service. Use {@link DumbService#runWhenSmart} to schedule runnables in smart mode.
 * <p>
 * Scheduled runnables will be executed on EDT thread after all three conditions met:
 * 1. There is no scanning in progress, and initial project scanning (on project open) has finished
 * 2. There is no dumb mode
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class SmartModeScheduler implements Disposable {
    public static final Logger LOG = Logger.getInstance(SmartModeScheduler.class);
    public static final int SCANNING = 1;
    public static final int DUMB = 1 << 1;

    private final Deque<Runnable> myRunWhenSmartQueue = new ConcurrentLinkedDeque<>();

    private final Application myApplication;
    private final Project myProject;
    private final UnindexedFilesScannerExecutor myFilesScannerExecutor;
    private final ObservableValue<DumbService.DumbState> myProjectDumbState;
    private final ObservableValue<Integer> myProjectScanningChanged;

    @Inject
    public SmartModeScheduler(Application application, Project project, DumbService dumbService, UnindexedFilesScannerExecutor executor) {
        myApplication = application;
        myProject = project;
        myFilesScannerExecutor = executor;
        myProjectDumbState = dumbService.getState();
        myProjectScanningChanged = executor.startedOrStoppedEvent();

        myProjectScanningChanged.addListener(value -> onStateChanged());
        myProjectDumbState.addListener(value -> onStateChanged());
    }

    private void addLast(Runnable runnable) {
        myRunWhenSmartQueue.addLast(runnable);
    }

    private void onStateChanged() {
        if (canRunSmart()) {
            // Always reschedule execution to avoid unexpected write lock acquired.
            //
            // Note2: DumbService tracks modality by itself: exit event occurs in the same modality as the enter event.
            //        Use default modality here to avoid deadlocks like in WEB-59844 (dumb mode may start and end in non NON_MODAL contexts)
            myApplication.invokeLater(this::runAllWhileSmart, myApplication.getDefaultModalityState(), myProject.getDisposed());
        }
    }

    public boolean canRunSmart() {
        return getCurrentMode() == 0;
    }

    public void runWhenSmart(Runnable runnable) {
        if (canRunSmart() && myApplication.isDispatchThread()) {
            // Execute immediately only because some tests expect this behavior. No production need.
            runnable.run();
        }
        else {
            addLast(runnable);
            onStateChanged();
        }
    }

    private void runAllWhileSmart() {
        // We need EDT or WriteLock to make sure that dumb mode does not start while the method is in progress
        // (see DumbServiceImpl.updateFinished).
        // Note that neither write lock nor EDT are enough to protect against switching to "almost smart": scanning can start at any moment
        //   (it does not need write lock nor EDT), so the code should be ready for scanning to start at any moment.
        myApplication.assertIsDispatchThread();

        // It may happen that one of the pending runWhenSmart actions triggers new dumb mode;
        // in this case we should quit processing pending actions and postpone them until the newly started dumb mode finishes.
        while (canRunSmart()) {
            Runnable runnable = myRunWhenSmartQueue.pollFirst();
            if (runnable == null) {
                break;
            }
            doRun(runnable);
        }
    }

    // Extracted to have a capture point
    private static void doRun(Runnable runnable) {
        try {
            runnable.run();
        }
        catch (ProcessCanceledException e) {
            LOG.error("Task canceled: " + runnable, AttachmentFactory.get().create("pce", e));
        }
        catch (Throwable e) {
            LOG.error("Error executing task " + runnable, e);
        }
    }

    public int getCurrentMode() {
        return (myFilesScannerExecutor.isRunning().get() ? SCANNING : 0) + (myProjectDumbState.get().isDumb() ? DUMB : 0);
    }

    public void clear() {
        myRunWhenSmartQueue.clear();
    }

    @Override
    public void dispose() {
        clear();
    }
}
