// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.application.util.Semaphore;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.util.lang.ExceptionUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * Utility methods for progress indicator management and cancellation-aware waiting.
 *
 * @author gregsh
 */
public class ProgressIndicatorUtils {
    private static final Logger LOG = Logger.getInstance(ProgressIndicatorUtils.class);

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
    public static <T> T withTimeout(long timeoutMs, @Nonnull Supplier<T> computation) {
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

    public static <T, E extends Throwable> T computeWithLockAndCheckingCanceled(@Nonnull Lock lock, int timeout, @Nonnull TimeUnit timeUnit, @Nonnull ThrowableComputable<T, E> computable)
        throws E, ProcessCanceledException {
        awaitWithCheckCanceled(lock, timeout, timeUnit);

        try {
            return computable.compute();
        }
        finally {
            lock.unlock();
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
            catch (InterruptedException e) {
                throw new ProcessCanceledException(e);
            }
            catch (Throwable e) {
                Throwable cause = e.getCause();
                if (cause instanceof ProcessCanceledException) {
                    throw (ProcessCanceledException) cause;
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
                //noinspection InstanceofCatchParameter
                if (!(e instanceof InterruptedException)) {
                    LOG.warn(e);
                }
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
