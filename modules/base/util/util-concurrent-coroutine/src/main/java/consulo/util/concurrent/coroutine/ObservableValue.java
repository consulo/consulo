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
package consulo.util.concurrent.coroutine;

import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A read-only view of a single value that notifies listeners about changes. Listeners are
 * invoked on the thread that changed the value and only see the latest value, so intermediate
 * values of rapid successive changes may be skipped.
 *
 * @author VISTALL
 * @since 2026-10-18
 * @see MutableObservableValue
 */
public interface ObservableValue<T extends @Nullable Object> {
    static <T extends @Nullable Object> ObservableValue<T> of(T value) {
        return MutableObservableValue.of(value);
    }

    static <A extends @Nullable Object, B extends @Nullable Object, R extends @Nullable Object> ObservableValue<R> combine(
        ObservableValue<A> first,
        ObservableValue<B> second,
        BiFunction<? super A, ? super B, ? extends R> combiner
    ) {
        MutableObservableValue<R> combined = MutableObservableValue.of(combiner.apply(first.get(), second.get()));
        first.addListener(value -> combined.set(combiner.apply(first.get(), second.get())));
        second.addListener(value -> combined.set(combiner.apply(first.get(), second.get())));
        return combined;
    }

    T get();

    /**
     * @return removes the listener
     */
    Runnable addListener(Consumer<? super T> listener);

    default <R extends @Nullable Object> ObservableValue<R> map(Function<? super T, ? extends R> mapper) {
        MutableObservableValue<R> mapped = MutableObservableValue.of(mapper.apply(get()));
        addListener(value -> mapped.set(mapper.apply(get())));
        return mapped;
    }
}
