// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.concurrency;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.SensitiveProgressWrapper;
import consulo.application.impl.internal.progress.StandardProgressIndicatorBase;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ApplicationUtil;
import consulo.application.util.concurrent.Job;
import consulo.application.util.concurrent.JobLauncher;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author cdr
 */
@Singleton
@ServiceImpl
public class JobLauncherImpl extends JobLauncher {
  private static final Logger LOG = Logger.getInstance(JobLauncherImpl.class);
  static final int CORES_FORK_THRESHOLD = 1;

  @Override
  public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things, ProgressIndicator progress, @Nonnull Predicate<? super T> thingProcessor) throws ProcessCanceledException {
    ApplicationEx app = (ApplicationEx)Application.get();
    return invokeConcurrentlyUnderProgress(things, progress, app.isReadAccessAllowed(), app.isInImpatientReader(), thingProcessor);
  }

  @Override
  public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<? extends T> things,
                                                     ProgressIndicator progress,
                                                     boolean runInReadAction,
                                                     boolean failFastOnAcquireReadAction,
                                                     @Nonnull Predicate<? super T> thingProcessor) throws ProcessCanceledException {
    // supply our own indicator even if we haven't given one - to support cancellation
    // use StandardProgressIndicator by default to avoid assertion in SensitiveProgressWrapper() ctr later
    ProgressIndicator wrapper = progress == null ? new StandardProgressIndicatorBase() : new SensitiveProgressWrapper(progress);

    Boolean result = processImmediatelyIfTooFew(things, wrapper, runInReadAction, thingProcessor);
    if (result != null) return result.booleanValue();

    ProgressManager pm = ProgressManager.getInstance();
    Predicate<? super T> processor = ((CoreProgressManager)pm).isPrioritizedThread(Thread.currentThread()) ? t -> pm.computePrioritized(() -> thingProcessor.test(t)) : thingProcessor;

    List<ApplierCompleter<T>> failedSubTasks = Collections.synchronizedList(new ArrayList<>());
    ApplierCompleter<T> applier = new ApplierCompleter<>(null, runInReadAction, failFastOnAcquireReadAction, wrapper, things, processor, 0, things.size(), failedSubTasks, null);
    try {
      ForkJoinPool.commonPool().execute(applier);
      // call checkCanceled a bit more often than .invoke()
      while (!applier.isDone()) {
        ProgressManager.checkCanceled();
        // does automatic compensation against starvation (in ForkJoinPool.awaitJoin)
        try {
          applier.get(10, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ignored) {
        }
        catch (ExecutionException e) {
          throw e.getCause();
        }
      }
      if (applier.throwable != null) {
        throw applier.throwable;
      }
    }
    catch (ApplierCompleter.ComputationAbortedException e) {
      // one of the processors returned false
      return false;
    }
    catch (ApplicationUtil.CannotRunReadActionException e) {
      // failFastOnAcquireReadAction==true and one of the processors called runReadAction() during the pending write action
      throw e;
    }
    catch (ProcessCanceledException e) {
      LOG.debug(e);
      // task1.processor returns false and the task cancels the indicator
      // then task2 calls checkCancel() and get here
      return false;
    }
    catch (RuntimeException | Error e) {
      throw e;
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }
    //assert applier.isDone();
    return applier.completeTaskWhichFailToAcquireReadAction();
  }

  // if {@code things} are too few to be processed in the real pool, returns TRUE if processed successfully, FALSE if not
  // returns null if things need to be processed in the real pool
  private static <T> Boolean processImmediatelyIfTooFew(@Nonnull List<? extends T> things,
                                                        @Nonnull ProgressIndicator progress,
                                                        boolean runInReadAction,
                                                        @Nonnull Predicate<? super T> thingProcessor) {
    // commit can be invoked from within write action
    //if (runInReadAction && ApplicationManager.getApplication().isWriteAccessAllowed()) {
    //  throw new RuntimeException("Must not run invokeConcurrentlyUnderProgress() from under write action because of imminent deadlock");
    //}
    if (things.isEmpty()) return true;

    if (things.size() <= 1 || JobSchedulerImpl.getJobPoolParallelism() <= CORES_FORK_THRESHOLD || runInReadAction && ApplicationManager.getApplication().isWriteAccessAllowed()) {
      AtomicBoolean result = new AtomicBoolean(true);
      Runnable runnable = () -> ProgressManager.getInstance().executeProcessUnderProgress(() -> {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < things.size(); i++) {
          T thing = things.get(i);
          if (!thingProcessor.test(thing)) {
            result.set(false);
            break;
          }
        }
      }, progress);
      if (runInReadAction) {
        ApplicationManager.getApplication().runReadAction(runnable);
      }
      else {
        runnable.run();
      }
      return result.get();
    }
    return null;
  }

  // This implementation is not really async

  @Nonnull
  @Override
  public Job<Void> submitToJobThread(@Nonnull Runnable action, @Nullable Consumer<? super Future<?>> onDoneCallback) {
    VoidForkJoinTask task = new VoidForkJoinTask(action, onDoneCallback);
    task.submit();
    return task;
  }

  private static class VoidForkJoinTask implements Job<Void> {
    private final Runnable myAction;
    private final Consumer<? super Future<?>> myOnDoneCallback;

    private enum Status {
      STARTED,
      EXECUTED
    } // null=not yet executed, STARTED=started execution, EXECUTED=finished

    private volatile Status myStatus;
    private final ForkJoinTask<Void> myForkJoinTask = new ForkJoinTask<Void>() {
      @Override
      public Void getRawResult() {
        return null;
      }

      @Override
      protected void setRawResult(Void value) {
      }

      @Override
      protected boolean exec() {
        myStatus = Status.STARTED;
        try {
          myAction.run();
          complete(null); // complete manually before calling callback
        }
        catch (Throwable throwable) {
          completeExceptionally(throwable);
        }
        finally {
          myStatus = Status.EXECUTED;
          if (myOnDoneCallback != null) {
            myOnDoneCallback.accept(this);
          }
        }
        return true;
      }
    };

    private VoidForkJoinTask(@Nonnull Runnable action, @Nullable Consumer<? super Future<?>> onDoneCallback) {
      myAction = action;
      myOnDoneCallback = onDoneCallback;
    }

    private void submit() {
      ForkJoinPool.commonPool().execute(myForkJoinTask);
    }
    //////////////// Job

    // when canceled in the middle of the execution returns false until finished
    @Override
    public boolean isDone() {
      boolean wasCancelled = myForkJoinTask.isCancelled(); // must be before status check
      Status status = myStatus;
      return status == Status.EXECUTED || status == null && wasCancelled;
    }

    @Override
    public boolean isCanceled() {
      return myForkJoinTask.isCancelled();
    }

    @Override
    public void cancel() {
      myForkJoinTask.cancel(true);
    }

    // waits for the job to finish execution (when called on a canceled job in the middle of the execution, wait for finish)
    @Override
    public void waitForCompletion(int millis) throws InterruptedException, ExecutionException, TimeoutException {
      while (!isDone()) {
        try {
          myForkJoinTask.get(millis, TimeUnit.MILLISECONDS);
          break;
        }
        catch (CancellationException e) {
          // was canceled in the middle of execution
          // can't do anything but wait. help other tasks in the meantime
          if (!isDone()) {
            ForkJoinPool.commonPool().awaitQuiescence(millis, TimeUnit.MILLISECONDS);
            if (!isDone()) throw new TimeoutException();
          }
        }
      }
    }
  }

  /**
   * Process all elements from the {@code failedToProcess} and then {@code things} concurrently in the underlying pool.
   * Processing happens concurrently maintaining {@code JobSchedulerImpl.CORES_COUNT} parallelism.
   * Stop when {@code tombStone} element is occurred.
   * If was unable to process some element, add it back to the {@code failedToProcess} queue.
   *
   * @return true if all elements processed successfully, false if at least one processor returned false or exception occurred
   */
  public <T> boolean processQueue(@Nonnull final BlockingQueue<T> things,
                                  @Nonnull final Queue<T> failedToProcess,
                                  @Nonnull final ProgressIndicator progress,
                                  @Nonnull final T tombStone,
                                  @Nonnull final Predicate<? super T> thingProcessor) {
    class MyTask implements Callable<Boolean> {
      private final int mySeq;
      private boolean result;

      private MyTask(int seq) {
        mySeq = seq;
      }

      @Override
      public Boolean call() {
        ProgressManager.getInstance().executeProcessUnderProgress(() -> {
          try {
            while (true) {
              ProgressManager.checkCanceled();
              T element = failedToProcess.poll();
              if (element == null) element = things.take();

              if (element == tombStone) {
                things.offer(element);
                result = true;
                break;
              }
              try {
                if (!thingProcessor.test(element)) {
                  result = false;
                  break;
                }
              }
              catch (RuntimeException e) {
                failedToProcess.add(element);
                throw e;
              }
            }
          }
          catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }, progress);
        return result;
      }

      @Override
      public String toString() {
        return super.toString() + " seq=" + mySeq;
      }
    }
    progress.checkCanceled(); // do not start up expensive threads if there's no need to
    boolean isSmallEnough = things.contains(tombStone);
    if (isSmallEnough) {
      try {
        // do not distribute for small queues
        return new MyTask(0).call();
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    List<ForkJoinTask<Boolean>> tasks = new ArrayList<>();
    for (int i = 0; i < Math.max(1, JobSchedulerImpl.getJobPoolParallelism() - 1); i++) {
      tasks.add(ForkJoinPool.commonPool().submit(new MyTask(i)));
    }

    boolean result = true;
    RuntimeException exception = null;
    for (ForkJoinTask<Boolean> task : tasks) {
      try {
        result &= task.join();
      }
      catch (RuntimeException e) {
        exception = e;
      }
    }
    if (exception != null) {
      throw exception;
    }
    return result;
  }
}
