// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.async;

import com.intellij.collaboration.util.ProgressIndicatorsProvider;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

/**
 * Collection of utilities to use CompletableFuture and not care about CF and platform quirks
 *
 * @deprecated Deprecated with migration to coroutines
 */
@Deprecated
public final class CompletableFutureUtil {
    private CompletableFutureUtil() {
    }

    /**
     * Check if the exception is a cancellation signal
     */
    public static boolean isCancellation(@Nonnull Throwable error) {
        return error instanceof ProcessCanceledException
            || error instanceof CancellationException
            || error instanceof InterruptedException
            || (error.getCause() != null && isCancellation(error.getCause()));
    }

    /**
     * Extract actual exception from the one returned by completable future
     */
    public static @Nonnull Throwable extractError(@Nonnull Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return extractError(error.getCause());
        }
        if (error instanceof ExecutionException && error.getCause() != null) {
            return extractError(error.getCause());
        }
        return error;
    }

    /**
     * Submit a task to IO thread pool under correct {@link ProgressIndicator}
     *
     * @deprecated Deprecated with migration to coroutines
     */
    @Deprecated
    public static <T> @Nonnull CompletableFuture<T> submitIOTask(
        @Nonnull ProgressManager progressManager,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Function<ProgressIndicator, T> task
    ) {
        return submitIOTask(progressManager, progressIndicator, false, task);
    }

    /**
     * Submit a task to IO thread pool under correct {@link ProgressIndicator}
     */
    @ApiStatus.Internal
    @Deprecated
    @Nonnull
    public static <T>  CompletableFuture<T> submitIOTask(
        @Nonnull ProgressManager progressManager,
        @Nonnull ProgressIndicator progressIndicator,
        boolean cancelIndicatorOnFutureCancel,
        @Nonnull Function<ProgressIndicator, T> task
    ) {
        return CompletableFuture.supplyAsync(
                (Supplier<T>) () -> progressManager.runProcess(
                    () -> task.apply(progressIndicator),
                    progressIndicator
                ),
                ProcessIOExecutorService.INSTANCE
            )
            .whenComplete((result, error) -> {
                if (cancelIndicatorOnFutureCancel && error != null && isCancellation(error) && !progressIndicator.isCanceled()) {
                    progressIndicator.cancel();
                }
            });
    }

    /**
     * Submit a task to IO thread pool under correct {@link ProgressIndicator} acquired from {@code indicatorProvider}
     * and release the indicator when task is completed
     *
     * @deprecated Deprecated with migration to coroutines
     */
    @Deprecated
    @Nonnull
    public static <T> CompletableFuture<T> submitIOTask(
        @Nonnull ProgressManager progressManager,
        @Nonnull ProgressIndicatorsProvider indicatorProvider,
        @Nonnull Function<ProgressIndicator, T> task
    ) {
        return submitIOTask(progressManager, indicatorProvider, false, task);
    }

    /**
     * Submit a task to IO thread pool under correct {@link ProgressIndicator} acquired from {@code indicatorProvider}
     * and release the indicator when task is completed
     */
    @ApiStatus.Internal
    @Deprecated
    @Nonnull
    public static <T> CompletableFuture<T> submitIOTask(
        @Nonnull ProgressManager progressManager,
        @Nonnull ProgressIndicatorsProvider indicatorProvider,
        boolean cancelIndicatorOnFutureCancel,
        @Nonnull Function<ProgressIndicator, T> task
    ) {
        ProgressIndicator indicator = indicatorProvider.acquireIndicator();
        return submitIOTask(progressManager, indicator, cancelIndicatorOnFutureCancel, task)
            .whenComplete((r, e) -> indicatorProvider.releaseIndicator(indicator));
    }

    /**
     * Handle the result of async computation on EDT.
     * <p>
     * To allow proper GC the handler is cleaned up when {@code disposable} is disposed.
     *
     * @param handler invoked when computation completes
     * @deprecated Deprecated with migration to coroutines
     */
    @ApiStatus.Internal
    @ApiStatus.ScheduledForRemoval
    @Deprecated
    public static <T> @Nonnull CompletableFuture<Void> handleOnEdt(
        @Nonnull CompletableFuture<T> future,
        @Nonnull Disposable disposable,
        @Nonnull BiConsumer<T, Throwable> handler
    ) {
        AtomicReference<BiConsumer<T, Throwable>> handlerReference = new AtomicReference<>(handler);
        Disposer.register(disposable, () -> handlerReference.set(null));

        return future.handleAsync(
            (BiFunction<T, Throwable, Void>) (result, error) -> {
                var handlerFromRef = handlerReference.get();
                if (handlerFromRef == null) {
                    throw new ProcessCanceledException();
                }
                handlerFromRef.accept(result, error != null ? extractError(error) : null);
                return null;
            },
            getEDTExecutor(null)
        );
    }

    /**
     * Handle the result of async computation on EDT
     *
     * @param handler invoked when computation completes
     * @deprecated Deprecated with migration to coroutines
     */
    @ApiStatus.Internal
    @Deprecated
    @Nonnull
    public static <T, R> CompletableFuture<R> handleOnEdt(
        @Nonnull CompletableFuture<T> future,
        @Nullable ModalityState modalityState,
        @Nonnull BiFunction<T, Throwable, R> handler
    ) {
        return future.handleAsync(
            (result, error) ->
                handler.apply(result, error != null ? extractError(error) : null), getEDTExecutor(modalityState)
        );
    }

    @Nonnull
    public static <T, R> CompletableFuture<R> handleOnEdt(
        @Nonnull CompletableFuture<T> future,
        @Nonnull BiFunction<T, Throwable, R> handler
    ) {
        return handleOnEdt(future, null, handler);
    }

    /**
     * Handle the result of async computation on EDT
     *
     * @param handler invoked when computation completes without exception
     * @deprecated Deprecated with migration to coroutines
     */
    @Deprecated
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T, R> CompletableFuture<R> successOnEdt(
        @Nonnull CompletableFuture<T> future,
        @Nullable ModalityState modalityState,
        @Nonnull Function<T, R> handler
    ) {
        return handleOnEdt(future, modalityState, (result, error) -> {
            if (error != null) {
                throw wrapAsRuntimeIfNeeded(extractError(error));
            }
            return handler.apply(result);
        });
    }

    /**
     * Handle the error on EDT
     *
     * @param handler invoked when computation throws an exception which IS NOT a cancellation
     * @deprecated Deprecated with migration to coroutines
     */
    @Deprecated
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> errorOnEdt(
        @Nonnull CompletableFuture<T> future,
        @Nullable ModalityState modalityState,
        @Nonnull Consumer<Throwable> handler
    ) {
        return handleOnEdt(
            future,
            modalityState,
            (result, error) -> {
                if (error != null) {
                    Throwable actualError = extractError(error);
                    if (isCancellation(actualError)) {
                        throw new ProcessCanceledException();
                    }
                    handler.accept(actualError);
                    throw wrapAsRuntimeIfNeeded(actualError);
                }
                return result;
            }
        );
    }

    private static @Nonnull Executor getEDTExecutor(@Nullable ModalityState modalityState) {
        return runnable -> Application.get().invokeLater(
            runnable,
            modalityState != null ? modalityState : ModalityState.defaultModalityState()
        );
    }

    private static @Nonnull RuntimeException wrapAsRuntimeIfNeeded(@Nonnull Throwable t) {
        if (t instanceof RuntimeException re) {
            return re;
        }
        if (t instanceof Error e) {
            throw e;
        }
        return new CompletionException(t);
    }
}
