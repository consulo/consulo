// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.application.impl;

import com.google.common.annotations.VisibleForTesting;
import consulo.application.Application;
import consulo.application.NonBlockingReadAction;
import consulo.application.constraint.BaseConstrainedExecution;
import consulo.application.constraint.ConstrainedExecution;
import consulo.application.impl.internal.RunnableAsCallable;
import consulo.application.impl.internal.progress.ProgressIndicatorUtils;
import consulo.application.impl.internal.progress.SensitiveProgressWrapper;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.ClientId;
import consulo.application.util.Semaphore;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.codeEditor.Editor;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

@VisibleForTesting
public final class NonBlockingReadActionImpl<T> implements NonBlockingReadAction<T> {
  private static final Logger LOG = Logger.getInstance(NonBlockingReadActionImpl.class);
  private static final Executor SYNC_DUMMY_EXECUTOR = __ -> {
    throw new UnsupportedOperationException();
  };

  private final Application myApplication;
  // myModalityState and myUiThreadAction must be both null or both not-null
  private final ModalityState myModalityState;
  private final Consumer<? super T> myUiThreadAction;
  private final ConstrainedExecution.ContextConstraint[] myConstraints;
  private final BooleanSupplier[] myCancellationConditions;
  private final Set<? extends Disposable> myDisposables;
  private final
  @Nullable
  List<?> myCoalesceEquality;
  private final
  @Nullable
  ProgressIndicator myProgressIndicator;
  private final Callable<? extends T> myComputation;

  private static final Set<Submission<?>> ourTasks = ContainerUtil.newConcurrentSet();
  private static final Map<List<?>, Submission<?>> ourTasksByEquality = new HashMap<>();
  private static final SubmissionTracker ourUnboundedSubmissionTracker = new SubmissionTracker();

  NonBlockingReadActionImpl(@Nonnull Application application, @Nonnull Callable<? extends T> computation) {
    this(application,
         computation,
         (ModalityState)null,
         null,
         new ConstrainedExecution.ContextConstraint[0],
         new BooleanSupplier[0],
         Collections.emptySet(),
         null,
         null);
  }

  private NonBlockingReadActionImpl(@Nonnull Application application,
                                    @Nonnull Callable<? extends T> computation,
                                    @Nonnull Function<Application, ModalityState> modalityGetter,
                                    @Nullable Consumer<? super T> uiThreadAction,
                                    ConstrainedExecution.ContextConstraint[] constraints,
                                    BooleanSupplier[] cancellationConditions,
                                    @Nonnull Set<? extends Disposable> disposables,
                                    @Nullable List<?> coalesceEquality,
                                    @Nullable ProgressIndicator progressIndicator) {
    this(application,
         computation,
         modalityGetter.apply(application),
         uiThreadAction,
         constraints,
         cancellationConditions,
         disposables,
         coalesceEquality,
         progressIndicator);
  }

  private NonBlockingReadActionImpl(@Nonnull Application application,
                                    @Nonnull Callable<? extends T> computation,
                                    @Nullable ModalityState modalityState,
                                    @Nullable Consumer<? super T> uiThreadAction,
                                    ConstrainedExecution.ContextConstraint[] constraints,
                                    BooleanSupplier[] cancellationConditions,
                                    @Nonnull Set<? extends Disposable> disposables,
                                    @Nullable List<?> coalesceEquality,
                                    @Nullable ProgressIndicator progressIndicator) {
    myApplication = application;
    myComputation = computation;
    myModalityState = modalityState;
    myUiThreadAction = uiThreadAction;
    myConstraints = constraints;
    myCancellationConditions = cancellationConditions;
    myDisposables = disposables;
    myCoalesceEquality = coalesceEquality;
    myProgressIndicator = progressIndicator;
    if ((myModalityState == null) != (uiThreadAction == null)) {
      throw new IllegalArgumentException("myModalityState and myUiThreadAction must be both null or both not-null but got: " + myModalityState + ", " + uiThreadAction);
    }
  }

  @Nonnull
  private NonBlockingReadActionImpl<T> withConstraint(@Nonnull ConstrainedExecution.ContextConstraint constraint) {
    return new NonBlockingReadActionImpl<>(myApplication,
                                           myComputation,
                                           myModalityState,
                                           myUiThreadAction,
                                           ArrayUtil.append(myConstraints, constraint),
                                           myCancellationConditions,
                                           myDisposables,
                                           myCoalesceEquality,
                                           myProgressIndicator);
  }

  private void invokeLater(@Nonnull Runnable runnable) {
    // TODO [VISTALL] wtf? uui thread?? for read?
    myApplication.invokeLater(runnable);
  }

