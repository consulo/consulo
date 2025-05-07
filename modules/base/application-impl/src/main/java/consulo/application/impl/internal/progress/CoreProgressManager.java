// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.progress;

import consulo.application.Application;
import consulo.application.impl.internal.BaseApplication;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.JobScheduler;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.internal.ProgressManagerEx;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.*;
import consulo.application.util.ApplicationUtil;
import consulo.application.util.ClientId;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.collection.primitive.longs.ConcurrentLongObjectMap;
import consulo.util.collection.primitive.longs.LongMaps;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

public class CoreProgressManager extends ProgressManager implements ProgressManagerEx, Disposable {
    private static final Logger LOG = Logger.getInstance(CoreProgressManager.class);

    static final int CHECK_CANCELED_DELAY_MILLIS = 10;
    private final AtomicInteger myUnsafeProgressCount = new AtomicInteger(0);

    public static final boolean ENABLED = !"disabled".equals(System.getProperty("idea.ProcessCanceledException"));
    private static CheckCanceledHook ourCheckCanceledHook;
    private ScheduledFuture<?> myCheckCancelledFuture; // guarded by threadsUnderIndicator

    // indicator -> threads which are running under this indicator.
    // THashMap is avoided here because of tombstones overhead
    private static final Map<ProgressIndicator, Set<Thread>> threadsUnderIndicator = new HashMap<>(); // guarded by threadsUnderIndicator
    // the active indicator for the thread id
    private static final ConcurrentLongObjectMap<ProgressIndicator> currentIndicators = LongMaps.newConcurrentLongObjectHashMap();
    // top-level indicators for the thread id
    private static final ConcurrentLongObjectMap<ProgressIndicator> threadTopLevelIndicators = LongMaps.newConcurrentLongObjectHashMap();
    // threads which are running under canceled indicator
    // THashSet is avoided here because of possible tombstones overhead
    protected static final Set<Thread> threadsUnderCanceledIndicator = new HashSet<>(); // guarded by threadsUnderIndicator

    @Nonnull
    private static volatile CheckCanceledBehavior ourCheckCanceledBehavior = CheckCanceledBehavior.NONE;

    private enum CheckCanceledBehavior {
        NONE,
        ONLY_HOOKS,
        INDICATOR_PLUS_HOOKS
    }

    /**
     * active (i.e. which have {@link #executeProcessUnderProgress(Runnable, ProgressIndicator)} method running) indicators
     * which are not inherited from {@link StandardProgressIndicator}.
     * for them an extra processing thread (see {@link #myCheckCancelledFuture}) has to be run
     * to call their non-standard {@link ProgressIndicator#checkCanceled()} method periodically.
     */
    // multiset here (instead of a set) is for simplifying add/remove indicators on process-with-progress start/end with possibly identical indicators.
    private static final Collection<ProgressIndicator> nonStandardIndicators = ConcurrentHashMap.newKeySet();

    /**
     * true if running in non-cancelable section started with
     * {@link #executeNonCancelableSection(Runnable)} in this thread
     */
    private static final ThreadLocal<Boolean> isInNonCancelableSection = new ThreadLocal<>();
    // do not supply initial value to conserve memory

    protected final ApplicationEx myApplication;

    public CoreProgressManager(Application application) {
        myApplication = (ApplicationEx) application;
    }

