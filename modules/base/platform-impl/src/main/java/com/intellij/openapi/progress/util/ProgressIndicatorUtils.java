/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.progress.util;

import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.Semaphore;
import consulo.logging.Logger;
import consulo.util.lang.ExceptionUtil;
import javax.annotation.Nonnull;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;

/**
 * Methods in this class are used to equip long background processes which take read actions with a special listener
 * that fires when a write action is about to begin, and cancels corresponding progress indicators to avoid blocking the UI.
 * These processes should be ready to get {@link ProcessCanceledException} at any moment.
 * Processes may want to react on cancellation event by restarting the activity, see
 * {@link ReadTask#onCanceled(ProgressIndicator)} for that.
 *
 * @author gregsh
 */
public class ProgressIndicatorUtils {
  private static final Logger LOG = Logger.getInstance(ProgressIndicatorUtils.class);

  private ProgressIndicatorUtils() {
  }

  @Nonnull
  public static ProgressIndicator forceWriteActionPriority(@Nonnull ProgressIndicator progress, @Nonnull Disposable parentDisposable) {
    ApplicationManager.getApplication().addApplicationListener(new ApplicationAdapter() {
      @Override
      public void beforeWriteActionStart(@Nonnull Object action) {
        if (progress.isRunning()) {
          progress.cancel();
        }
      }
    }, parentDisposable);
    return progress;
  }

  @Nonnull
  public static CompletableFuture<?> submitWithWriteActionPriority(@Nonnull ReadTask task) {
    return scheduleWithWriteActionPriority(new ProgressIndicatorBase(), task);
  }

  public static void scheduleWithWriteActionPriority(@Nonnull ReadTask task) {
    submitWithWriteActionPriority(task);
  }

  @Nonnull
  public static CompletableFuture<?> scheduleWithWriteActionPriority(@Nonnull ProgressIndicator progressIndicator, @Nonnull ReadTask readTask) {
    return scheduleWithWriteActionPriority(progressIndicator, PooledThreadExecutor.INSTANCE, readTask);
  }

  @Nonnull
  public static CompletableFuture<?> scheduleWithWriteActionPriority(@Nonnull Executor executor, @Nonnull ReadTask task) {
    return scheduleWithWriteActionPriority(new ProgressIndicatorBase(), executor, task);
  }

  /**
   * Same as {@link #runInReadActionWithWriteActionPriority(Runnable)}, optionally allowing to pass a {@link ProgressIndicator}
   * instance, which can be used to cancel action externally.
   */
  public static boolean runInReadActionWithWriteActionPriority(@Nonnull final Runnable action,
                                                               @Nullable ProgressIndicator progressIndicator) {
    final Ref<Boolean> result = new Ref<>(Boolean.FALSE);
    runWithWriteActionPriority(() -> result.set(ApplicationManagerEx.getApplicationEx().tryRunReadAction(action)),
                               progressIndicator == null ? new ProgressIndicatorBase() : progressIndicator);
    return result.get();
  }

  /**
   * This method attempts to run provided action synchronously in a read action, so that, if possible, it wouldn't impact any pending,
   * executing or future write actions (for this to work effectively the action should invoke {@link ProgressManager#checkCanceled()} or
   * {@link ProgressIndicator#checkCanceled()} often enough).
   * It returns {@code true} if action was executed successfully. It returns {@code false} if the action was not
   * executed successfully, i.e. if:
   * <ul>
   * <li>write action was in progress when the method was called</li>
   * <li>write action was pending when the method was called</li>
   * <li>action started to execute, but was aborted using {@link ProcessCanceledException} when some other thread initiated
   * write action</li>
   * </ul>
   * If caller needs to retry the invocation of this method in a loop, it should consider pausing between attempts, to avoid potential
   * 100% CPU usage.
   */
  public static boolean runInReadActionWithWriteActionPriority(@Nonnull final Runnable action) {
    return runInReadActionWithWriteActionPriority(action, null);
  }

  public static boolean runWithWriteActionPriority(@Nonnull Runnable action, @Nonnull ProgressIndicator progressIndicator) {
    final ApplicationEx application = (ApplicationEx)ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      throw new IllegalStateException("Must not call from EDT");
    }
    if (application.isWriteActionPending()) {
      // first catch: check if write action acquisition started: especially important when current thread has read action, because
      // tryRunReadAction below would just run without really checking if a write action is pending
      if (!progressIndicator.isCanceled()) progressIndicator.cancel();
      return false;
    }