  @Override
  @Nonnull
  public NonBlockingReadAction<T> inSmartMode(@Nonnull ComponentManager project) {
    return withConstraint(new InSmartMode((Project)project)).expireWith(project);
  }

  @Override
  @Nonnull
  public NonBlockingReadAction<T> withDocumentsCommitted(@Nonnull ComponentManager project) {
    return withConstraint(new WithDocumentsCommitted((Project)project, myApplication.getAnyModalityState())).expireWith(project);
  }

  @Override
  @Nonnull
  public NonBlockingReadAction<T> expireWhen(@Nonnull BooleanSupplier expireCondition) {
    return new NonBlockingReadActionImpl<>(myApplication,
                                           myComputation,
                                           myModalityState,
                                           myUiThreadAction,
                                           myConstraints,
                                           ArrayUtil.append(myCancellationConditions, expireCondition),
                                           myDisposables,
                                           myCoalesceEquality,
                                           myProgressIndicator);
  }

  @Nonnull
  @Override
  public NonBlockingReadAction<T> expireWith(@Nonnull Disposable parentDisposable) {
    Set<Disposable> disposables = new HashSet<>(myDisposables);
    disposables.add(parentDisposable);
    return new NonBlockingReadActionImpl<>(myApplication,
                                           myComputation,
                                           myModalityState,
                                           myUiThreadAction,
                                           myConstraints,
                                           myCancellationConditions,
                                           disposables,
                                           myCoalesceEquality,
                                           myProgressIndicator);
  }

  @Override
  @Nonnull
  public NonBlockingReadAction<T> wrapProgress(@Nonnull ProgressIndicator progressIndicator) {
    LOG.assertTrue(myProgressIndicator == null, "Unspecified behaviour. Outer progress indicator is already set for the action.");
    return new NonBlockingReadActionImpl<>(myApplication,
                                           myComputation,
                                           myModalityState,
                                           myUiThreadAction,
                                           myConstraints,
                                           myCancellationConditions,
                                           myDisposables,
                                           myCoalesceEquality,
                                           progressIndicator);
  }

  @Override
  public NonBlockingReadAction<T> finishOnUiThread(@Nonnull Function<Application, ModalityState> modalityGetter,
                                                   @Nonnull Consumer<? super T> uiThreadAction) {
    return new NonBlockingReadActionImpl<>(myApplication,
                                           myComputation,
                                           modalityGetter,
                                           uiThreadAction,
                                           myConstraints,
                                           myCancellationConditions,
                                           myDisposables,
                                           myCoalesceEquality,
                                           myProgressIndicator);
  }

  @Override
  public
  @Nonnull
  NonBlockingReadAction<T> coalesceBy(Object... equality) {
    if (myCoalesceEquality != null) throw new IllegalStateException("Setting equality twice is not allowed");
    if (equality.length == 0) throw new IllegalArgumentException("Equality should include at least one object");
    if (equality.length == 1 && isTooCommon(equality[0])) {
      throw new IllegalArgumentException("Equality should be unique: passing " + equality[0] + " is likely to interfere with unrelated computations from different places");
    }
    return new NonBlockingReadActionImpl<>(myApplication,
                                           myComputation,
                                           myModalityState,
                                           myUiThreadAction,
                                           myConstraints,
                                           myCancellationConditions,
                                           myDisposables,
                                           new ArrayList<>(Arrays.asList(equality)),
                                           myProgressIndicator);
  }

  private static boolean isTooCommon(Object o) {
    return o instanceof Project || o instanceof PsiElement || o instanceof Document || o instanceof VirtualFile || o instanceof Editor || o instanceof FileEditor || o instanceof Class ||
      // o instanceof KClass ||
      o instanceof String || o == null;
  }

  @Override
  public T executeSynchronously() throws ProcessCanceledException {
    if (myModalityState != null || myCoalesceEquality != null) {
      throw new IllegalStateException((myModalityState != null ? "finishOnUiThread" : "coalesceBy") + " is not supported with synchronous non-blocking read actions");
    }

    ProgressIndicator outerIndicator =
      myProgressIndicator != null ? myProgressIndicator : ProgressIndicatorProvider.getGlobalProgressIndicator();
    return new Submission<>(this, SYNC_DUMMY_EXECUTOR, outerIndicator).executeSynchronously();
  }

  @Override
  public
  @Nonnull
  CancellablePromise<T> submit(@Nonnull Executor backgroundThreadExecutor) {
    Submission<T> submission = new Submission<>(this, backgroundThreadExecutor, myProgressIndicator);
    if (myCoalesceEquality == null) {
      submission.transferToBgThread();
    }
    else {
      submission.submitOrScheduleCoalesced(myCoalesceEquality);
    }
    return submission;
  }

