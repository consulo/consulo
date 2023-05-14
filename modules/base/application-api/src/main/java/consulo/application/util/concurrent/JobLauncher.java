// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.concurrent;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Invitation-only service for running short-lived computing-intensive IO-free tasks on all available CPU cores.
 * DO NOT USE for your tasks, IO-bound or long tasks, there are
 * {@link Application#executeOnPooledThread},
 * {@link ProcessIOExecutorService} and {@link NonUrgentExecutor} for that.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class JobLauncher {
  public static JobLauncher getInstance() {
    return Application.get().getInstance(JobLauncher.class);
  }

  /**
   * Schedules concurrent execution of #thingProcessor over each element of #things and waits for completion
   * With checkCanceled in each thread delegated to our current progress
   *
   * @param things         data to process concurrently
   * @param progress       progress indicator
   * @param thingProcessor to be invoked concurrently on each element from the collection
   * @return false if tasks have been canceled,
   * or at least one processor returned false,
   * or threw an exception,
   * or we were unable to start read action in at least one thread
   * @throws ProcessCanceledException if at least one task has thrown ProcessCanceledException
   */
  public abstract <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things, ProgressIndicator progress, @Nonnull Predicate<? super T> thingProcessor)
          throws ProcessCanceledException;

  /**
   * Schedules concurrent execution of #thingProcessor over each element of #things and waits for completion
   * With checkCanceled in each thread delegated to our current progress
   *
   * @param things                      data to process concurrently
   * @param progress                    progress indicator
   * @param failFastOnAcquireReadAction if true, returns false when failed to acquire read action
   * @param thingProcessor              to be invoked concurrently on each element from the collection
   * @return false if tasks have been canceled,
   * or at least one processor returned false,
   * or threw an exception,
   * or we were unable to start read action in at least one thread
   * @throws ProcessCanceledException if at least one task has thrown ProcessCanceledException
   * @deprecated use {@link #invokeConcurrentlyUnderProgress(List, ProgressIndicator, Predicate)} instead
   */
  @Deprecated
  public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things, ProgressIndicator progress, boolean failFastOnAcquireReadAction, @Nonnull Predicate<? super T> thingProcessor)
          throws ProcessCanceledException {
    return invokeConcurrentlyUnderProgress(things, progress, ApplicationManager.getApplication().isReadAccessAllowed(), failFastOnAcquireReadAction, thingProcessor);
  }


  public abstract <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things,
                                                              ProgressIndicator progress,
                                                              boolean runInReadAction,
                                                              boolean failFastOnAcquireReadAction,
                                                              @Nonnull Predicate<? super T> thingProcessor) throws ProcessCanceledException;

  /**
   * NEVER EVER submit runnable which can lock itself for indeterminate amount of time.
   * This will cause deadlock since this thread pool is an easily exhaustible resource.
   * Use {@link Application#executeOnPooledThread(Runnable)} instead
   */
  @Nonnull
  public abstract Job<Void> submitToJobThread(@Nonnull final Runnable action, @Nullable Consumer<? super Future<?>> onDoneCallback);
}
