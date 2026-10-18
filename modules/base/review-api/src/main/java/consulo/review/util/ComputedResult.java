// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.review.util;

import consulo.component.ProcessCanceledException;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the state of computing some value of type {@code T}
 * <p>
 * {@link #loading()} means that the computation hasn't completed yet
 */
public final class ComputedResult<T extends @Nullable Object> {
    private static final ComputedResult<?> LOADING = new ComputedResult<>(false, null, null);

    private final boolean myCompleted;
    private final @Nullable T myValue;
    private final @Nullable Throwable myError;

    private ComputedResult(boolean completed, @Nullable T value, @Nullable Throwable error) {
        myCompleted = completed;
        myValue = value;
        myError = error;
    }

    @SuppressWarnings("unchecked")
    public static <T extends @Nullable Object> ComputedResult<T> loading() {
        return (ComputedResult<T>) LOADING;
    }

    public static <T extends @Nullable Object> ComputedResult<T> success(T value) {
        return new ComputedResult<>(true, value, null);
    }

    public static <T extends @Nullable Object> ComputedResult<T> failure(Throwable error) {
        return new ComputedResult<>(true, null, error);
    }

    /**
     * @return {@code null} when the computation was cancelled
     */
    public static <T extends @Nullable Object> @Nullable ComputedResult<T> compute(Supplier<? extends T> computer) {
        try {
            return success(computer.get());
        }
        catch (CancellationException | ProcessCanceledException e) {
            return null;
        }
        catch (Exception e) {
            return failure(e);
        }
    }

    public boolean isSuccess() {
        return myCompleted && myError == null;
    }

    public boolean isInProgress() {
        return !myCompleted;
    }

    public @Nullable T getOrNull() {
        return myValue;
    }

    public @Nullable Throwable exceptionOrNull() {
        return myError;
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    public <R extends @Nullable Object> R fold(Supplier<? extends R> onInProgress,
                                               Function<? super T, ? extends R> onSuccess,
                                               Function<? super Throwable, ? extends R> onFailure) {
        if (!myCompleted) {
            return onInProgress.get();
        }
        if (myError != null) {
            return onFailure.apply(myError);
        }
        return onSuccess.apply((T) myValue);
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    public ComputedResult<T> onSuccess(Consumer<? super T> consumer) {
        if (isSuccess()) {
            consumer.accept((T) myValue);
        }
        return this;
    }

    public ComputedResult<T> onFailure(Consumer<? super Throwable> consumer) {
        if (myError != null) {
            consumer.accept(myError);
        }
        return this;
    }

    public ComputedResult<T> onInProgress(Runnable consumer) {
        if (isInProgress()) {
            consumer.run();
        }
        return this;
    }

    @SuppressWarnings({"unchecked", "NullAway"})
    public <R extends @Nullable Object> ComputedResult<R> map(Function<? super T, ? extends R> mapper) {
        if (!isSuccess()) {
            return (ComputedResult<R>) this;
        }
        return success(mapper.apply((T) myValue));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComputedResult<?> that)) {
            return false;
        }
        return myCompleted == that.myCompleted && Objects.equals(myValue, that.myValue) && Objects.equals(myError, that.myError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myCompleted, myValue, myError);
    }

    @Override
    public String toString() {
        if (!myCompleted) {
            return "ComputedResult[in progress]";
        }
        return myError != null ? "ComputedResult[failure: " + myError + "]" : "ComputedResult[success: " + myValue + "]";
    }
}
