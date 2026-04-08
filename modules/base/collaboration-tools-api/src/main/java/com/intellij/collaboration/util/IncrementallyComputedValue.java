// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

@ApiStatus.Experimental
public final class IncrementallyComputedValue<T> {
    private final @Nullable T value;
    private final boolean hasValue;
    private final boolean complete;
    private final @Nullable Exception exception;

    private IncrementallyComputedValue(@Nullable T value, boolean hasValue, boolean complete, @Nullable Exception exception) {
        this.value = value;
        this.hasValue = hasValue;
        this.complete = complete;
        this.exception = exception;
    }

    public boolean isLoading() {
        return !complete && exception == null;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isValueAvailable() {
        return hasValue;
    }

    @Nonnull
    public T getValueOrNull() {
        return hasValue ? value : null;
    }

    @Nonnull
    public Exception getExceptionOrNull() {
        return exception;
    }

    @Nonnull
    public static <T> IncrementallyComputedValue<T> loading() {
        return new IncrementallyComputedValue<>(null, false, false, null);
    }

    @Nonnull
    public static <T> IncrementallyComputedValue<T> partialSuccess(@Nonnull T value) {
        return new IncrementallyComputedValue<>(value, true, false, null);
    }

    @Nonnull
    public static <T> IncrementallyComputedValue<T> success(@Nonnull T value) {
        return new IncrementallyComputedValue<>(value, true, true, null);
    }

    @Nonnull
    public static <T> IncrementallyComputedValue<T> partialFailure(@Nonnull T value, @Nonnull Exception error) {
        return new IncrementallyComputedValue<>(value, true, false, error);
    }

    @Nonnull
    public static <T> IncrementallyComputedValue<T> failure(@Nonnull Exception error) {
        return new IncrementallyComputedValue<>(null, false, false, error);
    }

    public @Nonnull IncrementallyComputedValue<T> onValueAvailable(@Nonnull Consumer<T> consumer) {
        if (hasValue) {
            consumer.accept(value);
        }
        return this;
    }

    public @Nonnull IncrementallyComputedValue<T> onNoValue(@Nonnull Runnable handler) {
        if (!hasValue) {
            handler.run();
        }
        return this;
    }
}
