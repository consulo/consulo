// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.project.impl.internal;

import consulo.annotation.InheritCallerContext;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.FrequentErrorLogger;
import consulo.application.internal.NoAccessDuringPsiEventsService;
import consulo.application.progress.PingProgress;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.component.util.ModificationTracker;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbModeTask;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.event.DumbModeListenerBackgroundable;
import consulo.project.impl.internal.MergingQueueGuiExecutor.ExecutorStateListener;
import consulo.project.impl.internal.MergingTaskQueue.SubmissionReceipt;
import consulo.project.impl.internal.SingleTaskExecutor.AutoclosableProgressive;
import consulo.project.internal.DumbServiceInternal;
import consulo.project.internal.UnindexedFilesScannerExecutor;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ModalityState;
import consulo.ui.NotificationType;
import consulo.util.collection.Lists;
import consulo.util.concurrent.ConcurrencyUtil;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class DumbServiceImpl extends DumbServiceInternal implements Disposable, ModificationTracker {
    public static final boolean ALWAYS_SMART = Boolean.getBoolean("idea.no.dumb.mode");

    /**
     * Flag to force dumb tasks to work on background thread in tests or synchronous headless mode.
     */
    public static final String IDEA_FORCE_DUMB_QUEUE_TASKS = "idea.force.dumb.queue.tasks";

    private static final Logger LOG = Logger.getInstance(DumbServiceImpl.class);
    private static final FrequentErrorLogger ourErrorLogger = FrequentErrorLogger.newInstance(LOG);

    private final ObservableValue<DumbState> myState;

    private final AtomicBoolean myInitialDumbTaskRequiredForSmartModeSubmitted = new AtomicBoolean(false);

    // diagnostic state that helps to ensure balanced calls of listeners
    private enum DumbModeEventListenerState {
        ENTERED,
        EXITED
    }

    // in the beginning, we have dumb mode
    private final AtomicReference<DumbModeEventListenerState> myDumbModeListenerBackgroundableState =
        new AtomicReference<>(DumbModeEventListenerState.ENTERED);

    // this variable is intended to be used only from the EDT
    private DumbModeEventListenerState myDumbModeListenerState = DumbModeEventListenerState.ENTERED;

    private volatile boolean myIsDisposed;

    // Not thread safe. Should only be accessed from EDT. Launches myGuiDumbTaskRunner at most once.
    // DumbService can invoke `launch` from completeJustSubmittedTasks or from queueTaskOnEdt
    private final class DumbTaskLauncher {
        private final ModalityState myModality;
        private boolean myLaunched;

        private final AtomicBoolean myClosed = new AtomicBoolean(false);

        private volatile @Nullable Throwable myCloseTrace; // for diagnostics

        private DumbTaskLauncher(ModalityState modality) {
            myModality = modality;
        }

        void cancel() {
            // only not launched tasks can be canceled
            if (!myLaunched) {
                myLaunched = true;
                close();
            }
        }

        private void close() {
            if (myClosed.compareAndSet(false, true)) {
                myCloseTrace = new Throwable("Close trace");
                if (myApplication.isDispatchThread()) {
                    myDumbTaskLaunchers.remove(this);
                    // without redispatching, because it can be invoked from completeJustSubmittedTasks
                    decrementDumbCounterBlocking();
                }
                else if (Registry.is("ide.dumb.service.use.background.write.action", true) && myModality.equals(ModalityState.nonModal())) {
                    Coroutine.<Void, Void>first(CodeExecution.run(() -> myDumbTaskLaunchers.remove(this)))
                        .then(decrementDumbCounterSuspending())
                        .runAsync(CoroutineScope.of(myProject.coroutineContext()), null);
                }
                else {
                    myApplication.invokeLater(() -> {
                        myDumbTaskLaunchers.remove(this);
                        decrementDumbCounterBlocking();
                    }, myModality);
                }
            }
            else {
                LOG.error("The task is already closed", new Throwable("Current trace", myCloseTrace));
            }
        }

        void launch() {
            LOG.debug("DumbTaskLauncher is about to launch: " + myLaunched);
            if (!myLaunched) {
                myLaunched = true;
                myGuiDumbTaskRunner.startBackgroundProcess(this::close);
            }
        }
    }

    // We need to track FutureDumbTasks because completeJustSubmittedTasks should
    // not only complete all the dumb tasks, but also should finish dumb mode.
    private final List<DumbTaskLauncher> myDumbTaskLaunchers = Lists.newLockFreeCopyOnWriteList();

    private final Application myApplication;
    private final Project myProject;
    private final DumbModeListener myPublisher;
    private final DumbModeListenerBackgroundable myPublisherBackgroundable;

    private volatile @Nullable Throwable myDumbModeStartTrace;
    private volatile ScheduledTasksScope myScheduledTasksScope = new ScheduledTasksScope();
    private final DumbServiceMergingTaskQueue myTaskQueue = new DumbServiceMergingTaskQueue();
    private final DumbServiceGuiExecutor myGuiDumbTaskRunner;
    private final DumbServiceAlternativeResolveTracker myAlternativeResolveTracker;

    private volatile @Nullable Thread myWaitIntolerantThread;

    private static final class ScheduledTasksScope {
        private final List<Runnable> myCancellations = Lists.newLockFreeCopyOnWriteList();

        Runnable register(Runnable cancellation) {
            myCancellations.add(cancellation);
            return () -> myCancellations.remove(cancellation);
        }

        void cancel() {
            List<Runnable> cancellations = new ArrayList<>(myCancellations);
            myCancellations.clear();
            for (Runnable cancellation : cancellations) {
                cancellation.run();
            }
        }

        boolean hasChildren() {
            return !myCancellations.isEmpty();
        }
    }

    private final class DumbTaskListener implements ExecutorStateListener {
        /*
         * beforeFirstTask and afterLastTask always follow one after another. Receiving several beforeFirstTask or afterLastTask in row is
         * always a failure of DumbServiceGuiTaskQueue.
         * return true to start queue processing, false otherwise
         */
        @Override
        public boolean beforeFirstTask() {
            // if a queue has already been emptied by modal dumb progress, DumbServiceGuiExecutor will not invoke processing on empty queue
            LOG.assertTrue(state().isDumb(), "State should be DUMB, but was " + state());
            return true;
        }

        @Override
        public void afterLastTask(@Nullable SubmissionReceipt latestReceipt) {
        }
    }

    @Inject
    public DumbServiceImpl(Application application, Project project, ApplicationConcurrency concurrency) {
        myApplication = application;
        myProject = project;
        myState = ObservableValue.<DumbState>of(new DumbStateImpl(!project.isDefault(), 0L, 0));
        myPublisher = project.getMessageBus().syncPublisher(DumbModeListener.class);
        myPublisherBackgroundable = project.getMessageBus().syncPublisher(DumbModeListenerBackgroundable.class);

        myGuiDumbTaskRunner = new DumbServiceGuiExecutor(project, myTaskQueue, new DumbTaskListener(), concurrency);
        if (Registry.is("scanning.should.pause.dumb.queue", false)) {
            new DumbServiceScanningListener(project, myGuiDumbTaskRunner.guiSuspender()).subscribe();
        }
        if (Registry.is("vfs.refresh.should.pause.dumb.queue", true)) {
            new DumbServiceVfsBatchListener(application, project, myGuiDumbTaskRunner.guiSuspender());
        }
        myAlternativeResolveTracker = new DumbServiceAlternativeResolveTracker();
        // any project starts in dumb mode (except a default project which is always smart)
        // we assume that queueStartupActivitiesRequiredForSmartMode will be invoked to advance DUMB > SMART
    }

    private DumbStateImpl state() {
        return (DumbStateImpl) myState.get();
    }

    public void queueStartupActivitiesRequiredForSmartMode() {
        if (!myInitialDumbTaskRequiredForSmartModeSubmitted.compareAndSet(false, true)) {
            return;
        }

        InitialDumbTaskRequiredForSmartMode task = new InitialDumbTaskRequiredForSmartMode(myProject);
        queueTask(task);
    }

    @Override
    public void cancelTask(DumbModeTask task) {
        LOG.info("cancel " + task + " [" + myProject.getName() + "]");
        myTaskQueue.cancelTask(task);
    }

    @Override
    public void dispose() {
        myIsDisposed = true;
        myApplication.assertWriteAccessAllowed();
        // cancel the tasks that were about to be scheduled while DumbService.disposed was called
        myScheduledTasksScope.cancel();
        myTaskQueue.disposePendingTasks();
    }

    @Override
    public void suspendIndexingAndRun(LocalizeValue activityName, Runnable activity) {
        myGuiDumbTaskRunner.suspendAndRun(activityName, activity);
    }

    @Override
    public boolean isDumb() {
        if (ALWAYS_SMART) {
            return false;
        }
        if (!myApplication.isReadAccessAllowed() && Registry.is("ide.check.is.dumb.contract")) {
            ourErrorLogger.error(
                "To avoid race conditions isDumb method should be used only under read action or in EDT thread.",
                new IllegalStateException()
            );
        }
        return state().isDumb();
    }

    @Override
    public <T> T runInDumbMode(String debugReason, Supplier<T> block) {
        LOG.info("[" + myProject + "]: running dumb task without visible indicator: " + debugReason);

        Throwable originException = new Throwable();

        boolean counterIncremented = false;
        try {
            // we need correct modality
            // Because we need to avoid additional dispatch. UNDISPATCHED coroutine is not a solution, because
            // multiple UNDISPATCHED coroutines in the same (EDT) thread ends up in some strange state (as revealed by unit tests)
            incrementDumbCounterBlocking(originException);
            counterIncremented = true;
            return block.get();
        }
        finally {
            // in the case of cancellation, this block won't execute if NonCancellable is omitted
            if (counterIncremented) {
                decrementDumbCounterBlocking();
                LOG.info("[" + myProject + "]: finished dumb task without visible indicator: " + debugReason);
            }
        }
    }

    private boolean tryIncrementStateCounter() {
        return ((DumbStateImpl) myState.getAndUpdate(it -> ((DumbStateImpl) it).tryIncrementDumbCounter())).incrementWillChangeDumbState();
    }

    private boolean doIncrementStateCounter() {
        DumbStateImpl old = (DumbStateImpl) myState.getAndUpdate(it -> ((DumbStateImpl) it).incrementDumbCounter());
        boolean isStateChanged = old.isSmart();
        if (isStateChanged) {
            boolean balanced =
                myDumbModeListenerBackgroundableState.compareAndSet(DumbModeEventListenerState.EXITED, DumbModeEventListenerState.ENTERED);
            if (!balanced) {
                LOG.error("Unexpected listener state: dumb mode is going to be entered without exiting");
            }
            runCatchingIgnorePCE(myPublisherBackgroundable::enteredDumbMode);
        }
        return isStateChanged;
    }

    // We cannot make this function `suspend`, because we have a contract that if dumb task is queued from EDT, dumb service becomes dumb
    // immediately. DumbService.queue is blocking method at the moment.
    private void incrementDumbCounterBlocking(Throwable trace) {
        if (tryIncrementStateCounter()) {
            myDumbModeStartTrace = trace;
            // If already dumb - just increment the counter. We don't need a write action (to not interrupt NBRA), neither we need EDT.
            // Otherwise, increment the counter under write action because this will change dumb state
            boolean enteredDumb = myApplication.runWriteAction((Supplier<Boolean>) this::doIncrementStateCounter);
            // here we are forcing the execution of listeners in a separate EDT event
            // Assume the listeners run in a single EDT event:
            // ```
            // (bgt)
            //(1) bgWa { exitDumbMode() } -> (2) invokeLater { (3) DumbModeListener.enteredDumbMode() }
            //
            // edt
            //(4) edtWa { enterDumbMode() } -> (5) DumbModeListener.enteredDumbMode()
            // ```
            // If 4 and 5 are executed synchronously, there can be order 1-2-4-5-3, and `runEnteredListeners` will be invoked
            // before `runExitedListeners`.
            // This would lead to repeated calls to `runEnteredListeners`, which is not permitted by the contract of these listeners.
            // The forced `invokeLater` will ensure that published requests for exit will be executed before new requests for enter.
            // This works given that `invokeLater` is fair, which is true.
            myApplication.invokeLater(() -> proceedWithPublishingOfIncrementEvents(enteredDumb));
        }

        LOG.assertTrue(state().isDumb(), "Should be dumb");
    }

    /**
     * Executes update of dumb counter with the help of background write action
     */
    private CoroutineStep<Void, Void> incrementDumbCounterSuspending(Throwable trace) {
        return WriteLock.apply(input -> {
            boolean enterDumbMode = tryIncrementStateCounter();
            if (enterDumbMode) {
                myDumbModeStartTrace = trace;
                // If already dumb - just increment the counter. We don't need a write action (to not interrupt NBRA), neither we need EDT.
                // Otherwise, increment the counter under write action because this will change dumb state
                boolean enteredDumb = doIncrementStateCounter();
                if (enteredDumb) {
                    myApplication.invokeLater(() -> proceedWithPublishingOfIncrementEvents(true));
                }
            }
            LOG.assertTrue(state().isDumb(), "Should be dumb");
            return null;
        });
    }

    private void proceedWithPublishingOfIncrementEvents(boolean enteredDumb) {
        if (enteredDumb) {
            LOG.info("enter dumb mode [" + myProject.getName() + "]");
            if (LOG.isDebugEnabled()) {
                LOG.debug("dumb mode [" + myProject.getName() + "] trace", myDumbModeStartTrace);
            }
            try {
                publishDumbModeChangedEvent(DumbModeEventListenerState.ENTERED);
            }
            catch (Throwable t) {
                // in unit tests we may get here because of exception thrown from Log.error from catch block inside runCatchingIgnorePCE
                decrementDumbCounterBlocking();
                throw t;
            }
        }
    }

    private boolean tryDecrementDumbCounter() {
        return ((DumbStateImpl) myState.getAndUpdate(it -> ((DumbStateImpl) it).tryDecrementDumbCounter())).decrementWillChangeDumbState();
    }

    private boolean doDecrementDumbCounter() {
        DumbStateImpl updated = (DumbStateImpl) myState.updateAndGet(it -> ((DumbStateImpl) it).decrementDumbCounter());
        boolean isStateChanged = updated.isSmart();
        if (isStateChanged) {
            boolean balanced =
                myDumbModeListenerBackgroundableState.compareAndSet(DumbModeEventListenerState.ENTERED, DumbModeEventListenerState.EXITED);
            if (!balanced) {
                LOG.error("Unexpected listener state: dumb mode is going to be exited without entering");
            }
            runCatchingIgnorePCE(myPublisherBackgroundable::exitDumbMode);
        }
        return isStateChanged;
    }

    // this method is not `suspend` for the sake of symmetry: incrementDumbCounter is not `suspend` as of now
    private void decrementDumbCounterBlocking() {
        // If there are other dumb tasks - just decrement the counter. We don't need a write action (to not interrupt NBRA),
        // neither we need EDT.
        // Otherwise, decrement the counter under write action because this will change dumb state
        if (tryDecrementDumbCounter()) {
            boolean exitDumb = myApplication.runWriteAction((Supplier<Boolean>) this::doDecrementDumbCounter);
            // for rationale for this `invokeLater`, see explanation in `incrementDumbCounterBlocking`
            myApplication.invokeLater(() -> proceedWithPublishingOfDecrementEvents(exitDumb));
        }
    }

    public void proceedWithPublishingOfDecrementEvents(boolean exitDumb) {
        if (exitDumb) {
            LOG.info("exit dumb mode [" + myProject.getName() + "]");
            myDumbModeStartTrace = null;
            publishDumbModeChangedEvent(DumbModeEventListenerState.EXITED);
        }
    }

    private CoroutineStep<Void, Void> decrementDumbCounterSuspending() {
        LOG.assertTrue(state().isDumb(), "Should be dumb");
        return WriteLock.apply(input -> {
            if (tryDecrementDumbCounter()) {
                boolean isNowSmart = doDecrementDumbCounter();
                if (isNowSmart) {
                    myApplication.invokeLater(() -> proceedWithPublishingOfDecrementEvents(true));
                }
            }
            return null;
        });
    }

    /**
     * Since {@link DumbModeListener} is invoked asynchronously from the changing the dumb status,
     * it is possible for someone to enter modal context and change dumb mode inside.
     * It would mean that {@link Application#invokeLater} with {@link DumbModeListener#exitDumbMode} would be delayed until the modal dialog
     * is closed,
     * so we would get repeated calls to {@link DumbModeListener#enteredDumbMode}
     * <p>
     * To avoid this situation, we deduplicate calls to {@link DumbModeListener} via a publicly available {@link #myDumbModeListenerState}
     * on the EDT.
     */
    private void publishDumbModeChangedEvent(DumbModeEventListenerState desiredListenerState) {
        myApplication.assertIsDispatchThread();

        switch (desiredListenerState) {
            case ENTERED -> {
                if (myDumbModeListenerState == DumbModeEventListenerState.EXITED) {
                    myDumbModeListenerState = DumbModeEventListenerState.ENTERED;
                    runCatchingIgnorePCE(myPublisher::enteredDumbMode);
                }
            }
            case EXITED -> {
                if (myDumbModeListenerState == DumbModeEventListenerState.ENTERED) {
                    myDumbModeListenerState = DumbModeEventListenerState.EXITED;
                    runCatchingIgnorePCE(myPublisher::exitDumbMode);
                }
            }
        }
    }

    @Override
    public boolean canRunSmart() {
        return myProject.getInstance(SmartModeScheduler.class).canRunSmart();
    }

    @Override
    public void runWhenSmart(Runnable runnable) {
        myProject.getInstance(SmartModeScheduler.class).runWhenSmart(runnable);
    }

    @Override
    public void unsafeRunWhenSmart(Runnable runnable) {
        // we probably don't need unsafeRunWhenSmart anymore
        runWhenSmart(runnable);
    }

    @Override
    public void queueTask(DumbModeTask task) {
        if (myIsDisposed) {
            LOG.debug("DumbServiceImpl is disposed, throwing a ProcessCanceledException when trying to queue " + task);
            throw new ProcessCanceledException(new IllegalStateException("Cannot queue task " + task + " after disposal"));
        }

        LOG.debug("Scheduling task " + task);
        if (myProject.isDefault()) {
            LOG.error("No indexing tasks should be created for default project: " + task);
        }
        Throwable trace = new Throwable();
        ModalityState modality = myApplication.getDefaultModalityState();
        if (modality.equals(ModalityState.any())) {
            LOG.error("Unexpected modality: should not be ANY. Replace with NON_MODAL");
            modality = ModalityState.nonModal();
        }
        if (myApplication.isDispatchThread()) {
            queueTaskOnEdt(task, modality, trace);
        }
        else if (Registry.is("ide.dumb.service.use.background.write.action", true) && modality.equals(ModalityState.nonModal())) {
            queueTaskOnBackground(task, trace);
        }
        else {
            ModalityState edtModality = modality;
            invokeLaterOnEdtInScheduledTasksScope(
                edtModality,
                () -> queueTaskOnEdt(task, edtModality, trace),
                () -> Disposer.dispose(task)
            );
        }
    }

    private void invokeLaterOnEdtInScheduledTasksScope(ModalityState modality, Runnable block, Runnable onCancelled) {
        ScheduledTasksScope scope = myScheduledTasksScope;
        AtomicBoolean done = new AtomicBoolean(false);
        Runnable unregister = scope.register(() -> {
            if (done.compareAndSet(false, true)) {
                onCancelled.run();
            }
        });
        myApplication.invokeLater(() -> {
            if (done.compareAndSet(false, true)) {
                unregister.run();
                block.run();
            }
        }, modality);
    }

    private void queueTaskOnEdt(DumbModeTask task, ModalityState modality, Throwable trace) {
        // First, increment dumb mode, then add the task.
        // If increment failed, task execution will not be scheduled, and we will be stuck in dumb mode.
        // In unit tests, much safer behavior is to ignore the task.
        // In prod, both behaviors are bad.
        myApplication.assertIsDispatchThread();
        incrementDumbCounterBlocking(trace);

        myTaskQueue.addTask(task);

        // we want to invoke LATER. I.e. right now one can invoke completeJustSubmittedTasks and
        // drain the queue synchronously under modal progress
        DumbTaskLauncher launcher = new DumbTaskLauncher(modality);
        myDumbTaskLaunchers.add(launcher);
        invokeLaterOnEdtInScheduledTasksScope(modality, launcher::launch, () -> myApplication.invokeLater(launcher::cancel, modality));
    }

    private void queueTaskOnBackground(DumbModeTask task, Throwable trace) {
        ScheduledTasksScope scope = myScheduledTasksScope;
        Continuation<Void> continuation = Coroutine.<Void, Void>first(incrementDumbCounterSuspending(trace))
            .then(CodeExecution.run(() -> {
                // First, increment dumb mode, then add the task.
                // If increment failed, task execution will not be scheduled, and we will be stuck in dumb mode.
                // In unit tests, much safer behavior is to ignore the task.
                // In prod, both behaviors are bad.
                myTaskQueue.addTask(task);

                DumbTaskLauncher launcher = new DumbTaskLauncher(ModalityState.nonModal());
                myDumbTaskLaunchers.add(launcher);
                launcher.launch();
            }))
            .runAsync(CoroutineScope.of(myProject.coroutineContext()), null);
        Runnable unregister = scope.register(continuation::cancel);
        continuation.onFinish(c -> unregister.run());
        continuation.onCancel(c -> {
            unregister.run();
            Disposer.dispose(task);
        });
    }

    @Override
    public void showDumbModeNotification(LocalizeValue message) {
        myProject.getUIAccess().giveIfNeed(() -> {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(myProject);
            if (ideFrame != null) {
                StatusBar statusBar = ideFrame.getStatusBar();
                if (statusBar != null) {
                    statusBar.notifyProgressByBalloon(NotificationType.INFO, message.get());
                }
            }
        });
    }

    @Override
    public void cancelAllTasksAndWait() {
        if (!((ApplicationEx) myApplication).isWriteThread() || myApplication.isWriteAccessAllowed()) {
            throw new AssertionError("Must be called on write thread without write action");
        }

        LOG.info("Purge dumb task queue");
        Thread currentThread = Thread.currentThread();
        String initialThreadName = currentThread.getName();
        ConcurrencyUtil.runUnderThreadName(initialThreadName + " [DumbService.cancelAllTasksAndWait(state = " + state() + ")]", () -> {
            // isRunning will be false eventually, because we are on EDT, and no new task can be queued outside the EDT
            // (we only wait for a currently running task to terminate).
            myGuiDumbTaskRunner.cancelAllTasks();
            while (myGuiDumbTaskRunner.isRunning().get() && !myProject.isDisposed()) {
                PingProgress.interactWithEdtProgress();
                LockSupport.parkNanos(50_000_000);
            }

            // Invoked after myGuiDumbTaskRunner has stopped to make sure that all the tasks submitted from the executor callbacks
            // are canceled,
            // This also cancels all the tasks that are waiting for the EDT to queue new dumb tasks
            ScheduledTasksScope oldTaskScope = myScheduledTasksScope;
            myScheduledTasksScope = new ScheduledTasksScope();
            oldTaskScope.cancel();
        });
    }

    @Override
    public void waitForSmartMode() {
        doWaitForSmartMode(null);
    }

    @Override
    public boolean waitForSmartMode(long timeoutMillis) {
        return doWaitForSmartMode(timeoutMillis);
    }

    private boolean doWaitForSmartMode(@Nullable Long milliseconds) {
        if (ALWAYS_SMART) {
            return true;
        }
        if (((ApplicationEx) myApplication).holdsReadLock()) {
            throw new AssertionError("Don't invoke waitForSmartMode from inside read action in dumb mode");
        }
        if (myWaitIntolerantThread == Thread.currentThread()) {
            throw new AssertionError("Don't invoke waitForSmartMode from a background startup activity");
        }
        CountDownLatch switched = new CountDownLatch(1);
        SmartModeScheduler smartModeScheduler = myProject.getInstance(SmartModeScheduler.class);
        if (smartModeScheduler.getCurrentMode() == 0) {
            // optimization: let's return right away if already in smart mode
            return true;
        }
        smartModeScheduler.runWhenSmart(switched::countDown);

        // we check getCurrentMode here because of tests which may hang because runWhenSmart needs EDT for scheduling
        long startTime = System.currentTimeMillis();
        while (!myProject.isDisposed() && smartModeScheduler.getCurrentMode() != 0) {
            // it is fine to unblock the caller when myProject.isDisposed, even if didn't reach smart mode: we are on background thread
            // without read action. Dumb mode may start immediately after the caller is unblocked, so the caller is prepared for this
            // situation.
            try {
                if (switched.await(50, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
            catch (InterruptedException ignored) {
            }

            ProgressManager.checkCanceled();
            if (milliseconds != null && startTime + milliseconds < System.currentTimeMillis()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void smartInvokeLater(Runnable runnable) {
        smartInvokeLater(runnable, myApplication.getDefaultModalityState());
    }

    @Override
    public void smartInvokeLater(Runnable runnable, ModalityState modalityState) {
        myApplication.invokeLater(
            () -> {
                if (canRunSmart()) {
                    runnable.run();
                }
                else {
                    LOG.debug("smartInvokeLater dispatched");
                    runWhenSmart(() -> smartInvokeLater(runnable, modalityState));
                }
            },
            modalityState,
            myProject.getDisposed()
        );
    }

    @Override
    public void completeJustSubmittedTasks() {
        myApplication.assertIsDispatchThread();
        LOG.assertTrue(myProject.isInitialized(), "Project should have been initialized");

        // there is no race: myTaskQueue is only updated from EDT
        if (!myTaskQueue.isEmpty()) {
            incrementDumbCounterBlocking(new Throwable());
            try {
                while (!myTaskQueue.isEmpty()) {
                    boolean queueProcessedUnderModalProgress = processQueueUnderModalProgress();
                    if (!queueProcessedUnderModalProgress) {
                        if (myApplication.isUnitTestMode()) {
                            LOG.assertTrue(myTaskQueue.isEmpty(), "This behavior is valid, but most likely not expected in tests: " +
                                "completeJustSubmittedTasks does nothing because the queue is already " +
                                "being processed in the background thread.");
                        }
                        // processQueueUnderModalProgress did nothing (i.e. processing is being done under non-modal indicator)
                        break;
                    }
                }
            }
            finally {
                decrementDumbCounterBlocking();
            }
        }

        // there is no race: dumbTaskLaunchers is only updated from EDT
        // the myTaskQueue is empty, we expect that DumbTaskLauncher::launch will do nothing other than finishing dumb mode
        // we need a copy, because the task will remove itself from the list
        for (DumbTaskLauncher launcher : new ArrayList<>(myDumbTaskLaunchers)) {
            launcher.launch();
        }

        // it is still possible that the IDE is dumb at this point. This may happen if dumb queue is actually processed
        // in the background, and the background thread is processing the last task from the queue.
        // This will happen, for example, in unit tests in DUMB_EMPTY_INDEX indexing mode: background thread will be processing
        // the eternal task.
    }

    private boolean processQueueUnderModalProgress() {
        Throwable startTrace = new Throwable();
        NoAccessDuringPsiEventsService.getInstance().checkCallContext();
        return myGuiDumbTaskRunner.tryStartProcessInThisThread((AutoclosableProgressive processTask) -> {
            try {
                LOG.info("Processing dumb queue under modal progress (start)");
                LOG.debug("Processing dumb queue under modal progress (start)", startTrace);
                String title = ProjectLocalize.progressIndexingTitle().get();
                ((ApplicationEx) myApplication).executeSuspendingWriteAction(myProject, title, () -> {
                    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                    try (processTask) {
                        processTask.run(indicator);
                    }
                });
            }
            finally {
                LOG.info("Processing dumb queue under modal progress (end)");
                LOG.debug("Processing dumb queue under modal progress (end)", startTrace);
            }
        });
    }

    @Override
    public AccessToken runWithWaitForSmartModeDisabled() {
        myWaitIntolerantThread = Thread.currentThread();
        return new AccessToken() {
            @Override
            public void finish() {
                myWaitIntolerantThread = null;
            }
        };
    }

    @Override
    public long getModificationCount() {
        // todo: drop mod tracker in scanner executor after there is a proper way to track indexes updates IJPL-472
        return state().modificationCounter()
            + UnindexedFilesScannerExecutor.getInstance(myProject).getModificationTracker().getModificationCount();
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return this;
    }

    @Override
    public ObservableValue<DumbState> getState() {
        return myState;
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public boolean isAlternativeResolveEnabled() {
        return myAlternativeResolveTracker.isAlternativeResolveEnabled();
    }

    @Override
    public void setAlternativeResolveEnabled(boolean enabled) {
        myAlternativeResolveTracker.setAlternativeResolveEnabled(enabled);
    }

    @Override
    public @Nullable Throwable getDumbModeStartTrace() {
        return myDumbModeStartTrace;
    }

    private record DumbStateImpl(boolean dumb, long modificationCounter, int dumbCounter) implements DumbState {
        @Override
        public boolean isDumb() {
            return dumb;
        }

        private DumbStateImpl nextCounterState(int nextVal) {
            if (nextVal > 0) {
                return new DumbStateImpl(true, modificationCounter + 1, nextVal);
            }
            else {
                LOG.assertTrue(nextVal == 0, "Invalid nextVal=" + nextVal);
                return new DumbStateImpl(false, modificationCounter + 1, 0);
            }
        }

        boolean incrementWillChangeDumbState() {
            return isSmart();
        }

        boolean decrementWillChangeDumbState() {
            return dumbCounter == 1;
        }

        DumbStateImpl incrementDumbCounter() {
            return nextCounterState(dumbCounter + 1);
        }

        DumbStateImpl decrementDumbCounter() {
            return nextCounterState(dumbCounter - 1);
        }

        DumbStateImpl tryIncrementDumbCounter() {
            return incrementWillChangeDumbState() ? this : incrementDumbCounter();
        }

        DumbStateImpl tryDecrementDumbCounter() {
            return decrementWillChangeDumbState() ? this : decrementDumbCounter();
        }

        boolean isSmart() {
            return !isDumb();
        }
    }

    @TestOnly
    public void ensureInitialDumbTaskRequiredForSmartModeSubmitted() {
        if (!myInitialDumbTaskRequiredForSmartModeSubmitted.get()) {
            queueStartupActivitiesRequiredForSmartMode();
        }
    }

    @TestOnly
    public boolean isRunning() {
        return myGuiDumbTaskRunner.isRunning().get();
    }

    @TestOnly
    public boolean hasScheduledTasks() {
        // when queued on EDT, dumb mode starts immediately, but executor does not start immediately - it schedules start to the end
        // of the EDT
        // queue to give a chance to invoke completeJustSubmittedTasks and index files under modal progress.
        return myScheduledTasksScope.hasChildren() || myGuiDumbTaskRunner.hasScheduledTasks();
    }

    /**
     * @deprecated Implementation details should not be accessed in production code,
     * use {@link consulo.project.DumbService#getInstance(Project)}
     */
    @Deprecated
    public static DumbServiceImpl getInstance(Project project) {
        return (DumbServiceImpl) consulo.project.DumbService.getInstance(project);
    }

    private static void runCatchingIgnorePCE(@InheritCallerContext Runnable runnable) {
        try {
            runnable.run();
        }
        catch (ProcessCanceledException ignored) {
        }
        catch (Throwable t) {
            LOG.error(t);
        }
    }

    public static boolean isSynchronousTaskExecution() {
        Application application = Application.get();
        return (application.isUnitTestMode() || isSynchronousHeadlessApplication(application))
            && !Boolean.parseBoolean(System.getProperty(IDEA_FORCE_DUMB_QUEUE_TASKS, "false"));
    }

    private static boolean isSynchronousHeadlessApplication(Application application) {
        return application.isHeadlessEnvironment() && !Boolean.getBoolean("ide.async.headless.mode");
    }
}
