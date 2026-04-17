// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2025 consulo.io
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
package consulo.collaboration.util;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the state of computing some value of type T.
 * <p>
 * null value means that the computation hasn't completed yet (loading).
 * Faithfully ported from JB's Kotlin {@code ComputedResult<T>} inline value class.
 */
public final class ComputedResult<T> {
    private static final ComputedResult<?> LOADING_INSTANCE = new ComputedResult<>(null, null, false);

    private final @Nullable T myValue;
    private final @Nullable Throwable myError;
    private final boolean myHasResult;

    private ComputedResult(@Nullable T value, @Nullable Throwable error, boolean hasResult) {
        myValue = value;
        myError = error;
        myHasResult = hasResult;
    }

    @SuppressWarnings("unchecked")
    public static <T> ComputedResult<T> loading() {
        return (ComputedResult<T>) LOADING_INSTANCE;
    }

    public static <T> ComputedResult<T> success(T value) {
        return new ComputedResult<>(value, null, true);
    }

    public static <T> ComputedResult<T> failure(Throwable error) {
        return new ComputedResult<>(null, error, true);
    }

    public boolean isSuccess() {
        return myHasResult && myError == null;
    }

    public boolean isInProgress() {
        return !myHasResult;
    }

    public boolean isFailure() {
        return myHasResult && myError != null;
    }

    public @Nullable T getOrNull() {
        return myValue;
    }

    public @Nullable Throwable exceptionOrNull() {
        return myError;
    }

    public <R> R fold(Supplier0<R> onInProgress, Function<T, R> onSuccess, Function<Throwable, R> onFailure) {
        if (!myHasResult) {
            return onInProgress.get();
        }
        if (myError != null) {
            return onFailure.apply(myError);
        }
        return onSuccess.apply(myValue);
    }

    public ComputedResult<T> onSuccess(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(myValue);
        }
        return this;
    }

    public ComputedResult<T> onFailure(Consumer<Throwable> consumer) {
        if (myError != null) {
            consumer.accept(myError);
        }
        return this;
    }

    public <R> ComputedResult<R> map(Function<T, R> mapper) {
        if (!myHasResult) {
            return loading();
        }
        if (myError != null) {
            return failure(myError);
        }
        return success(mapper.apply(myValue));
    }

    @FunctionalInterface
    public interface Supplier0<R> {
        R get();
    }
}
