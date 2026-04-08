// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import kotlinx.coroutines.flow.FlowCollector;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the state of computing some value of type {@code T}.
 * <p>
 * A null {@code result} means that the computation hasn't completed yet.
 */
public final class ComputedResult<T> {
    private final @Nullable Result<T> result;

    ComputedResult(@Nullable Result<T> result) {
        this.result = result;
    }

    public @Nullable Result<T> getResult() {
        return result;
    }

    public boolean isSuccess() {
        return result != null && result.isSuccess();
    }

    public boolean isInProgress() {
        return result == null;
    }

    public static <T> @Nonnull ComputedResult<T> loading() {
        return new ComputedResult<>(null);
    }

    public static <T> @Nonnull ComputedResult<T> success(T value) {
        return new ComputedResult<>(Result.success(value));
    }

    public static <T> @Nonnull ComputedResult<T> failure(@Nonnull Throwable error) {
        return new ComputedResult<>(Result.failure(error));
    }

    public static <T> @Nullable ComputedResult<T> compute(@Nonnull ThrowingSupplier<T> computer) {
        try {
            T result = computer.get();
            return success(result);
        }
        catch (CancellationException ce) {
            return null;
        }
        catch (Exception e) {
            return failure(e);
        }
    }

    public static <T> @Nonnull ComputedResult<T> fromResult(@Nonnull Result<T> result) {
        return new ComputedResult<>(result);
    }

    public @Nullable T getOrNull() {
        return result != null ? result.getOrNull() : null;
    }

    public @Nullable Throwable exceptionOrNull() {
        return result != null ? result.exceptionOrNull() : null;
    }

    public <R> R fold(
        @Nonnull java.util.function.Supplier<R> onInProgress,
        @Nonnull Function<T, R> onSuccess,
        @Nonnull Function<Throwable, R> onFailure
    ) {
        if (result == null) {
            return onInProgress.get();
        }
        return result.fold(onSuccess, onFailure);
    }

    public @Nonnull ComputedResult<T> onSuccess(@Nonnull Consumer<T> consumer) {
        if (result != null) {
            result.onSuccess(consumer);
        }
        return this;
    }

    public @Nonnull ComputedResult<T> onFailure(@Nonnull Consumer<Throwable> consumer) {
        if (result != null) {
            result.onFailure(consumer);
        }
        return this;
    }

    public @Nonnull ComputedResult<T> onInProgress(@Nonnull Runnable consumer) {
        if (isInProgress()) {
            consumer.run();
        }
        return this;
    }

    public <R> @Nonnull ComputedResult<R> map(@Nonnull Function<T, R> mapper) {
        return new ComputedResult<>(result != null ? result.map(mapper) : null);
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Simple Result wrapper analogous to Kotlin's Result type.
     */
    public static final class Result<T> {
        private final T value;
        private final Throwable exception;
        private final boolean success;

        private Result(T value, Throwable exception, boolean success) {
            this.value = value;
            this.exception = exception;
            this.success = success;
        }

        public static <T> @Nonnull Result<T> success(T value) {
            return new Result<>(value, null, true);
        }

        public static <T> @Nonnull Result<T> failure(@Nonnull Throwable exception) {
            return new Result<>(null, exception, false);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isFailure() {
            return !success;
        }

        public @Nullable T getOrNull() {
            return success ? value : null;
        }

        public @Nullable Throwable exceptionOrNull() {
            return exception;
        }

        public T getOrThrow() {
            if (success) {
                return value;
            }
            if (exception instanceof RuntimeException re) {
                throw re;
            }
            if (exception instanceof Error e) {
                throw e;
            }
            throw new RuntimeException(exception);
        }

        public <R> R fold(@Nonnull Function<T, R> onSuccess, @Nonnull Function<Throwable, R> onFailure) {
            return success ? onSuccess.apply(value) : onFailure.apply(exception);
        }

        public @Nonnull Result<T> onSuccess(@Nonnull Consumer<T> handler) {
            if (success) {
                handler.accept(value);
            }
            return this;
        }

        public @Nonnull Result<T> onFailure(@Nonnull Consumer<Throwable> handler) {
            if (!success && exception != null) {
                handler.accept(exception);
            }
            return this;
        }

        public <R> @Nonnull Result<R> map(@Nonnull Function<T, R> mapper) {
            if (success) {
                try {
                    return Result.success(mapper.apply(value));
                }
                catch (Throwable e) {
                    return Result.failure(e);
                }
            }
            return Result.failure(exception);
        }
    }
}
