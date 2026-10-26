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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FutureUtil {
    private FutureUtil() {
    }

    public static boolean isCancellation(@Nullable Throwable error) {
        return error != null && unwrap(error) instanceof CancellationException;
    }

    public static Throwable unwrap(Throwable error) {
        Throwable result = error;
        while (result instanceof CompletionException || result instanceof ExecutionException) {
            Throwable cause = result.getCause();
            if (cause == null) {
                break;
            }
            result = cause;
        }
        return result;
    }

    public static Exception unwrapException(Throwable error) {
        Throwable cause = unwrap(error);
        return cause instanceof Exception exception ? exception : new CompletionException(cause);
    }

    /**
     * Maps items concurrently and preserves their order.
     * Runs at most {@code concurrency} transforms at the same time. The {@code concurrency} value must be positive.
     */
    /**
     * The first value of {@code value} - the current one or a later one - accepted by {@code predicate}.
     * The returned future is cancelled when {@code parentDisposable} is disposed first.
     */
    public static <T extends @Nullable Object> CompletableFuture<T> first(Disposable parentDisposable,
                                                                          ObservableValue<T> value,
                                                                          Predicate<? super T> predicate) {
        CompletableFuture<T> result = new CompletableFuture<>();
        Disposable subscription = Disposable.newDisposable("FutureUtil.first");
        if (!Disposer.tryRegister(parentDisposable, subscription)) {
            result.cancel(false);
            return result;
        }
        Disposer.register(subscription, () -> result.cancel(false));
        Runnable unsubscribe = value.addListener(newValue -> {
            if (predicate.test(newValue)) {
                result.complete(newValue);
            }
        });
        Disposer.register(subscription, unsubscribe::run);
        T current = value.get();
        if (predicate.test(current)) {
            result.complete(current);
        }
        result.whenComplete((it, error) -> Disposer.dispose(subscription));
        return result;
    }

    public static <T, R extends @Nullable Object> CompletableFuture<List<R>> mapConcurrentOrdered(Iterable<T> items,
                                                                                                  int concurrency,
                                                                                                  Function<T, CompletableFuture<R>> transform) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException("Concurrency must be positive, but was " + concurrency);
        }
        List<T> list = new ArrayList<>();
        items.forEach(list::add);
        List<CompletableFuture<R>> results = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            results.add(new CompletableFuture<>());
        }
        AtomicInteger next = new AtomicInteger();
        for (int i = 0; i < Math.min(concurrency, list.size()); i++) {
            launchNext(list, results, next, transform);
        }
        CompletableFuture<List<R>> result = CompletableFuture.allOf(results.toArray(CompletableFuture[]::new))
            .thenApply(ignored -> {
                List<R> values = new ArrayList<>(results.size());
                for (CompletableFuture<R> future : results) {
                    values.add(future.join());
                }
                return values;
            });
        result.whenComplete((value, error) -> {
            if (result.isCancelled() || error != null) {
                results.forEach(it -> it.cancel(false));
            }
        });
        return result;
    }

    private static <T, R extends @Nullable Object> void launchNext(List<T> items,
                                                                   List<CompletableFuture<R>> results,
                                                                   AtomicInteger next,
                                                                   Function<T, CompletableFuture<R>> transform) {
        int index = next.getAndIncrement();
        if (index >= items.size()) {
            return;
        }
        CompletableFuture<R> target = results.get(index);
        if (target.isDone()) {
            return;
        }
        CompletableFuture<R> started;
        try {
            started = transform.apply(items.get(index));
        }
        catch (Throwable e) {
            started = CompletableFuture.failedFuture(e);
        }
        started.whenComplete((value, error) -> {
            if (error != null) {
                target.completeExceptionally(unwrap(error));
            }
            else {
                target.complete(value);
            }
            launchNext(items, results, next, transform);
        });
    }
}