    // must be under threadsUnderIndicator lock
    private void startBackgroundNonStandardIndicatorsPing() {
        if (myCheckCancelledFuture == null) {
            myCheckCancelledFuture = JobScheduler.getScheduler().scheduleWithFixedDelay(() -> {
                for (ProgressIndicator indicator : nonStandardIndicators) {
                    try {
                        indicator.checkCanceled();
                    }
                    catch (ProcessCanceledException e) {
                        indicatorCanceled(indicator);
                    }
                }
            }, 0, CHECK_CANCELED_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    // must be under threadsUnderIndicator lock
    private void stopBackgroundNonStandardIndicatorsPing() {
        if (myCheckCancelledFuture != null) {
            myCheckCancelledFuture.cancel(true);
            myCheckCancelledFuture = null;
        }
    }

    @Override
    public void dispose() {
        synchronized (threadsUnderIndicator) {
            stopBackgroundNonStandardIndicatorsPing();
        }
    }

    static boolean isThreadUnderIndicator(@Nonnull ProgressIndicator indicator, @Nonnull Thread thread) {
        synchronized (threadsUnderIndicator) {
            Set<Thread> threads = threadsUnderIndicator.get(indicator);
            return threads != null && threads.contains(thread);
        }
    }

    public List<ProgressIndicator> getCurrentIndicators() {
        synchronized (threadsUnderIndicator) {
            return new ArrayList<>(threadsUnderIndicator.keySet());
        }
    }

    public static boolean runCheckCanceledHooks(@Nullable ProgressIndicator indicator) {
        CheckCanceledHook hook = ourCheckCanceledHook;
        return hook != null && hook.runHook(indicator);
    }

    @Override
    protected void doCheckCanceled() throws ProcessCanceledException {
        CheckCanceledBehavior behavior = ourCheckCanceledBehavior;
        if (behavior == CheckCanceledBehavior.NONE) {
            return;
        }

        final ProgressIndicator progress = getProgressIndicator();
        if (progress != null && behavior == CheckCanceledBehavior.INDICATOR_PLUS_HOOKS) {
            progress.checkCanceled();
        }
        else {
            runCheckCanceledHooks(progress);
        }
    }

    @Override
    public ProgressIndicator newBackgroundableProcessIndicator(Task.Backgroundable backgroundable) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public ProgressIndicator newBackgroundableProcessIndicator(@Nullable ComponentManager project,
                                                               @Nonnull TaskInfo info,
                                                               @Nonnull PerformInBackgroundOption option) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProgressIndicator() {
        return getProgressIndicator() != null;
    }

    @Override
    public boolean hasUnsafeProgressIndicator() {
        return myUnsafeProgressCount.get() > 0;
    }

    @Override
    public boolean hasModalProgressIndicator() {
        synchronized (threadsUnderIndicator) {
            return ContainerUtil.or(threadsUnderIndicator.keySet(), ProgressIndicator::isModal);
        }
    }

    @Override
    public void runProcess(@Nonnull final Runnable process, @Nullable ProgressIndicator progress) {
        executeProcessUnderProgress(() -> {
            try {
                try {
                    if (progress != null && !progress.isRunning()) {
                        progress.start();
                    }
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                process.run();
            }
            finally {
                if (progress != null && progress.isRunning()) {
                    progress.stop();
                    if (progress instanceof ProgressIndicatorEx) {
                        ((ProgressIndicatorEx) progress).processFinish();
                    }
                }
            }
        }, progress);
    }

    @Override
    public <T> T runProcess(@Nonnull final Supplier<T> process, ProgressIndicator progress) throws ProcessCanceledException {
        final SimpleReference<T> ref = new SimpleReference<>();
        runProcess(() -> ref.set(process.get()), progress);
        return ref.get();
    }

    @Override
    public void executeNonCancelableSection(@Nonnull Runnable runnable) {
        if (isInNonCancelableSection()) {
            runnable.run();
        }
        else {
            try {
                isInNonCancelableSection.set(Boolean.TRUE);
                executeProcessUnderProgress(runnable, NonCancelableIndicator.INSTANCE);
            }
            finally {
                isInNonCancelableSection.remove();
            }
        }
    }

    @Override
    public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process,
                                                       @Nonnull @Nls String progressTitle,
                                                       boolean canBeCanceled,
                                                       @Nullable ComponentManager project) {
        return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
    }

    @Override
    public <T, E extends Exception> T runProcessWithProgressSynchronously(@Nonnull final ThrowableComputable<T, E> process,
                                                                          @Nonnull @Nls String progressTitle,
                                                                          boolean canBeCanceled,
                                                                          @Nullable ComponentManager project) throws E {
        final AtomicReference<T> result = new AtomicReference<>();
        final AtomicReference<Throwable> exception = new AtomicReference<>();

        runProcessWithProgressSynchronously(new Task.Modal(project, progressTitle, canBeCanceled) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                try {
                    T compute = process.compute();
                    result.set(compute);
                }
                catch (Throwable t) {
                    exception.set(t);
                }
            }
        });

        Throwable t = exception.get();
        if (t != null) {
            ExceptionUtil.rethrowUnchecked(t);
            @SuppressWarnings("unchecked") E e = (E) t;
            throw e;
        }

        return result.get();
    }

    @Override
    public boolean runProcessWithProgressSynchronously(@Nonnull final Runnable process,
                                                       @Nonnull @Nls String progressTitle,
                                                       boolean canBeCanceled,
                                                       @Nullable ComponentManager project,
                                                       @Nullable JComponent parentComponent) {
        Task.Modal task = new Task.Modal(project, progressTitle, parentComponent, canBeCanceled) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                process.run();
            }
        };
        return runProcessWithProgressSynchronously(task);
    }

    @Override
    public void runProcessWithProgressAsynchronously(@Nonnull ComponentManager project,
                                                     @Nonnull @Nls String progressTitle,
                                                     @Nonnull Runnable process,
                                                     @Nullable Runnable successRunnable,
                                                     @Nullable Runnable canceledRunnable) {
        runProcessWithProgressAsynchronously(project,
            progressTitle,
            process,
            successRunnable,
            canceledRunnable,
            PerformInBackgroundOption.DEAF);
    }

    @Override
    public void runProcessWithProgressAsynchronously(@Nonnull ComponentManager project,
                                                     @Nonnull @Nls String progressTitle,
                                                     @Nonnull final Runnable process,
                                                     @Nullable final Runnable successRunnable,
                                                     @Nullable final Runnable canceledRunnable,
                                                     @Nonnull PerformInBackgroundOption option) {
        runProcessWithProgressAsynchronously(new Task.Backgroundable(project, progressTitle, true, option) {
            @Override
            public void run(@Nonnull final ProgressIndicator indicator) {
                process.run();
            }


            @RequiredUIAccess
            @Override
            public void onCancel() {
                if (canceledRunnable != null) {
                    canceledRunnable.run();
                }
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                if (successRunnable != null) {
                    successRunnable.run();
                }
            }
        });
    }

    @Override
    public void run(@Nonnull final Task task) {
        if (task.isHeadless()) {
            if (myApplication.isDispatchThread()) {
                runProcessWithProgressSynchronously(task);
            }
            else {
                runProcessWithProgressInCurrentThread(task, new EmptyProgressIndicator(), myApplication.getDefaultModalityState());
            }
        }
        else if (task.isModal()) {
            runSynchronously(task.asModal());
        }
        else {
            final Task.Backgroundable backgroundable = task.asBackgroundable();
            if (backgroundable.isConditionalModal() && !backgroundable.shouldStartInBackground()) {
                runSynchronously(task);
            }
            else {
                runAsynchronously(backgroundable);
            }
        }
    }

    private void runSynchronously(@Nonnull final Task task) {
        if (myApplication.isDispatchThread()) {
            runProcessWithProgressSynchronously(task);
        }
        else {
            myApplication.invokeAndWait(() -> runProcessWithProgressSynchronously(task));
        }
    }

    private void runAsynchronously(@Nonnull final Task.Backgroundable task) {
        if (myApplication.isDispatchThread()) {
            runProcessWithProgressAsynchronously(task);
        }
        else {
            myApplication.invokeLater(() -> {
                ComponentManager project = task.getProject();
                if (project != null && project.isDisposed()) {
                    LOG.info("Task canceled because of project disposal: " + task);
                    finishTask(task, true, null);
                    return;
                }

                runProcessWithProgressAsynchronously(task);
            }, myApplication.getDefaultModalityState());
        }
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public <V> CompletableFuture<V> executeTask(@Nonnull UIAccess uiAccess,
                                                @Nullable ComponentManager project,
                                                @Nonnull LocalizeValue titleText,
                                                boolean modal,
                                                boolean cancelable,
                                                Function<ProgressIndicator, V> func) {
        ProgressBuilderTaskInfo info = new ProgressBuilderTaskInfo(titleText, cancelable);

        BaseApplication application = (BaseApplication) Application.get();

        SimpleReference<IndicatorDisposable> indicatorDisposable = SimpleReference.create();

        CompletableFuture<ProgressIndicator> indicatorFuture = CompletableFuture.supplyAsync(() -> {
            ProgressIndicator indicator;
            if (modal) {
                indicator = application.createProgressWindow(titleText.get(),
                    cancelable,
                    true,
                    project,
                    null,
                    ApplicationLocalize.taskButtonCancel());
            }
            else {
                indicator = newBackgroundableProcessIndicator(project, info, PerformInBackgroundOption.ALWAYS_BACKGROUND);
            }

            if (indicator instanceof Disposable) {
                IndicatorDisposable disposable = new IndicatorDisposable(indicator);

                indicatorDisposable.set(disposable);

                Disposer.register(myApplication, disposable);
            }
            return indicator;
        }, uiAccess);

        CompletableFuture<V> future = new NewProgressRunner<>(progress -> startTask(func, progress, info), modal, indicatorFuture)
            .submit(application);

        future.whenComplete((v, throwable) -> {
            IndicatorDisposable disposable = indicatorDisposable.get();
            if (disposable != null) {
                disposable.dispose();
            }
        });
        return future;
    }

    @Nonnull
    public Future<?> runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task) {
        ProgressIndicator indicator = newBackgroundableProcessIndicator(task);
        return runProcessWithProgressAsynchronously(task, indicator, null);
    }

    @Nonnull
    public Future<?> runProcessWithProgressAsynchronously(@Nonnull final Task.Backgroundable task,
                                                          @Nonnull final ProgressIndicator progressIndicator,
                                                          @Nullable final Runnable continuation) {
        return runProcessWithProgressAsynchronously(task, progressIndicator, continuation, progressIndicator.getModalityState());
    }

    private static class IndicatorDisposable implements Disposable {
        @Nonnull
        private final ProgressIndicator myIndicator;

        IndicatorDisposable(@Nonnull ProgressIndicator indicator) {
            myIndicator = indicator;
        }

        @Override
        public void dispose() {
            // do nothing if already disposed
            Disposer.dispose((Disposable) myIndicator, false);
        }
    }

    @Override
    @Nonnull
    public Future<?> runProcessWithProgressAsynchronously(@Nonnull final Task.Backgroundable task,
                                                          @Nonnull final ProgressIndicator progressIndicator,
                                                          @Nullable final Runnable continuation,
                                                          @Nonnull final ModalityState modalityState) {
        IndicatorDisposable indicatorDisposable;
        if (progressIndicator instanceof Disposable) {
            // use IndicatorDisposable instead of progressIndicator to
            // avoid re-registering progressIndicator if it was registered on some other parent before
            indicatorDisposable = new IndicatorDisposable(progressIndicator);
            Disposer.register(myApplication, indicatorDisposable);
        }
        else {
            indicatorDisposable = null;
        }
        return runProcessWithProgressAsync(task,
            CompletableFuture.completedFuture(progressIndicator),
            continuation,
            indicatorDisposable
        );
    }

    @Nonnull
    protected Future<?> runProcessWithProgressAsync(@Nonnull Task.Backgroundable task,
                                                    @Nonnull CompletableFuture<? extends ProgressIndicator> progressIndicator,
                                                    @Nullable Runnable continuation,
                                                    @Nullable IndicatorDisposable indicatorDisposable) {
        AtomicLong elapsed = new AtomicLong();
        return new ProgressRunner<>(progress -> {
            long start = System.currentTimeMillis();
            try {
                startTask(task, progress, continuation);
            }
            finally {
                elapsed.set(System.currentTimeMillis() - start);
            }
            return null;
        }).withProgress(progressIndicator)
            .submit()
            .whenComplete(ClientId.decorateBiConsumer((result, err) -> {
                if (!result.isCanceled()) {
                    notifyTaskFinished(task, elapsed.get());
                }

                ApplicationUtil.invokeLaterSomewhere(myApplication, () -> {
                    finishTask(task, result.isCanceled(), result.getThrowable() instanceof ProcessCanceledException ? null : result.getThrowable());
                    if (indicatorDisposable != null) {
                        Disposer.dispose(indicatorDisposable);
                    }
                });
            }));
    }

    public void notifyTaskFinished(@Nonnull Task.Backgroundable task, long elapsed) {

    }

    @Override
    public boolean runProcessWithProgressSynchronously(@Nonnull final Task task) {
        SimpleReference<Throwable> exceptionRef = new SimpleReference<>();
        Runnable taskContainer = () -> {
            try {
                startTask(task, getProgressIndicator(), null);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                exceptionRef.set(e);
            }
        };

        boolean result = myApplication.runProcessWithProgressSynchronously(taskContainer, task.getTitle(), task.isCancellable(), task.isModal(),
            task.getProject(), task.getParentComponent(), task.getCancelTextValue());

        ApplicationUtil.invokeAndWaitSomewhere(myApplication, () -> finishTask(task, !result, exceptionRef.get()));
        return result;
    }

    protected void startTask(@Nonnull Task task, @Nonnull ProgressIndicator indicator, @Nullable Runnable continuation) {
        try {
            task.run(indicator);
        }
        finally {
            try {
                if (indicator instanceof ProgressIndicatorEx) {
                    ((ProgressIndicatorEx) indicator).finish(task);
                }
            }
            finally {
                if (continuation != null) {
                    continuation.run();
                }
            }
        }
    }

    protected <T> T startTask(@Nonnull Function<ProgressIndicator, T> task,
                              @Nonnull ProgressIndicator indicator,
                              @Nonnull TaskInfo taskInfo) {
        try {
            return task.apply(indicator);
        }
        finally {
            if (indicator instanceof ProgressIndicatorEx progressIndicatorEx) {
                progressIndicatorEx.finish(taskInfo);
            }
        }
    }

    @Override
    public void runProcessWithProgressInCurrentThread(@Nonnull final Task task,
                                                      @Nonnull final ProgressIndicator progressIndicator,
                                                      @Nonnull final ModalityState modalityState) {
        if (progressIndicator instanceof Disposable) {
            Disposer.register(myApplication, (Disposable) progressIndicator);
        }

        boolean processCanceled = false;
        Throwable exception = null;
        try {
            runProcess(() -> startTask(task, progressIndicator, null), progressIndicator);
        }
        catch (ProcessCanceledException e) {
            processCanceled = true;
        }
        catch (Throwable e) {
            exception = e;
        }

        boolean finalCanceled = processCanceled || progressIndicator.isCanceled();
        Throwable finalException = exception;

        ApplicationUtil.invokeAndWaitSomewhere(myApplication, () -> finishTask(task, finalCanceled, finalException));
    }

    @RequiredUIAccess
    protected void finishTask(@Nonnull Task task, boolean canceled, @Nullable Throwable error) {
        try {
            if (error != null) {
                task.onThrowable(error);
            }
            else if (canceled) {
                task.onCancel();
            }
            else {
                task.onSuccess();
            }
        }
        finally {
            task.onFinished();
        }
    }

    @Override
    public void runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task, @Nonnull ProgressIndicator progressIndicator) {
        runProcessWithProgressAsynchronously(task, progressIndicator, null);
    }

    @Override
    public ProgressIndicator getProgressIndicator() {
        return getCurrentIndicator(Thread.currentThread());
    }

    @Override
    public void executeProcessUnderProgress(@Nonnull Runnable process, ProgressIndicator progress) throws ProcessCanceledException {
        if (progress == null) {
            myUnsafeProgressCount.incrementAndGet();
        }

        try {
            ProgressIndicator oldIndicator = null;
            boolean set = progress != null && progress != (oldIndicator = getProgressIndicator());
            if (set) {
                Thread currentThread = Thread.currentThread();
                long threadId = currentThread.threadId();
                setCurrentIndicator(threadId, progress);
                try {
                    registerIndicatorAndRun(progress, currentThread, oldIndicator, process);
                }
                finally {
                    setCurrentIndicator(threadId, oldIndicator);
                }
            }
            else {
                process.run();
            }
        }
        finally {
            if (progress == null) {
                myUnsafeProgressCount.decrementAndGet();
            }
        }
    }

    @Override
    public boolean runInReadActionWithWriteActionPriority(@Nonnull Runnable action, @Nullable ProgressIndicator indicator) {
        myApplication.runReadAction(action);
        return true;
    }

    private void registerIndicatorAndRun(@Nonnull ProgressIndicator indicator,
                                         @Nonnull Thread currentThread,
                                         ProgressIndicator oldIndicator,
                                         @Nonnull Runnable process) {
        List<Set<Thread>> threadsUnderThisIndicator = new ArrayList<>();
        synchronized (threadsUnderIndicator) {
            boolean oneOfTheIndicatorsIsCanceled = false;

            for (ProgressIndicator thisIndicator = indicator;
                 thisIndicator != null;
                 thisIndicator =
                     thisIndicator instanceof WrappedProgressIndicator ? ((WrappedProgressIndicator) thisIndicator).getOriginalProgressIndicator() : null) {
                Set<Thread> underIndicator = threadsUnderIndicator.computeIfAbsent(thisIndicator, __ -> new SmartHashSet<>());
                boolean alreadyUnder = !underIndicator.add(currentThread);
                threadsUnderThisIndicator.add(alreadyUnder ? null : underIndicator);

                boolean isStandard = thisIndicator instanceof StandardProgressIndicator;
                if (!isStandard) {
                    nonStandardIndicators.add(thisIndicator);
                    startBackgroundNonStandardIndicatorsPing();
                }

                oneOfTheIndicatorsIsCanceled |= thisIndicator.isCanceled();
            }

            if (oneOfTheIndicatorsIsCanceled) {
                threadsUnderCanceledIndicator.add(currentThread);
            }
            else {
                threadsUnderCanceledIndicator.remove(currentThread);
            }

            updateShouldCheckCanceled();
        }

        try {
            process.run();
        }
        finally {
            synchronized (threadsUnderIndicator) {
                ProgressIndicator thisIndicator = null;
                // order doesn't matter
                for (int i = 0; i < threadsUnderThisIndicator.size(); i++) {
                    thisIndicator = i == 0 ? indicator : ((WrappedProgressIndicator) thisIndicator).getOriginalProgressIndicator();
                    Set<Thread> underIndicator = threadsUnderThisIndicator.get(i);
                    boolean removed = underIndicator != null && underIndicator.remove(currentThread);
                    if (removed && underIndicator.isEmpty()) {
                        threadsUnderIndicator.remove(thisIndicator);
                    }
                    boolean isStandard = thisIndicator instanceof StandardProgressIndicator;
                    if (!isStandard) {
                        nonStandardIndicators.remove(thisIndicator);
                        if (nonStandardIndicators.isEmpty()) {
                            stopBackgroundNonStandardIndicatorsPing();
                        }
                    }
                    // by this time oldIndicator may have been canceled
                    if (oldIndicator != null && oldIndicator.isCanceled()) {
                        threadsUnderCanceledIndicator.add(currentThread);
                    }
                    else {
                        threadsUnderCanceledIndicator.remove(currentThread);
                    }
                }
                updateShouldCheckCanceled();
            }
        }
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public final void updateShouldCheckCanceled() {
        synchronized (threadsUnderIndicator) {
            CheckCanceledHook hook = createCheckCanceledHook();
            boolean hasCanceledIndicator = !threadsUnderCanceledIndicator.isEmpty();
            ourCheckCanceledHook = hook;
            ourCheckCanceledBehavior =
                hook == null && !hasCanceledIndicator ? CheckCanceledBehavior.NONE : hasCanceledIndicator && ENABLED ? CheckCanceledBehavior.INDICATOR_PLUS_HOOKS : CheckCanceledBehavior.ONLY_HOOKS;
        }
    }

    @Nullable
    protected CheckCanceledHook createCheckCanceledHook() {
        return null;
    }

    @Override
    protected void indicatorCanceled(@Nonnull ProgressIndicator indicator) {
        // mark threads running under this indicator as canceled
        synchronized (threadsUnderIndicator) {
            Set<Thread> threads = threadsUnderIndicator.get(indicator);
            if (threads != null) {
                for (Thread thread : threads) {
                    boolean underCancelledIndicator = false;
                    for (ProgressIndicator currentIndicator = getCurrentIndicator(thread);
                         currentIndicator != null;
                         currentIndicator =
                             currentIndicator instanceof WrappedProgressIndicator ? ((WrappedProgressIndicator) currentIndicator).getOriginalProgressIndicator() : null) {
                        if (currentIndicator == indicator) {
                            underCancelledIndicator = true;
                            break;
                        }
                    }

                    if (underCancelledIndicator) {
                        threadsUnderCanceledIndicator.add(thread);
                        updateShouldCheckCanceled();
                    }
                }
            }
        }
    }

    @TestOnly
    public static boolean isCanceledThread(@Nonnull Thread thread) {
        synchronized (threadsUnderIndicator) {
            return threadsUnderCanceledIndicator.contains(thread);
        }
    }

    @Override
    public boolean isInNonCancelableSection() {
        return isInNonCancelableSection.get() != null;
    }

    private static final long MAX_PRIORITIZATION_NANOS = TimeUnit.SECONDS.toNanos(12);
    private static final Thread[] NO_THREADS = new Thread[0];
    private final Set<Thread> myPrioritizedThreads = ContainerUtil.newConcurrentSet();
    private volatile Thread[] myEffectivePrioritizedThreads = NO_THREADS;
    private int myDeprioritizations = 0;
    private final Object myPrioritizationLock = ObjectUtil.sentinel("myPrioritizationLock");
    private volatile long myPrioritizingStarted = 0;

    @Override
    public <T, E extends Throwable> T computePrioritized(@Nonnull ThrowableComputable<T, E> computable) throws E {
        Thread thread = Thread.currentThread();
        boolean prioritize;
        synchronized (myPrioritizationLock) {
            if (isCurrentThreadPrioritized()) {
                prioritize = false;
            }
            else {
                prioritize = true;
                if (myPrioritizedThreads.isEmpty()) {
                    myPrioritizingStarted = System.nanoTime();
                }
                myPrioritizedThreads.add(thread);
                updateEffectivePrioritized();
            }
        }
        try {
            return computable.compute();
        }
        finally {
            if (prioritize) {
                synchronized (myPrioritizationLock) {
                    myPrioritizedThreads.remove(thread);
                    updateEffectivePrioritized();
                }
            }
        }
    }

    public boolean isCurrentThreadPrioritized() {
        return myPrioritizedThreads.contains(Thread.currentThread());
    }

    private void updateEffectivePrioritized() {
        Thread[] prev = myEffectivePrioritizedThreads;
        Thread[] current = myDeprioritizations > 0 || myPrioritizedThreads.isEmpty() ? NO_THREADS : myPrioritizedThreads.toArray(NO_THREADS);
        myEffectivePrioritizedThreads = current;
        if (prev.length == 0 && current.length > 0) {
            prioritizingStarted();
        }
        else if (prev.length > 0 && current.length == 0) {
            prioritizingFinished();
        }
    }

    protected void prioritizingStarted() {
    }

    protected void prioritizingFinished() {
    }

    public boolean isPrioritizedThread(@Nonnull Thread from) {
        return myPrioritizedThreads.contains(from);
    }

    public void suppressPrioritizing() {
        synchronized (myPrioritizationLock) {
            if (++myDeprioritizations == 100 + ForkJoinPool.getCommonPoolParallelism() * 2) {
                Attachment attachment = AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString());
                attachment.setIncluded(true);
                LOG.error("A suspiciously high nesting of suppressPrioritizing, forgot to call restorePrioritizing?", attachment);
            }
            updateEffectivePrioritized();
        }
    }

    public void restorePrioritizing() {
        synchronized (myPrioritizationLock) {
            if (--myDeprioritizations < 0) {
                myDeprioritizations = 0;
                LOG.error("Unmatched suppressPrioritizing/restorePrioritizing");
            }
            updateEffectivePrioritized();
        }
    }

    protected boolean sleepIfNeededToGivePriorityToAnotherThread() {
        if (!isCurrentThreadEffectivelyPrioritized() && checkLowPriorityReallyApplicable()) {
            LockSupport.parkNanos(1_000_000);
            avoidBlockingPrioritizingThread();
            return true;
        }
        return false;
    }

    private boolean isCurrentThreadEffectivelyPrioritized() {
        Thread current = Thread.currentThread();
        for (Thread prioritized : myEffectivePrioritizedThreads) {
            if (prioritized == current) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLowPriorityReallyApplicable() {
        long time = System.nanoTime() - myPrioritizingStarted;
        if (time < 5_000_000) {
            return false; // don't sleep when activities are very short (e.g. empty processing of mouseMoved events)
        }

        if (avoidBlockingPrioritizingThread()) {
            return false;
        }

        if (myApplication.isDispatchThread()) {
            return false; // EDT always has high priority
        }

        if (time > MAX_PRIORITIZATION_NANOS) {
            // Don't wait forever in case someone forgot to stop prioritizing before waiting for other threads to complete
            // wait just for 12 seconds; this will be noticeable (and we'll get 2 thread dumps) but not fatal
            stopAllPrioritization();
            return false;
        }
        return true;
    }

    private boolean avoidBlockingPrioritizingThread() {
        if (isAnyPrioritizedThreadBlocked()) {
            // the current thread could hold a lock that prioritized threads are waiting for
            suppressPrioritizing();
            checkLaterThreadsAreUnblocked();
            return true;
        }
        return false;
    }

    private void checkLaterThreadsAreUnblocked() {
        try {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                if (isAnyPrioritizedThreadBlocked()) {
                    checkLaterThreadsAreUnblocked();
                }
                else {
                    restorePrioritizing();
                }
            }, 5, TimeUnit.MILLISECONDS);
        }
        catch (RejectedExecutionException ignore) {
        }
    }

    private void stopAllPrioritization() {
        synchronized (myPrioritizationLock) {
            myPrioritizedThreads.clear();
            updateEffectivePrioritized();
        }
    }

    private boolean isAnyPrioritizedThreadBlocked() {
        for (Thread thread : myEffectivePrioritizedThreads) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING || state == Thread.State.BLOCKED) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public static ModalityState getCurrentThreadProgressModality() {
        ProgressIndicator indicator = threadTopLevelIndicators.get(Thread.currentThread().threadId());
        ModalityState modality = indicator == null ? null : indicator.getModalityState();
        return modality != null ? modality : ModalityState.nonModal();
    }

    private static void setCurrentIndicator(long threadId, ProgressIndicator indicator) {
        if (indicator == null) {
            currentIndicators.remove(threadId);
            threadTopLevelIndicators.remove(threadId);
        }
        else {
            currentIndicators.put(threadId, indicator);
            if (!threadTopLevelIndicators.containsKey(threadId)) {
                threadTopLevelIndicators.put(threadId, indicator);
            }
        }
    }

    private static ProgressIndicator getCurrentIndicator(@Nonnull Thread thread) {
        return currentIndicators.get(thread.threadId());
    }

    @FunctionalInterface
    public interface CheckCanceledHook {
        CheckCanceledHook[] EMPTY_ARRAY = new CheckCanceledHook[0];

        /**
         * @param indicator the indicator whose {@link ProgressIndicator#checkCanceled()} was called, or null if a non-progressive thread performed {@link ProgressManager#checkCanceled()}
         * @return true if the hook has done anything that might take some time.
         */
        boolean runHook(@Nullable ProgressIndicator indicator);
    }

    public static void assertUnderProgress(@Nonnull ProgressIndicator indicator) {
        synchronized (threadsUnderIndicator) {
            Set<Thread> threads = threadsUnderIndicator.get(indicator);
            if (threads == null || !threads.contains(Thread.currentThread())) {
                LOG.error("Must be executed under progress indicator: " + indicator + ". Please see e.g. ProgressManager.runProcess()");
            }
        }
    }

    @Override
    public WrappedProgressIndicator wrapProgressIndicator(@Nullable ProgressIndicator indicator) {
        return ProgressWrapper.wrap(indicator);
    }

    @Override
    public ProgressIndicator unwrapProgressIndicator(WrappedProgressIndicator indicator) {
        return ProgressWrapper.unwrap(indicator);
    }
}
