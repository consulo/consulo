// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

public final class ResultUtil {
    private ResultUtil() {
    }

    /**
     * Runs the {@code block} catching user exceptions (not {@link Error}, not {@link CancellationException})
     */
    @Nonnull
    public static <R> Result<R> runCatchingUser(@Nonnull ThrowingSupplier<R> block) {
        try {
            return Result.success(block.get());
        }
        catch (CancellationException ce) {
            throw ce;
        }
        catch (Exception e) {
            return Result.failure(e);
        }
    }

    /**
     * Allows processing the error before re-throwing it.
     * Will silently re-throw cancellation and JVM errors.
     */
    public static <T> T processErrorAndGet(@Nonnull Result<T> result, @Nonnull Consumer<Throwable> handler) {
        if (result.isFailure()) {
            Throwable error = result.exceptionOrNull();
            if (error != null && !(error instanceof CancellationException) && !(error instanceof Error)) {
                handler.accept(error);
            }
            return result.getOrThrow();
        }
        return result.getOrThrow();
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * A simple Result wrapper analogous to Kotlin's Result type.
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

      @Nonnull
        public static <T> Result<T> success(T value) {
            return new Result<>(value, null, true);
        }

      @Nonnull
        public static <T> Result<T> failure(@Nonnull Throwable exception) {
            return new Result<>(null, exception, false);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isFailure() {
            return !success;
        }

        public T getOrNull() {
            return success ? value : null;
        }

        public Throwable exceptionOrNull() {
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

        public @Nonnull Result<T> onFailure(@Nonnull Consumer<Throwable> handler) {
            if (!success && exception != null) {
                handler.accept(exception);
            }
            return this;
        }

        public @Nonnull Result<T> onSuccess(@Nonnull Consumer<T> handler) {
            if (success) {
                handler.accept(value);
            }
            return this;
        }
    }
}
