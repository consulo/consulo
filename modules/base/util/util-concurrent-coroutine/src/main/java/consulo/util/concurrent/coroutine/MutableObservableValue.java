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

import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A thread-safe holder of a single value that notifies listeners about changes. Listeners are
 * invoked on the thread that changed the value and only see the latest value, so intermediate
 * values of rapid successive changes may be skipped.
 * <p>
 * Expose it as {@link ObservableValue} where callers must only read.
 *
 * @author VISTALL
 * @since 2026-09-05
 */
public final class MutableObservableValue<T extends @Nullable Object> implements ObservableValue<T> {
    public static <T extends @Nullable Object> MutableObservableValue<T> of(T initialValue) {
        return new MutableObservableValue<>(initialValue);
    }

    private final AtomicReference<T> myValue;

    private final List<Consumer<? super T>> myListeners = Lists.newLockFreeCopyOnWriteList();

    public MutableObservableValue(T initialValue) {
        myValue = new AtomicReference<>(initialValue);
    }

    @Override
    public T get() {
        return myValue.get();
    }

    public void set(T value) {
        T previous = myValue.getAndSet(value);
        if (!Objects.equals(previous, value)) {
            fireChanged(value);
        }
    }

    public boolean compareAndSet(T expect, T update) {
        if (!myValue.compareAndSet(expect, update)) {
            return false;
        }
        if (!Objects.equals(expect, update)) {
            fireChanged(update);
        }
        return true;
    }

    public T getAndUpdate(UnaryOperator<T> operator) {
        T previous;
        T next;
        do {
            previous = myValue.get();
            next = operator.apply(previous);
        }
        while (!myValue.compareAndSet(previous, next));

        if (!Objects.equals(previous, next)) {
            fireChanged(next);
        }
        return previous;
    }

    public T updateAndGet(UnaryOperator<T> operator) {
        T previous;
        T next;
        do {
            previous = myValue.get();
            next = operator.apply(previous);
        }
        while (!myValue.compareAndSet(previous, next));

        if (!Objects.equals(previous, next)) {
            fireChanged(next);
        }
        return next;
    }

    public void update(UnaryOperator<T> operator) {
        updateAndGet(operator);
    }

    @Override
    public Runnable addListener(Consumer<? super T> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    public int listenerCount() {
        return myListeners.size();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), myValue.get());
    }

    private void fireChanged(T value) {
        for (Consumer<? super T> listener : myListeners) {
            listener.accept(value);
        }
    }
}
