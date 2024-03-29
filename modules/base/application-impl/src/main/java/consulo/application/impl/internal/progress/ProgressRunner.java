// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.progress;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityStateEx;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.internal.ApplicationWithIntentWriteLock;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.*;
import consulo.application.util.ClientId;
import consulo.application.util.Semaphore;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.util.lang.ref.Ref;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * A builder-like API for running tasks with {@link ProgressIndicator}.
 * <p>
 * Main goals of this implementation is to:
 * <ul>
 * <li>Provide a unified way of running tasks under different conditions</li>
 * <li>Remove dependence on the calling thread for API usage</li>
 * <li>Streamline extensibility</li>
 * <li>Encourage asynchronous usage</li>
 * </ul>
 * <h3>Usage</h3>
 * <p>
 * Create a new {@code ProgressRunner} object with a constructor, providing it with a task to execute.
 * Specify execution thread and progress indicator via respective calls {@link #withProgress} and {@link #onThread}, respectively.
 * Submit task and retrieve result as a {@code CompletableFuture} via {@link #submit()} or synchronously as data via {@link #submitAndGet()}
 *
 * @param <R> type of result to be computed by a given task
 */
public final class ProgressRunner<R> {
  public enum ThreadToUse {
    /**
     * Write Thread with implicit read access and the ability to execute write actions. Can be EDT.
     */
    WRITE,
    /**
     * Arbitrary thread with the ability to execute read actions.
     */
    POOLED,
    /**
     * Use only to open project on start-up.
     */
    FJ
  }

  @Nonnull
  private final Function<? super ProgressIndicator, ? extends R> myComputation;

  private final boolean isSync;

  private final boolean isModal;

  private final ThreadToUse myThreadToUse;
  @Nonnull
  private final CompletableFuture<? extends ProgressIndicator> myProgressIndicatorFuture;

  /**
   * Creates new {@code ProgressRunner} builder instance dedicated to calculating {@code computation}.
   * Does not start computation.
   *
   * @param computation runnable to be executed under progress
   */
  public ProgressRunner(@Nonnull Runnable computation) {
    this(__ -> {
      computation.run();
      return null;
    });
  }

  /**
   * Creates new {@code ProgressRunner} builder instance dedicated to calculating {@code task}.
   * Does not start computation.
   *
   * @param task task to be executed under progress
   */
  public ProgressRunner(@Nonnull Task task) {
    this(progress -> {
      try {
        task.run(progress);
      }
      finally {
        if (progress instanceof ProgressIndicatorEx) {
          ((ProgressIndicatorEx)progress).finish(task);
        }
      }
      return null;
    });
  }

  /**
   * Creates new {@code ProgressRunner} builder instance dedicated to calculating {@code computation}.
   * Does not start computation.
   *
   * @param computation runnable to be executed under progress
   */
  public ProgressRunner(@Nonnull Function<? super ProgressIndicator, ? extends R> computation) {
    this(computation, false, false, ThreadToUse.POOLED, CompletableFuture.completedFuture(new EmptyProgressIndicator()));
  }

  private ProgressRunner(@Nonnull Function<? super ProgressIndicator, ? extends R> computation,
                         boolean sync,
                         boolean modal,
                         @Nonnull ThreadToUse use,
                         @Nonnull CompletableFuture<? extends ProgressIndicator> progressIndicatorFuture) {
    myComputation = ClientId.decorateFunction(computation);
    isSync = sync;
    isModal = modal;
    myThreadToUse = use;
    myProgressIndicatorFuture = progressIndicatorFuture;
  }

  @Nonnull
  public ProgressRunner<R> sync() {
    return isSync ? this : new ProgressRunner<>(myComputation, true, isModal, myThreadToUse, myProgressIndicatorFuture);
  }

  @Nonnull
  public ProgressRunner<R> modal() {
    return isModal ? this : new ProgressRunner<>(myComputation, isSync, true, myThreadToUse, myProgressIndicatorFuture);
  }

  /**
   * Specifies thread which should execute computation. Possible values: {@link ThreadToUse#POOLED} and {@link ThreadToUse#WRITE}.
   *
   * @param thread thread to execute computation
   */
  @Nonnull
  public ProgressRunner<R> onThread(@Nonnull ThreadToUse thread) {
    return thread == myThreadToUse ? this : new ProgressRunner<>(myComputation, isSync, isModal, thread, myProgressIndicatorFuture);
  }

  /**
   * Specifies a progress indicator to be associated with computation under progress.
   *
   * @param progressIndicator progress indicator instance
   */
  @Nonnull
  public ProgressRunner<R> withProgress(@Nonnull ProgressIndicator progressIndicator) {
    ProgressIndicator myIndicator;
    try {
      myIndicator = myProgressIndicatorFuture.isDone() ? myProgressIndicatorFuture.get() : null;
    }
    catch (InterruptedException | ExecutionException e) {
      myIndicator = null;
    }
    return progressIndicator.equals(myIndicator) ? this : new ProgressRunner<>(myComputation, isSync, isModal, myThreadToUse, CompletableFuture.completedFuture(progressIndicator));
  }

  /**
   * Specifies an asynchronous computation which will be used to obtain progress indicator to be associated with computation under progress.
   * The future must return not null indicator.
   *
   * @param progressIndicatorFuture future with progress indicator
   */
  @Nonnull
  public ProgressRunner<R> withProgress(@Nonnull CompletableFuture<? extends ProgressIndicator> progressIndicatorFuture) {
    return myProgressIndicatorFuture == progressIndicatorFuture ? this : new ProgressRunner<>(myComputation, isSync, isModal, myThreadToUse, progressIndicatorFuture);
  }

  /**
   * Executes computation with the previously specified environment synchronously.
   *
   * @return a {@link ProgressResult} data class representing the result of computation
   */
  @Nonnull
  public ProgressResult<R> submitAndGet() {
    Future<ProgressResult<R>> future = sync().submit();

    try {
      return future.get();
    }
    catch (Throwable e) {
      throw new AssertionError("submit() handles exceptions and always returns successful future");
    }
  }

  /**
   * Executes computation with the previously specified environment asynchronously, or synchronously
   * if {@link #sync()} was called previously.
   *
   * @return a completable future representing the computation via {@link ProgressResult} data class
   */
  @Nonnull
  public CompletableFuture<ProgressResult<R>> submit() {
    /*
    General flow:
    1. Create Progress
    2. (opt) Get on write thread to grab modality
    3. Grab modality
    4. (opt) Release IW
    5. Run/Launch task
    6. (opt) Poll tasks on WT
    */

    boolean forceSyncExec = checkIfForceDirectExecNeeded();

    CompletableFuture<? extends ProgressIndicator> progressFuture = myProgressIndicatorFuture.thenApply(progress -> {
      // in case of abrupt application exit when 'ProgressManager.getInstance().runProcess(process, progress)' below
      // does not have a chance to run, and as a result the progress won't be disposed
      if (progress instanceof Disposable) {
        Disposer.register(ApplicationManager.getApplication(), (Disposable)progress);
      }
      return progress;
    });

    Semaphore modalityEntered = new Semaphore(forceSyncExec ? 0 : 1);

    Supplier<R> onThreadCallable = () -> {
      Ref<R> result = Ref.create();
      if (isModal) {
        modalityEntered.waitFor();
      }

      // runProcess handles starting/stopping progress and setting thread's current progress
      ProgressIndicator progressIndicator;
      try {
        progressIndicator = progressFuture.join();
      }
      catch (Throwable e) {
        throw new RuntimeException("Can't get progress", e);
      }
      //noinspection ConstantConditions
      if (progressIndicator == null) {
        throw new IllegalStateException("Expected not-null progress indicator but got null from " + myProgressIndicatorFuture);
      }

      ProgressManager.getInstance().runProcess(() -> result.set(myComputation.apply(progressIndicator)), progressIndicator);
      return result.get();
    };

    CompletableFuture<R> resultFuture;
    if (forceSyncExec) {
      resultFuture = new CompletableFuture<>();
      try {
        resultFuture.complete(onThreadCallable.get());
      }
      catch (Throwable t) {
        resultFuture.completeExceptionally(t);
      }
    }
    else if (ApplicationManager.getApplication().isDispatchThread()) {
      resultFuture = execFromEDT(progressFuture, modalityEntered, onThreadCallable);
    }
    else {
      resultFuture = normalExec(progressFuture, modalityEntered, onThreadCallable);
    }

    return resultFuture.handle((result, e) -> {
      Throwable throwable = unwrap(e);
      return new ProgressResult<>(result, throwable instanceof ProcessCanceledException || isCanceled(progressFuture), throwable);
    });
  }

  // The case of sync exec from the EDT without the ability to poll events (no BlockingProgressIndicator#startBlocking or presence of Write Action)
  // must be handled by very synchronous direct call (alt: use proper progress indicator, i.e. PotemkinProgress or ProgressWindow).
  // Note: running sync task on pooled thread from EDT can lead to deadlock if pooled thread will try to invokeAndWait.
  private boolean checkIfForceDirectExecNeeded() {
    if (isSync && UIAccess.isUIThread() && !ApplicationManager.getApplication().isWriteThread()) {
      throw new IllegalStateException("Running sync tasks on pure EDT (w/o IW lock) is dangerous for several reasons.");
    }
    if (!isSync && isModal && UIAccess.isUIThread()) {
      throw new IllegalStateException("Running async modal tasks from EDT is impossible: modal implies sync dialog show + polling events");
    }

    boolean forceDirectExec = isSync && ApplicationManager.getApplication().isDispatchThread() && (ApplicationManager.getApplication().isWriteAccessAllowed() || !isModal);
    if (forceDirectExec) {
      String reason = ApplicationManager.getApplication().isWriteAccessAllowed() ? "inside Write Action" : "not modal execution";
      @NonNls String failedConstraints = "";
      if (isModal) failedConstraints += "Use Modal execution; ";
      if (myThreadToUse == ThreadToUse.POOLED || myThreadToUse == ThreadToUse.FJ) failedConstraints += "Use pooled thread; ";
      failedConstraints = failedConstraints.isEmpty() ? "none" : failedConstraints;
      Logger.getInstance(ProgressRunner.class).warn("Forced to sync exec on EDT. Reason: " + reason + ". Failed constraints: " + failedConstraints, new Throwable());
    }
    return forceDirectExec;
  }

  @Nonnull
  private CompletableFuture<R> execFromEDT(@Nonnull CompletableFuture<? extends ProgressIndicator> progressFuture, @Nonnull Semaphore modalityEntered, @Nonnull Supplier<R> onThreadCallable) {
    CompletableFuture<R> taskFuture = launchTask(onThreadCallable, progressFuture);
    CompletableFuture<R> resultFuture;

    if (isModal) {
      // Running task with blocking EDT event pumping has the following contract in test mode:
      //   if a random EDT event processed by blockingPI fails with an exception, the event pumping
      //   is stopped and the submitted task fails with respective exception.
      // The task submitted to e.g. POOLED thread might not be able to finish at all because it requires invoke&waits,
      //   but EDT is broken due an exception. Hence, initial task should be completed exceptionally
      CompletableFuture<Void> blockingRunFuture = progressFuture.thenAccept(progressIndicator -> {
        if (progressIndicator instanceof BlockingProgressIndicator) {
          //noinspection deprecation
          ((BlockingProgressIndicator)progressIndicator).startBlocking(modalityEntered::up, taskFuture);
        }
        else {
          Logger.getInstance(ProgressRunner.class).warn("Can't go modal without BlockingProgressIndicator");
          modalityEntered.up();
        }
      }).exceptionally(throwable -> {
        taskFuture.completeExceptionally(throwable);
        return null;
      });
      // `startBlocking` might throw unrelated exceptions execute the submitted task so potential failure should be handled.
      // Relates to testMode-ish execution: unhandled exceptions on event pumping are `LOG.error`-ed, hence throw exceptions in tests.
      resultFuture = taskFuture.thenCombine(blockingRunFuture, (r, __) -> r);
    }
    else {
      resultFuture = taskFuture;
    }

    if (isSync) {
      try {
        resultFuture.get();
      }
      catch (Throwable ignore) {
        // ignore possible exceptions, as they will be handled by the subsequent get/whenComplete calls.
      }
    }
    return resultFuture;
  }

  @Nonnull
  private CompletableFuture<R> normalExec(@Nonnull CompletableFuture<? extends ProgressIndicator> progressFuture, @Nonnull Semaphore modalityEntered, @Nonnull Supplier<R> onThreadCallable) {

    if (isModal) {
      Function<ProgressIndicator, ProgressIndicator> modalityRunnable = progressIndicator -> {
        LaterInvocator.enterModal(progressIndicator, (IdeaModalityStateEx)progressIndicator.getModalityState());
        modalityEntered.up();
        return progressIndicator;
      };
      // If a progress indicator has not been calculated yet, grabbing IW lock might lead to deadlock, as progress might need it for init
      progressFuture = progressFuture.thenApplyAsync(modalityRunnable, r -> {
        if (ApplicationManager.getApplication().isWriteThread()) {
          r.run();
        }
        else {
          ApplicationManager.getApplication().invokeLaterOnWriteThread(r);
        }
      });
    }

    CompletableFuture<R> resultFuture = launchTask(onThreadCallable, progressFuture);

    if (isModal) {
      CompletableFuture<Void> modalityExitFuture = resultFuture.handle((r, throwable) -> r) // ignore result computation exception
              .thenAcceptBoth(progressFuture, (r, progressIndicator) -> {
                if (ApplicationManager.getApplication().isWriteThread()) {
                  LaterInvocator.leaveModal(progressIndicator);
                }
                else {
                  ApplicationManager.getApplication().invokeLaterOnWriteThread(() -> LaterInvocator.leaveModal(progressIndicator), (ModalityState)progressIndicator.getModalityState());
                }
              });

      // It's better to associate task future with modality exit so that future finish will lead to expected state (modality exit)
      resultFuture = resultFuture.thenCombine(modalityExitFuture, (r, __) -> r);
    }

    if (isSync) {
      waitForFutureUnlockingThread(resultFuture);
    }
    return resultFuture;
  }

  private static void waitForFutureUnlockingThread(@Nonnull CompletableFuture<?> resultFuture) {
    if (UIAccess.isUIThread()) {
      throw new UnsupportedOperationException("Sync waiting from EDT is dangerous.");
    }
    try {
      resultFuture.get();
    }
    catch (Throwable ignore) {
    }
  }

  private static void pollLaterInvocatorActively(@Nonnull CompletableFuture<?> resultFuture, @Nonnull Runnable pollAction) {
    ((ApplicationWithIntentWriteLock)Application.get()).runUnlockingIntendedWrite(() -> {
      while (true) {
        try {
          resultFuture.get(10, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ignore) {
          ((ApplicationWithIntentWriteLock)Application.get()).runIntendedWriteActionOnCurrentThread(pollAction);
          continue;
        }
        catch (Throwable ignored) {
        }
        break;
      }
      return null;
    });
  }

  public static boolean isCanceled(@Nonnull Future<? extends ProgressIndicator> progressFuture) {
    try {
      return progressFuture.get().isCanceled();
    }
    catch (Throwable e) {
      return false;
    }
  }

  public static Throwable unwrap(@Nullable Throwable exception) {
    return exception instanceof CompletionException || exception instanceof ExecutionException ? exception.getCause() : exception;
  }

  @Nonnull
  private CompletableFuture<R> launchTask(@Nonnull Supplier<R> callable, @Nonnull CompletableFuture<? extends ProgressIndicator> progressIndicatorFuture) {
    CompletableFuture<R> resultFuture;
    switch (myThreadToUse) {
      case POOLED:
        resultFuture = CompletableFuture.supplyAsync(callable, AppExecutorUtil.getAppExecutorService());
        break;
      case FJ:
        resultFuture = CompletableFuture.supplyAsync(callable, ForkJoinPool.commonPool());
        break;
      case WRITE:
        resultFuture = new CompletableFuture<>();
        Runnable runnable = () -> {
          try {
            resultFuture.complete(callable.get());
          }
          catch (Throwable e) {
            resultFuture.completeExceptionally(e);
          }
        };

        progressIndicatorFuture.whenComplete((progressIndicator, throwable) -> {
          if (throwable != null) {
            resultFuture.completeExceptionally(throwable);
            return;
          }
          ModalityState processModality = (ModalityState)progressIndicator.getModalityState();
          ApplicationManager.getApplication().invokeLaterOnWriteThread(runnable, processModality);
        });
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + myThreadToUse);
    }
    return resultFuture;
  }
}
