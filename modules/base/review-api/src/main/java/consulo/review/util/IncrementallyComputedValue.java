// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class IncrementallyComputedValue<T extends @Nullable Object> {
    private final boolean myValueAvailable;
    private final @Nullable T myValue;
    private final boolean myComplete;
    private final boolean myLoading;
    private final @Nullable Exception myException;

    private IncrementallyComputedValue(boolean valueAvailable, @Nullable T value, boolean complete, boolean loading, @Nullable Exception exception) {
        myValueAvailable = valueAvailable;
        myValue = value;
        myComplete = complete;
        myLoading = loading;
        myException = exception;
    }

    public boolean isLoading() {
        return myLoading;
    }

    public boolean isComplete() {
        return myComplete;
    }

    public boolean isValueAvailable() {
        return myValueAvailable;
    }

    public @Nullable T getValueOrNull() {
        return myValue;
    }

    public @Nullable Exception getExceptionOrNull() {
        return myException;
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> initial() {
        return new IncrementallyComputedValue<>(false, null, false, false, null);
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> loading() {
        return new IncrementallyComputedValue<>(false, null, false, true, null);
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> partialSuccess(T value) {
        return partialSuccess(value, true);
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> partialSuccess(T value, boolean loading) {
        return new IncrementallyComputedValue<>(true, value, false, loading, null);
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> success(T value) {
        return new IncrementallyComputedValue<>(true, value, true, false, null);
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> partialFailure(T value, Exception error) {
        return new IncrementallyComputedValue<>(true, value, false, false, error);
    }

    public static <T extends @Nullable Object> IncrementallyComputedValue<T> failure(Exception error) {
        return new IncrementallyComputedValue<>(false, null, false, false, error);
    }

    public IncrementallyComputedValue<T> complete(Supplier<T> ifEmpty) {
        return new IncrementallyComputedValue<>(true, myValueAvailable ? myValue : ifEmpty.get(), true, myLoading, myException);
    }

    public IncrementallyComputedValue<T> withLoading(boolean loading) {
        return new IncrementallyComputedValue<>(myValueAvailable, myValue, myComplete, loading, myException);
    }

    public IncrementallyComputedValue<T> appendPart(boolean lastPart, Function<@Nullable T, T> append) {
        T newValue = append.apply(myValue);
        return new IncrementallyComputedValue<>(true, newValue, lastPart, myLoading, myException);
    }

    public IncrementallyComputedValue<T> withException(Exception exception) {
        return new IncrementallyComputedValue<>(myValueAvailable, myValue, myComplete, myLoading, exception);
    }

    @SuppressWarnings("NullAway")
    public IncrementallyComputedValue<T> onValueAvailable(Consumer<T> consumer) {
        if (myValueAvailable) {
            consumer.accept(myValue);
        }
        return this;
    }

    public IncrementallyComputedValue<T> onNoValue(Runnable handler) {
        if (!myValueAvailable) {
            handler.run();
        }
        return this;
    }

    /**
     * Feeds {@code collector} with the state of a list computed from the batches {@code batches} produces.
     *
     * @param batches given a consumer of batches, produces them one after another and completes after the last one
     */
    public static <T> CompletableFuture<?> collectIncrementallyTo(Function<Consumer<List<T>>, CompletableFuture<?>> batches,
                                                                  Consumer<IncrementallyComputedValue<List<T>>> collector) {
        collector.accept(loading());
        List<T> collected = new ArrayList<>();
        boolean[] anyBatch = new boolean[1];
        CompletableFuture<?> production = batches.apply(batch -> {
            anyBatch[0] = true;
            collected.addAll(batch);
            collector.accept(partialSuccess(List.copyOf(collected)));
        });
        return production.handle((result, error) -> {
            if (error == null) {
                if (anyBatch[0]) {
                    collector.accept(success(List.copyOf(collected)));
                }
                return null;
            }
            Exception exception = error instanceof Exception e ? e : new RuntimeException(error);
            if (anyBatch[0]) {
                collector.accept(partialFailure(List.copyOf(collected), exception));
            }
            else {
                collector.accept(failure(exception));
            }
            return null;
        });
    }

    /**
     * Hands every new part of the list in {@code source} to {@code batchConsumer} until the list is complete or fails.
     *
     * @return unsubscribes from {@code source}
     */
    public static <T> Runnable consumeIncrementally(ObservableValue<IncrementallyComputedValue<List<T>>> source,
                                                    Consumer<List<T>> batchConsumer,
                                                    Consumer<Exception> onError) {
        int[] counter = new int[1];
        boolean[] finished = new boolean[1];
        Runnable[] unsubscribe = new Runnable[1];
        Consumer<IncrementallyComputedValue<List<T>>> handler = computedValue -> {
            synchronized (counter) {
                if (finished[0]) {
                    return;
                }
                List<T> items = computedValue.getValueOrNull();
                if (items != null && items.size() > counter[0]) {
                    batchConsumer.accept(items.subList(counter[0], items.size()));
                    counter[0] = items.size();
                }
                if (computedValue.isComplete()) {
                    finished[0] = true;
                }
                else {
                    Exception exception = computedValue.getExceptionOrNull();
                    if (exception != null) {
                        onError.accept(exception);
                        finished[0] = true;
                    }
                }
                if (finished[0] && unsubscribe[0] != null) {
                    unsubscribe[0].run();
                }
            }
        };
        unsubscribe[0] = source.addListener(handler);
        handler.accept(source.get());
        return () -> {
            synchronized (counter) {
                finished[0] = true;
            }
            unsubscribe[0].run();
        };
    }
}