  private static final class Submission<T> extends AsyncPromise<T> {
    @Nonnull
    private final Executor backendExecutor;
    private
    @Nullable
    final String myStartTrace;
    private volatile ProgressIndicator currentIndicator;
    private final ModalityState creationModality;
    @Nullable
    private Submission<?> myReplacement;
    @Nullable
    private final ProgressIndicator myProgressIndicator;
    @Nonnull
    private final NonBlockingReadActionImpl<T> builder;

    // a sum composed of: 1 for non-done promise, 1 for each currently running thread
    // so 0 means that the process is marked completed or canceled, and it has no running not-yet-finished threads
    private int myUseCount;

    private final AtomicBoolean myCleaned = new AtomicBoolean();
    private final List<Disposable> myExpirationDisposables = new ArrayList<>();

    Submission(@Nonnull NonBlockingReadActionImpl<T> builder,
               @Nonnull Executor backgroundThreadExecutor,
               @Nullable ProgressIndicator outerIndicator) {
      backendExecutor = backgroundThreadExecutor;
      this.builder = builder;
      creationModality = builder.myApplication.getDefaultModalityState();
      if (builder.myCoalesceEquality != null) {
        acquire();
      }
      myProgressIndicator = outerIndicator;

      if (LOG.isTraceEnabled()) {
        LOG.trace("Creating " + this);
      }

      myStartTrace = hasUnboundedExecutor() ? ourUnboundedSubmissionTracker.preventTooManySubmissions() : null;
      if (shouldTrackInTests()) {
        ourTasks.add(this);
      }
      if (!builder.myDisposables.isEmpty()) {
        builder.myApplication.runReadAction(() -> expireWithDisposables(this.builder.myDisposables));
      }
    }

    private void expireWithDisposables(@Nonnull Set<? extends Disposable> disposables) {
      for (Disposable parent : disposables) {
        if (parent instanceof Project ? ((Project)parent).isDisposed() : Disposer.isDisposed(parent)) {
          cancel();
          break;
        }
        Disposable child = new Disposable() { // not a lambda to create a separate object for each parent
          @Override
          public void dispose() {
            cancel();
          }
        };
        //noinspection TestOnlyProblems
        Disposable
          parentDisposable = /*parent instanceof ProjectImpl && ((ProjectEx)parent).isLight() ? ((ProjectImpl)parent).getEarlyDisposable() : */
          parent;
        if (!Disposer.tryRegister(parentDisposable, child)) {
          cancel();
          break;
        }
        myExpirationDisposables.add(child);
      }
    }

    private boolean shouldTrackInTests() {
      return backendExecutor != SYNC_DUMMY_EXECUTOR && builder.myApplication.isUnitTestMode();
    }

    private boolean hasUnboundedExecutor() {
      return backendExecutor == AppExecutorUtil.getAppExecutorService();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      boolean result = super.cancel(mayInterruptIfRunning);
      cleanupIfNeeded();
      return result;
    }

    @Override
    public void setResult(@Nullable T t) {
      super.setResult(t);
      cleanupIfNeeded();
    }

    @Override
    public boolean setError(@Nonnull Throwable error) {
      boolean result = super.setError(error);
      cleanupIfNeeded();
      return result;
    }

    @Override
    protected boolean shouldLogErrors() {
      return backendExecutor != SYNC_DUMMY_EXECUTOR;
    }

    private void cleanupIfNeeded() {
      if (myCleaned.compareAndSet(false, true)) {
        cleanup();
      }
    }