    final ApplicationAdapter listener = new ApplicationAdapter() {
      @Override
      public void beforeWriteActionStart(@Nonnull Object action) {
        if (!progressIndicator.isCanceled()) progressIndicator.cancel();
      }
    };

    boolean succeededWithAddingListener = application.tryRunReadAction(() -> {
      // Even if writeLock.lock() acquisition is in progress at this point then runProcess will block wanting read action which is
      // also ok as last resort.
      application.addApplicationListener(listener);
    });
    if (!succeededWithAddingListener) { // second catch: writeLock.lock() acquisition is in progress or already acquired
      if (!progressIndicator.isCanceled()) progressIndicator.cancel();
      return false;
    }
    final Ref<Boolean> wasCancelled = new Ref<>();
    try {
      ProgressManager.getInstance().runProcess(() -> {
        try {
          action.run();
        }
        catch (ProcessCanceledException ignore) {
          wasCancelled.set(Boolean.TRUE);
        }
      }, progressIndicator);
    }
    finally {
      application.removeApplicationListener(listener);
    }
    return wasCancelled.get() != Boolean.TRUE;
  }

  @Nonnull
  public static CompletableFuture<?> scheduleWithWriteActionPriority(@Nonnull final ProgressIndicator progressIndicator,
                                                                     @Nonnull final Executor executor,
                                                                     @Nonnull final ReadTask readTask) {
    // invoke later even if on EDT
    // to avoid tasks eagerly restarting immediately, allocating many pooled threads
    // which get cancelled too soon when a next write action arrives in the same EDT batch
    // (can happen when processing multiple VFS events or writing multiple files on save)

    // use SwingUtilities instead of application.invokeLater
    // to tolerate any immediate modality changes (e.g. https://youtrack.jetbrains.com/issue/IDEA-135180)

    CompletableFuture<?> future = new CompletableFuture<>();
    SwingUtilities.invokeLater(() -> {
      final Application application = ApplicationManager.getApplication();
      if (application.isDisposed() || progressIndicator.isCanceled()) {
        future.complete(null);
        return;
      }
      final ApplicationAdapter listener = new ApplicationAdapter() {
        @Override
        public void beforeWriteActionStart(@Nonnull Object action) {
          if (!progressIndicator.isCanceled()) {
            progressIndicator.cancel();
            readTask.onCanceled(progressIndicator);
          }
        }
      };
      application.addApplicationListener(listener);
      future.whenComplete((BiConsumer<Object, Throwable>)(o, throwable) -> application.removeApplicationListener(listener));
      try {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            final ReadTask.Continuation continuation;
            try {
              continuation = runUnderProgress(progressIndicator, readTask);
            }
            catch (Throwable e) {
              future.completeExceptionally(e);
              throw e;
            }
            if (continuation == null) {
              future.complete(null);
            }
            else {
              application.invokeLater(new Runnable() {
                @Override
                public void run() {
                  application.removeApplicationListener(listener); // remove listener early to prevent firing it during continuation execution
                  try {
                    if (!progressIndicator.isCanceled()) {
                      continuation.getAction().run();
                    }
                  }
                  finally {
                    future.complete(null);
                  }
                }

                @Override
                public String toString() {
                  return "continuation of " + readTask;
                }
              }, continuation.getModalityState());
            }
          }

          @Override
          public String toString() {
            return readTask.toString();
          }
        });
      }
      catch (RuntimeException | Error e) {
        future.completeExceptionally(e);
        throw e;
      }
    });
    return future;
  }

  private static ReadTask.Continuation runUnderProgress(@Nonnull final ProgressIndicator progressIndicator, @Nonnull final ReadTask task) {
    return ProgressManager.getInstance().runProcess(() -> {
      try {
        return task.runBackgroundProcess(progressIndicator);
      }
      catch (ProcessCanceledException ignore) {
        return null;
      }
    }, progressIndicator);
  }

  /**
   * Ensure the current EDT activity finishes in case it requires many write actions, with each being delayed a bit
   * by background thread read action (until its first checkCanceled call). Shouldn't be called from under read action.
   */
  public static void yieldToPendingWriteActions() {
    Application application = ApplicationManager.getApplication();
    if (application.isReadAccessAllowed()) {
      throw new IllegalStateException("Mustn't be called from within read action");
    }
    if (application.isDispatchThread()) {
      throw new IllegalStateException("Mustn't be called from EDT");
    }
    application.invokeAndWait(EmptyRunnable.INSTANCE, ModalityState.any());
  }

  /**
   * Run the given computation with its execution time restricted to the given amount of time in milliseconds.<p></p>
   * <p>
   * Internally, it creates a new {@link ProgressIndicator}, runs the computation with that indicator and cancels it after the the timeout.
   * The computation should call {@link ProgressManager#checkCanceled()} frequently enough, so that after the timeout has been exceeded
   * it can stop the execution by throwing {@link ProcessCanceledException}, which will be caught by this {@code withTimeout}.<p></p>
   * <p>
   * If a {@link ProcessCanceledException} happens due to any other reason (e.g. a thread's progress indicator got canceled),
   * it'll be thrown out of this method.
   *
   * @return the computation result or {@code null} if timeout has been exceeded.
   */
  @Nullable
  public static <T> T withTimeout(long timeoutMs, @Nonnull Computable<T> computation) {
    ProgressManager.checkCanceled();
    ProgressIndicator outer = ProgressIndicatorProvider.getGlobalProgressIndicator();
    ProgressIndicator inner = outer != null ? new SensitiveProgressWrapper(outer) : new ProgressIndicatorBase(false, false);
    AtomicBoolean canceledByTimeout = new AtomicBoolean();
    ScheduledFuture<?> cancelProgress = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
      canceledByTimeout.set(true);
      inner.cancel();
    }, timeoutMs, TimeUnit.MILLISECONDS);
    try {
      return ProgressManager.getInstance().runProcess(computation, inner);
    }
    catch (ProcessCanceledException e) {
      if (canceledByTimeout.get()) {
        return null;
      }
      throw e; // canceled not by timeout
    }
    finally {
      cancelProgress.cancel(false);
    }
  }

  public static void awaitWithCheckCanceled(@Nonnull CountDownLatch waiter) {
    awaitWithCheckCanceled(() -> waiter.await(10, TimeUnit.MILLISECONDS));
  }

  public static <T> T awaitWithCheckCanceled(@Nonnull Future<T> future) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    return awaitWithCheckCanceled(future, indicator);
  }

  public static <T> T awaitWithCheckCanceled(@Nonnull Future<T> future, @Nullable ProgressIndicator indicator) {
    while (true) {
      checkCancelledEvenWithPCEDisabled(indicator);
      try {
        return future.get(10, TimeUnit.MILLISECONDS);
      }
      catch (TimeoutException | RejectedExecutionException ignore) {
      }
      catch (Throwable e) {
        Throwable cause = e.getCause();
        if (cause instanceof ProcessCanceledException) {
          throw (ProcessCanceledException)cause;
        }
        if (cause instanceof CancellationException) {
          throw new ProcessCanceledException(cause);
        }
        ExceptionUtil.rethrow(e);
      }
    }
  }

  public static void awaitWithCheckCanceled(@Nonnull Lock lock, int timeout, @Nonnull TimeUnit timeUnit) {
    awaitWithCheckCanceled(() -> lock.tryLock(timeout, timeUnit));
  }

  public static void awaitWithCheckCanceled(@Nonnull ThrowableComputable<Boolean, ? extends Exception> waiter) {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    boolean success = false;
    while (!success) {
      checkCancelledEvenWithPCEDisabled(indicator);
      try {
        success = waiter.compute();
      }
      catch (Exception e) {
        LOG.warn(e);
        throw new ProcessCanceledException(e);
      }
    }
  }

  /**
   * Use when otherwise a deadlock is possible.
   */
  public static void checkCancelledEvenWithPCEDisabled(@Nullable ProgressIndicator indicator) {
    if (indicator != null && indicator.isCanceled()) {
      indicator.checkCanceled(); // maybe it'll throw with some useful additional information
      throw new ProcessCanceledException();
    }
  }

  public static void awaitWithCheckCanceled(@Nonnull Semaphore semaphore, @Nullable ProgressIndicator indicator) {
    while (!semaphore.waitFor(10)) {
      checkCancelledEvenWithPCEDisabled(indicator);
    }
  }
}
