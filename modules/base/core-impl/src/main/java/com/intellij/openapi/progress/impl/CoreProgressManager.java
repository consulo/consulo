// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.progress.impl;

import com.google.common.collect.ConcurrentHashMultiset;
import com.intellij.codeWithMe.ClientId;
import com.intellij.concurrency.JobScheduler;
import com.intellij.diagnostic.ThreadDumper;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ObjectUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SmartHashSet;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.primitive.longs.ConcurrentLongObjectMap;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class CoreProgressManager extends ProgressManager implements Disposable {
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
  private static final ConcurrentLongObjectMap<ProgressIndicator> currentIndicators = ContainerUtil.createConcurrentLongObjectMap();
  // top-level indicators for the thread id
  private static final ConcurrentLongObjectMap<ProgressIndicator> threadTopLevelIndicators = ContainerUtil.createConcurrentLongObjectMap();
  // threads which are running under canceled indicator
  // THashSet is avoided here because of possible tombstones overhead
  static final Set<Thread> threadsUnderCanceledIndicator = new HashSet<>(); // guarded by threadsUnderIndicator

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
  private static final Collection<ProgressIndicator> nonStandardIndicators = ConcurrentHashMultiset.create();

  /**
   * true if running in non-cancelable section started with
   * {@link #executeNonCancelableSection(Runnable)} in this thread
   */
  private static final ThreadLocal<Boolean> isInNonCancelableSection = new ThreadLocal<>(); // do not supply initial value to conserve memory

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

  List<ProgressIndicator> getCurrentIndicators() {
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
    if (behavior == CheckCanceledBehavior.NONE) return;

    final ProgressIndicator progress = getProgressIndicator();
    if (progress != null && behavior == CheckCanceledBehavior.INDICATOR_PLUS_HOOKS) {
      progress.checkCanceled();
    }
    else {
      runCheckCanceledHooks(progress);
    }
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
      return ContainerUtil.or(threadsUnderIndicator.keySet(), i -> i.isModal());
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
            ((ProgressIndicatorEx)progress).processFinish();
          }
        }
      }
    }, progress);
  }

  @Override
  public <T> T runProcess(@Nonnull final Computable<T> process, ProgressIndicator progress) throws ProcessCanceledException {
    final Ref<T> ref = new Ref<>();
    runProcess(() -> ref.set(process.compute()), progress);
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
  public boolean runProcessWithProgressSynchronously(@Nonnull Runnable process, @Nonnull @Nls String progressTitle, boolean canBeCanceled, @Nullable Project project) {
    return runProcessWithProgressSynchronously(process, progressTitle, canBeCanceled, project, null);
  }

  @Override
  public <T, E extends Exception> T runProcessWithProgressSynchronously(@Nonnull final ThrowableComputable<T, E> process,
                                                                        @Nonnull @Nls String progressTitle,
                                                                        boolean canBeCanceled,
                                                                        @Nullable Project project) throws E {
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
      @SuppressWarnings("unchecked") E e = (E)t;
      throw e;
    }

    return result.get();
  }

  @Override
  public boolean runProcessWithProgressSynchronously(@Nonnull final Runnable process,
                                                     @Nonnull @Nls String progressTitle,
                                                     boolean canBeCanceled,
                                                     @Nullable Project project,
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
  public void runProcessWithProgressAsynchronously(@Nonnull Project project,
                                                   @Nonnull @Nls String progressTitle,
                                                   @Nonnull Runnable process,
                                                   @Nullable Runnable successRunnable,
                                                   @Nullable Runnable canceledRunnable) {
    runProcessWithProgressAsynchronously(project, progressTitle, process, successRunnable, canceledRunnable, PerformInBackgroundOption.DEAF);
  }

  @Override
  public void runProcessWithProgressAsynchronously(@Nonnull Project project,
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


      @Override
      public void onCancel() {
        if (canceledRunnable != null) {
          canceledRunnable.run();
        }
      }

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
      if (ApplicationManager.getApplication().isDispatchThread()) {
        runProcessWithProgressSynchronously(task);
      }
      else {
        runProcessWithProgressInCurrentThread(task, new EmptyProgressIndicator(), ModalityState.defaultModalityState());
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
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runProcessWithProgressSynchronously(task);
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(() -> runProcessWithProgressSynchronously(task));
    }
  }

  private void runAsynchronously(@Nonnull final Task.Backgroundable task) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runProcessWithProgressAsynchronously(task);
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> {
        Project project = task.getProject();
        if (project != null && project.isDisposed()) {
          LOG.info("Task canceled because of project disposal: " + task);
          finishTask(task, true, null);
          return;
        }

        runProcessWithProgressAsynchronously(task);
      }, ModalityState.defaultModalityState());
    }
  }

  @Nonnull
  public Future<?> runProcessWithProgressAsynchronously(@Nonnull Task.Backgroundable task) {
    return runProcessWithProgressAsynchronously(task, new EmptyProgressIndicator(), null);
  }

  @Nonnull
  public Future<?> runProcessWithProgressAsynchronously(@Nonnull final Task.Backgroundable task, @Nonnull final ProgressIndicator progressIndicator, @Nullable final Runnable continuation) {
    return runProcessWithProgressAsynchronously(task, progressIndicator, continuation, progressIndicator.getModalityState());
  }

  @Nonnull
  protected TaskRunnable createTaskRunnable(@Nonnull Task task, @Nonnull ProgressIndicator indicator, @Nullable Runnable continuation) {
    return new TaskRunnable(task, indicator, continuation);
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
      Disposer.dispose((Disposable)myIndicator, false);
    }
  }

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
      Disposer.register(ApplicationManager.getApplication(), indicatorDisposable);
    }
    else {
      indicatorDisposable = null;
    }
    return runProcessWithProgressAsync(task, CompletableFuture.completedFuture(progressIndicator), continuation, indicatorDisposable, modalityState);
  }

  @Nonnull
  protected Future<?> runProcessWithProgressAsync(@Nonnull Task.Backgroundable task,
                                                  @Nonnull CompletableFuture<? extends ProgressIndicator> progressIndicator,
                                                  @Nullable Runnable continuation,
                                                  @Nullable IndicatorDisposable indicatorDisposable,
                                                  @Nullable ModalityState modalityState) {
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
    }).onThread(ProgressRunner.ThreadToUse.POOLED).withProgress(progressIndicator).submit().whenComplete(ClientId.decorateBiConsumer((result, err) -> {
      if (!result.isCanceled()) {
        notifyTaskFinished(task, elapsed.get());
      }

      ModalityState modality;
      if (modalityState != null) {
        modality = modalityState;
      }
      else {
        try {
          modality = progressIndicator.get().getModalityState();
        }
        catch (Throwable e) {
          modality = ModalityState.NON_MODAL;
        }
      }

      ApplicationUtil.invokeLaterSomewhere(task.whereToRunCallbacks(), modality, () -> {
        finishTask(task, result.isCanceled(), result.getThrowable() instanceof ProcessCanceledException ? null : result.getThrowable());
        if (indicatorDisposable != null) {
          Disposer.dispose(indicatorDisposable);
        }
      });
    }));
  }

  void notifyTaskFinished(@Nonnull Task.Backgroundable task, long elapsed) {

  }

  public boolean runProcessWithProgressSynchronously(@Nonnull final Task task) {
    Ref<Throwable> exceptionRef = new Ref<>();
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

    ApplicationEx application = (ApplicationEx)Application.get();
    boolean result = application.runProcessWithProgressSynchronously(taskContainer, task.getTitle(), task.isCancellable(), task.isModal(), task.getProject(), task.getParentComponent(), task.getCancelText());

    ApplicationUtil.invokeAndWaitSomewhere(task.whereToRunCallbacks(), application.getDefaultModalityState(), () -> finishTask(task, !result, exceptionRef.get()));
    return result;
  }

  @Deprecated
  protected void startTask(@Nonnull Task task, @Nonnull ProgressIndicator indicator, @Nullable Runnable continuation) {
    try {
      task.run(indicator);
    }
    finally {
      try {
        if (indicator instanceof ProgressIndicatorEx) {
          ((ProgressIndicatorEx)indicator).finish(task);
        }
      }
      finally {
        if (continuation != null) {
          continuation.run();
        }
      }
    }
  }

  public void runProcessWithProgressInCurrentThread(@Nonnull final Task task, @Nonnull final ProgressIndicator progressIndicator, @Nonnull final ModalityState modalityState) {
    if (progressIndicator instanceof Disposable) {
      Disposer.register(ApplicationManager.getApplication(), (Disposable)progressIndicator);
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

    ApplicationUtil.invokeAndWaitSomewhere(task.whereToRunCallbacks(), modalityState, () -> finishTask(task, finalCanceled, finalException));
  }

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
    if (progress == null) myUnsafeProgressCount.incrementAndGet();

    try {
      ProgressIndicator oldIndicator = null;
      boolean set = progress != null && progress != (oldIndicator = getProgressIndicator());
      if (set) {
        Thread currentThread = Thread.currentThread();
        long threadId = currentThread.getId();
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
      if (progress == null) myUnsafeProgressCount.decrementAndGet();
    }
  }

  @Override
  public boolean runInReadActionWithWriteActionPriority(@Nonnull Runnable action, @Nullable ProgressIndicator indicator) {
    ApplicationManager.getApplication().runReadAction(action);
    return true;
  }

  private void registerIndicatorAndRun(@Nonnull ProgressIndicator indicator, @Nonnull Thread currentThread, ProgressIndicator oldIndicator, @Nonnull Runnable process) {
    List<Set<Thread>> threadsUnderThisIndicator = new ArrayList<>();
    synchronized (threadsUnderIndicator) {
      boolean oneOfTheIndicatorsIsCanceled = false;

      for (ProgressIndicator thisIndicator = indicator;
           thisIndicator != null;
           thisIndicator = thisIndicator instanceof WrappedProgressIndicator ? ((WrappedProgressIndicator)thisIndicator).getOriginalProgressIndicator() : null) {
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
          thisIndicator = i == 0 ? indicator : ((WrappedProgressIndicator)thisIndicator).getOriginalProgressIndicator();
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
  final void updateShouldCheckCanceled() {
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
               currentIndicator = currentIndicator instanceof WrappedProgressIndicator ? ((WrappedProgressIndicator)currentIndicator).getOriginalProgressIndicator() : null) {
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
        Attachment attachment = new Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString());
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

    if (ApplicationManager.getApplication().isDispatchThread()) {
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
    ProgressIndicator indicator = threadTopLevelIndicators.get(Thread.currentThread().getId());
    ModalityState modality = indicator == null ? null : indicator.getModalityState();
    return modality != null ? modality : ModalityState.NON_MODAL;
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
    return currentIndicators.get(thread.getId());
  }

  protected abstract static class TaskContainer implements Runnable {
    private final Task myTask;

    protected TaskContainer(@Nonnull Task task) {
      myTask = task;
    }

    @Nonnull
    public Task getTask() {
      return myTask;
    }

    @Override
    public String toString() {
      return myTask.toString();
    }
  }

  protected static class TaskRunnable extends TaskContainer {
    private final ProgressIndicator myIndicator;
    private final Runnable myContinuation;

    TaskRunnable(@Nonnull Task task, @Nonnull ProgressIndicator indicator, @Nullable Runnable continuation) {
      super(task);
      myIndicator = indicator;
      myContinuation = continuation;
    }

    @Override
    public void run() {
      try {
        getTask().run(myIndicator);
      }
      finally {
        try {
          if (myIndicator instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx)myIndicator).finish(getTask());
          }
        }
        finally {
          if (myContinuation != null) {
            myContinuation.run();
          }
        }
      }
    }
  }

  @FunctionalInterface
  interface CheckCanceledHook {
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
}
