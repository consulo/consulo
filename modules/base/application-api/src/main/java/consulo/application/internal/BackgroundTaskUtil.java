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
package consulo.application.internal;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBus;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BackgroundTaskUtil {
  private static final Logger LOG = Logger.getInstance(BackgroundTaskUtil.class);

  /**
   * Executor to perform <i>possibly</i> long operation on pooled thread.
   * If computation was performed within given time frame,
   * the computed callback will be executed synchronously (avoiding unnecessary <tt>invokeLater()</tt>).
   * In this case, {@code onSlowAction} will not be executed at all.
   * <ul>
   * <li> If the computation is fast, execute callback synchronously.
   * <li> If the computation is slow, execute <tt>onSlowAction</tt> synchronously. When the computation is completed, execute callback in EDT.
   * </ul><p>
   * It can be used to reduce blinking when background task might be completed fast.<br>
   * A Simple approach:
   * <pre>
   * onSlowAction.run() // show "Loading..."
   * executeOnPooledThread({
   *   Runnable callback = backgroundTask(); // some background computations
   *   invokeLater(callback); // apply changes
   * });
   * </pre>
   * will lead to "Loading..." visible between current moment and execution of invokeLater() event.
   * This period can be very short and looks like 'jumping' if background operation is fast.
   */
  @Nonnull
  @RequiredUIAccess
  public static ProgressIndicator executeAndTryWait(@Nonnull Function<ProgressIndicator, /*@NotNull*/ Runnable> backgroundTask,
                                                    @Nullable Runnable onSlowAction,
                                                    long waitMillis,
                                                    boolean forceEDT) {
    ModalityState modality = Application.get().getCurrentModalityState();

    if (forceEDT) {
      ProgressIndicator indicator = new EmptyProgressIndicator(modality);
      try {
        Runnable callback = backgroundTask.apply(indicator);
        finish(callback, indicator);
      }
      catch (ProcessCanceledException ignore) {
      }
      catch (Throwable t) {
        LOG.error(t);
      }
      return indicator;
    }
    else {
      Pair<Runnable, ProgressIndicator> pair =
        computeInBackgroundAndTryWait(backgroundTask,
                                      (callback, indicator) -> ApplicationManager.getApplication()
                                                                                 .invokeLater(() -> finish(callback, indicator), modality),
                                      modality,
                                      waitMillis);

      Runnable callback = pair.first;
      ProgressIndicator indicator = pair.second;

      if (callback != null) {
        finish(callback, indicator);
      }
      else {
        if (onSlowAction != null) onSlowAction.run();
      }

      return indicator;
    }
  }

  @RequiredUIAccess
  private static void finish(@Nonnull Runnable result, @Nonnull ProgressIndicator indicator) {
    if (!indicator.isCanceled()) result.run();
  }

  /**
   * Try to compute value in background and abort computation if it takes too long.
   * <ul>
   * <li> If the computation is fast, return computed value.
   * <li> If the computation is slow, abort computation (cancel ProgressIndicator).
   * </ul>
   */
  @Nullable
  @RequiredUIAccess
  public static <T> T tryComputeFast(@Nonnull Function<ProgressIndicator, T> backgroundTask, long waitMillis) {
    Pair<T, ProgressIndicator> pair = computeInBackgroundAndTryWait(backgroundTask, (result, indicator) -> {
    }, Application.get().getDefaultModalityState(), waitMillis);

    T result = pair.first;
    ProgressIndicator indicator = pair.second;

    indicator.cancel();
    return result;
  }

  @Nullable
  public static <T> T computeInBackgroundAndTryWait(@Nonnull Supplier<T> computable, @Nonnull Consumer<T> asyncCallback, long waitMillis) {
    Pair<T, ProgressIndicator> pair =
      computeInBackgroundAndTryWait(indicator -> computable.get(),
                                    (result, indicator) -> asyncCallback.accept(result),
                                    Application.get().getDefaultModalityState(),
                                    waitMillis);
    return pair.first;
  }

  /**
   * Compute value in background and try wait for its completion.
   * <ul>
   * <li> If the computation is fast, return computed value synchronously. Callback will not be called in this case.
   * <li> If the computation is slow, return <tt>null</tt>. When the computation is completed, pass the value to the callback.
   * </ul>
   * Callback will be executed on the same thread as the background task.
   */
  @Nonnull
  private static <T> Pair<T, ProgressIndicator> computeInBackgroundAndTryWait(@Nonnull Function<ProgressIndicator, T> task,
                                                                              @Nonnull BiConsumer<T, ProgressIndicator> asyncCallback,
                                                                              @Nonnull ModalityState modality,
                                                                              long waitMillis) {
    ProgressIndicator indicator = new EmptyProgressIndicator(modality);

    Helper<T> helper = new Helper<>();

    indicator.start();
    ApplicationManager.getApplication().executeOnPooledThread(() -> ProgressManager.getInstance().executeProcessUnderProgress(() -> {
      try {
        T result = task.apply(indicator);
        if (!helper.setResult(result)) {
          asyncCallback.accept(result, indicator);
        }
      }
      finally {
        indicator.stop();
      }
    }, indicator));

    T result = null;
    if (helper.await(waitMillis)) {
      result = helper.getResult();
    }

    return Pair.create(result, indicator);
  }


  /**
   * An alternative to plain {@link Application#executeOnPooledThread(Runnable)} which wraps the task in a process with a
   * {@link ProgressIndicator} which gets cancelled when the given disposable is disposed. <br/><br/>
   * <p>
   * This allows to stop a lengthy background activity by calling {@link ProgressManager#checkCanceled()}
   * and avoid Already Disposed exceptions (in particular, because checkCanceled() is called in {@link ServiceManager#getService(Class)}.
   */
  @Nonnull
  public static ProgressIndicator executeOnPooledThread(@Nonnull Disposable parent, @Nonnull Runnable runnable) {
    ProgressIndicator indicator = new EmptyProgressIndicator();
    indicator.start();

    CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
      ProgressManager.getInstance().runProcess(runnable, indicator);
    }, AppExecutorUtil.getAppExecutorService());

    Disposable disposable = () -> {
      if (indicator.isRunning()) indicator.cancel();
      try {
        future.get(1, TimeUnit.SECONDS);
      }
      catch (ExecutionException e) {
        if (e.getCause() instanceof ProcessCanceledException) {
          // ignore: expected cancellation
        }
        else {
          LOG.error(e);
        }
      }
      catch (InterruptedException | TimeoutException e) {
        LOG.error(e);
      }
    };

    if (!registerIfParentNotDisposed(parent, disposable)) {
      indicator.cancel();
      return indicator;
    }

    future.whenComplete((o, e) -> Disposer.dispose(disposable));

    return indicator;
  }

  public static void runUnderDisposeAwareIndicator(@Nonnull Disposable parent, @Nonnull Runnable task) {
    runUnderDisposeAwareIndicator(parent, () -> {
      task.run();
      return null;
    });
  }

  public static <T> T runUnderDisposeAwareIndicator(@Nonnull Disposable parent, @Nonnull Supplier<T> task) {
    ProgressIndicator indicator = new EmptyProgressIndicator(Application.get().getDefaultModalityState());
    indicator.start();

    Disposable disposable = () -> {
      if (indicator.isRunning()) indicator.cancel();
    };

    if (!registerIfParentNotDisposed(parent, disposable)) {
      indicator.cancel();
      throw new ProcessCanceledException();
    }

    try {
      return ProgressManager.getInstance().runProcess(task, indicator);
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  private static boolean registerIfParentNotDisposed(@Nonnull Disposable parent, @Nonnull Disposable disposable) {
    return AccessRule.read(() -> {
      if (Disposer.isDisposed(parent)) return false;
      try {
        Disposer.register(parent, disposable);
        return true;
      }
      catch (IllegalArgumentException ioe) {
        LOG.error(ioe);
        return false;
      }
    });
  }

  /**
   * Wraps {@link MessageBus#syncPublisher(Class)} in a dispose check,
   * and throws a {@link ProcessCanceledException} if the project is disposed,
   * instead of throwing an assertion which would happen otherwise.
   *
   * @see #syncPublisher(Class)
   */
  @Nonnull
  public static <L> L syncPublisher(@Nonnull ComponentManager project, @Nonnull Class<L> topic) throws ProcessCanceledException {
    return AccessRule.read(() -> {
      if (project.isDisposed()) throw new ProcessCanceledException();
      return project.getMessageBus().syncPublisher(topic);
    });
  }

  /**
   * Wraps {@link MessageBus#syncPublisher(Class)} in a dispose check,
   * and throws a {@link ProcessCanceledException} if the application is disposed,
   * instead of throwing an assertion which would happen otherwise.
   *
   * @see #syncPublisher(ComponentManager, Class)
   */
  @Nonnull
  public static <L> L syncPublisher(@Nonnull Class<L> topic) throws ProcessCanceledException {
    return AccessRule.read(() -> {
      if (ApplicationManager.getApplication().isDisposed()) throw new ProcessCanceledException();
      return ApplicationManager.getApplication().getMessageBus().syncPublisher(topic);
    });
  }


  private static class Helper<T> {
    private static final Object INITIAL_STATE = new Object();
    private static final Object SLOW_OPERATION_STATE = new Object();

    private final Semaphore mySemaphore = new Semaphore(0);
    private final AtomicReference<Object> myResultRef = new AtomicReference<>(INITIAL_STATE);

    /**
     * @return true if computation was fast, and callback should be handled by other thread
     */
    public boolean setResult(T result) {
      boolean isFast = myResultRef.compareAndSet(INITIAL_STATE, result);
      mySemaphore.release();
      return isFast;
    }

    /**
     * @return true if computation was fast, and callback should be handled by current thread
     */
    public boolean await(long waitMillis) {
      try {
        mySemaphore.tryAcquire(waitMillis, TimeUnit.MILLISECONDS);
      }
      catch (InterruptedException ignore) {
      }

      return !myResultRef.compareAndSet(INITIAL_STATE, SLOW_OPERATION_STATE);
    }

    public T getResult() {
      Object result = myResultRef.get();
      assert result != INITIAL_STATE && result != SLOW_OPERATION_STATE;
      //noinspection unchecked
      return (T)result;
    }
  }
}