    private void cleanup() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Cleaning " + this);
      }
      ProgressIndicator indicator = currentIndicator;
      if (indicator != null) {
        indicator.cancel();
      }
      if (builder.myCoalesceEquality != null) {
        release();
      }
      for (Disposable disposable : myExpirationDisposables) {
        Disposer.dispose(disposable);
      }
      if (hasUnboundedExecutor()) {
        ourUnboundedSubmissionTracker.unregisterSubmission(myStartTrace);
      }
      if (shouldTrackInTests()) {
        ourTasks.remove(this);
      }
    }

    private void acquire() {
      assert builder.myCoalesceEquality != null;
      synchronized (ourTasksByEquality) {
        myUseCount++;
      }
    }

    private void release() {
      assert builder.myCoalesceEquality != null;
      synchronized (ourTasksByEquality) {
        if (--myUseCount == 0 && ourTasksByEquality.get(builder.myCoalesceEquality) == this) {
          scheduleReplacementIfAny();
        }
      }
    }

    private void scheduleReplacementIfAny() {
      if (myReplacement == null || myReplacement.isDone()) {
        ourTasksByEquality.remove(builder.myCoalesceEquality, this);
      }
      else {
        ourTasksByEquality.put(builder.myCoalesceEquality, myReplacement);
        myReplacement.transferToBgThread();
      }
    }

    void submitOrScheduleCoalesced(@Nonnull List<?> coalesceEquality) {
      synchronized (ourTasksByEquality) {
        if (isDone()) return;

        Submission<?> current = ourTasksByEquality.putIfAbsent(coalesceEquality, this);
        if (current == null) {
          transferToBgThread();
        }
        else {
          if (!current.getComputationOrigin().equals(getComputationOrigin())) {
            reportCoalescingConflict(current);
          }
          if (current.myReplacement != null) {
            current.myReplacement.cancel();
            assert current == ourTasksByEquality.get(coalesceEquality);
          }
          current.myReplacement = this;
          current.cancel();
        }
      }
    }

    private void reportCoalescingConflict(@Nonnull Submission<?> current) {
      ourTasks.remove(this); // the next line will throw in tests and leave this submission hanging forever
      LOG.error("Same coalesceBy arguments are already used by " + current.getComputationOrigin() + " so they can cancel each other. " + "Please make them more unique.");
    }

    @Nonnull
    private String getComputationOrigin() {
      Object computation = builder.myComputation;
      if (computation instanceof RunnableAsCallable) {
        computation = ((RunnableAsCallable)computation).getRunnable();
      }
      String name = computation.getClass().getName();
      int dollars = name.indexOf("$$Lambda");
      return dollars >= 0 ? name.substring(0, dollars) : name;
    }

    void transferToBgThread() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Submitting " + this);
      }
      ApplicationEx app = (ApplicationEx)builder.myApplication;
      if (app.isWriteActionInProgress() || app.isWriteActionPending() || app.isReadAccessAllowed() && builder.findUnsatisfiedConstraint() != null) {
        rescheduleLater();
        return;
      }

      if (builder.myCoalesceEquality != null) {
        acquire();
      }
      try {
        backendExecutor.execute(ClientId.decorateRunnable(() -> {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Running in background " + this);
          }
          try {
            if (!attemptComputation()) {
              rescheduleLater();
            }
          }
          finally {
            if (builder.myCoalesceEquality != null) {
              release();
            }
          }
        }));
      }
      catch (RejectedExecutionException e) {
        LOG.warn("Rejected: " + this);
        throw e;
      }
    }

    T executeSynchronously() {
      try {
        while (true) {
          attemptComputation();

          if (isDone()) {
            if (isCancelled()) {
              throw new ProcessCanceledException();
            }
            try {
              return blockingGet(0, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException | ExecutionException e) {
              throw new RuntimeException(e);
            }
          }

          ProgressIndicatorUtils.checkCancelledEvenWithPCEDisabled(myProgressIndicator);
          ConstrainedExecution.ContextConstraint[] constraints = builder.myConstraints;
          if (shouldFinishOnEdt() || constraints.length != 0) {
            Semaphore semaphore = new Semaphore(1);
            builder.invokeLater(() -> {
              if (checkObsolete()) {
                semaphore.up();
              }
              else {
                BaseConstrainedExecution.scheduleWithinConstraints(semaphore::up, null, constraints);
              }
            });
            ProgressIndicatorUtils.awaitWithCheckCanceled(semaphore, myProgressIndicator);
            if (isCancelled()) {
              throw new ProcessCanceledException();
            }
          }
        }
      }
      finally {
        cleanupIfNeeded();
      }
    }

    private boolean attemptComputation() {
      ProgressIndicator indicator =
        myProgressIndicator == null ? new EmptyProgressIndicator(creationModality) : new SensitiveProgressWrapper(myProgressIndicator) {
          @Nonnull
          @Override
          public ModalityState getModalityState() {
            return creationModality;
          }
        };
      if (myProgressIndicator != null) {
        indicator.setIndeterminate(myProgressIndicator.isIndeterminate());
      }

      currentIndicator = indicator;
      try {
        SimpleReference<ConstrainedExecution.ContextConstraint> unsatisfiedConstraint = SimpleReference.create();
        boolean success;
        if (builder.myApplication.isReadAccessAllowed()) {
          insideReadAction(indicator, unsatisfiedConstraint);
          success = true;
          if (!unsatisfiedConstraint.isNull()) {
            throw new IllegalStateException("Constraint " + unsatisfiedConstraint + " cannot be satisfied");
          }
        }
        else {
          if (myProgressIndicator != null) {
            try {
              //Give ProgressSuspender a chance to suspend now, it can't do it under a read-action
              myProgressIndicator.checkCanceled();
            }
            catch (ProcessCanceledException e) {
              return false;
            }
          }
          success = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> insideReadAction(indicator, unsatisfiedConstraint),
                                                                                  indicator);
        }
        return success && unsatisfiedConstraint.isNull();
      }
      finally {
        currentIndicator = null;
      }
    }

    private void rescheduleLater() {
      if (Promises.isPending(this)) {
        builder.invokeLater(() -> reschedule());
      }
    }

    private void reschedule() {
      if (!checkObsolete()) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Rescheduling " + this);
        }
        BaseConstrainedExecution.scheduleWithinConstraints(() -> transferToBgThread(), null, builder.myConstraints);
      }
    }

    private void insideReadAction(@Nonnull ProgressIndicator indicator,
                                  @Nonnull SimpleReference<? super ConstrainedExecution.ContextConstraint> outUnsatisfiedConstraint) {
      try {
        if (checkObsolete()) {
          return;
        }
        ConstrainedExecution.ContextConstraint constraint = builder.findUnsatisfiedConstraint();
        if (constraint != null) {
          outUnsatisfiedConstraint.set(constraint);
          return;
        }

        T result = builder.myComputation.call();

        if (shouldFinishOnEdt()) {
          safeTransferToEdt(result);
        }
        else {
          setResult(result);
        }
      }
      //catch (ServiceNotReadyException e) {
      //  throw e;
      //}
      catch (ProcessCanceledException e) {
        if (!indicator.isCanceled()) {
          setError(e); // don't restart after a manually thrown PCE
        }
        throw e;
      }
      catch (Throwable e) {
        setError(e);
      }
    }

    private boolean shouldFinishOnEdt() {
      return builder.myModalityState != null;
    }

    private boolean checkObsolete() {
      if (Promises.isRejected(this)) return true;
      for (BooleanSupplier condition : builder.myCancellationConditions) {
        if (condition.getAsBoolean()) {
          cancel();
          return true;
        }
      }
      if (myProgressIndicator != null && myProgressIndicator.isCanceled()) {
        cancel();
        return true;
      }
      return false;
    }

    private void safeTransferToEdt(T result) {
      if (Promises.isRejected(this)) return;

      long stamp = AsyncExecutionServiceImpl.getWriteActionCounter();

      builder.myApplication.invokeLater(() -> {
        if (stamp != AsyncExecutionServiceImpl.getWriteActionCounter()) {
          reschedule();
          return;
        }

        if (checkObsolete()) {
          return;
        }

        setResult(result);

        if (isSucceeded()) { // in case another thread managed to cancel it just before `setResult`
          builder.myUiThreadAction.accept(result);
        }
      }, builder.myModalityState, this::isCancelled);
    }

    @Override
    public String toString() {
      return "Submission{" + builder.myComputation + ", " + getState() + "}";
    }
  }

  @Nullable
  private ConstrainedExecution.ContextConstraint findUnsatisfiedConstraint() {
    return ContainerUtil.find(myConstraints, t -> !t.isCorrectContext());
  }

  /**
   * Waits and pumps UI events until all submitted non-blocking read actions have completed. But only if they have chance to:
   * in dumb mode, submissions with {@link #inSmartMode} are ignored, because dumbness works differently in tests,
   * and a test might never switch to the smart mode at all.
   */
  @TestOnly
  public static void waitForAsyncTaskCompletion(Application application) {
    assert !application.isWriteAccessAllowed();
    for (Submission<?> task : ourTasks) {
      waitForTask(task);
    }
  }

  @TestOnly
  private static void waitForTask(@Nonnull Submission<?> task) {
    for (ConstrainedExecution.ContextConstraint constraint : task.builder.myConstraints) {
      if (constraint instanceof InSmartMode && !constraint.isCorrectContext()) {
        return;
      }
    }

    int iteration = 0;
    while (!task.isDone() && iteration++ < 60_000) {
      UIUtil.dispatchAllInvocationEvents();
      try {
        task.blockingGet(1, TimeUnit.MILLISECONDS);
        return;
      }
      catch (TimeoutException ignore) {
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (!task.isDone()) {
      //noinspection UseOfSystemOutOrSystemErr
      System.err.println(ThreadDumper.dumpThreadsToString());
      throw new AssertionError("Too long async task " + task);
    }
  }

  @TestOnly
  @Nonnull
  static Map<List<?>, Submission<?>> getTasksByEquality() {
    return ourTasksByEquality;
  }
}
