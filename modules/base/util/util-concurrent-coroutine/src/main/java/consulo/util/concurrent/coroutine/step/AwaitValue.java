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
package consulo.util.concurrent.coroutine.step;

import consulo.util.collection.Lists;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.Suspension;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A suspending step that waits until one or more {@link ObservableValue} sources satisfy a
 * predicate. If the predicate already holds the step completes without waiting; otherwise the
 * coroutine is suspended and re-evaluates the predicate on every change of any source.
 *
 * @author VISTALL
 * @since 2026-09-05
 */
public final class AwaitValue<I extends @Nullable Object, O extends @Nullable Object> extends CoroutineStep<I, O> {
    public static <I extends @Nullable Object, T extends @Nullable Object> AwaitValue<I, T> until(ObservableValue<T> value,
                                                                                                      Predicate<? super T> predicate) {
        return new AwaitValue<>(List.<ObservableValue<?>>of(value), input -> {
            T current = value.get();
            return predicate.test(current) ? new Matched<>(current) : null;
        });
    }

    public static <I extends @Nullable Object> AwaitValue<I, I> until(BooleanSupplier predicate, ObservableValue<?>... sources) {
        return new AwaitValue<>(List.of(sources), input -> predicate.getAsBoolean() ? new Matched<>(input) : null);
    }

    private record Matched<V extends @Nullable Object>(V value) {
    }

    private final List<ObservableValue<?>> mySources;

    private final Function<I, @Nullable Matched<O>> myMatcher;

    private AwaitValue(List<ObservableValue<?>> sources, Function<I, @Nullable Matched<O>> matcher) {
        mySources = sources;
        myMatcher = matcher;
    }

    @Override
    public void runAsync(CompletableFuture<I> previousExecution, @Nullable CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
        continuation.continueAccept(previousExecution, input -> awaitAsync(input, nextStep, continuation));
    }

    @Override
    @SuppressWarnings("NullAway")
    protected @Nullable O execute(@Nullable I input, Continuation<?> continuation) {
        Matched<O> matched = myMatcher.apply(input);
        if (matched != null) {
            return matched.value();
        }

        CompletableFuture<Matched<O>> result = new CompletableFuture<>();
        Runnable check = () -> {
            Matched<O> current = myMatcher.apply(input);
            if (current != null) {
                result.complete(current);
            }
        };

        List<Runnable> removals = listen(check);
        try {
            check.run();
            return result.join().value();
        }
        finally {
            removals.forEach(Runnable::run);
        }
    }

    private void awaitAsync(I input, @Nullable CoroutineStep<O, ?> nextStep, Continuation<?> continuation) {
        Suspension<O> suspension = continuation.suspend(this, nextStep);

        Matched<O> matched = myMatcher.apply(input);
        if (matched != null) {
            suspension.resume(matched.value());
            return;
        }

        AtomicBoolean done = new AtomicBoolean();
        List<Runnable> removals = Lists.newLockFreeCopyOnWriteList();
        Runnable stopListening = () -> removals.forEach(Runnable::run);

        Runnable check = () -> {
            Matched<O> current = myMatcher.apply(input);
            if (current != null && done.compareAndSet(false, true)) {
                stopListening.run();
                suspension.ifNotCancelled(() -> suspension.resume(current.value()));
            }
        };

        for (ObservableValue<?> source : mySources) {
            removals.add(source.addListener(value -> check.run()));
        }
        suspension.onCancel(Optional.of(stopListening));

        check.run();
        if (done.get() || suspension.isCancelled()) {
            stopListening.run();
        }
    }

    private List<Runnable> listen(Runnable check) {
        List<Runnable> removals = Lists.newLockFreeCopyOnWriteList();
        for (ObservableValue<?> source : mySources) {
            removals.add(source.addListener(value -> check.run()));
        }
        return removals;
    }
}
