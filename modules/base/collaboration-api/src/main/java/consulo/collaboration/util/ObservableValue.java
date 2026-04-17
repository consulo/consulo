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

import consulo.disposer.Disposable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Thread-safe mutable value holder with change listeners.
 * Replacement for Kotlin's {@code MutableStateFlow<T>}.
 *
 * @param <T> the type of value held
 */
public final class ObservableValue<T> {
    private final ReentrantLock myLock = new ReentrantLock();
    private final CopyOnWriteArrayList<Consumer<T>> myListeners = new CopyOnWriteArrayList<>();
    private volatile @Nullable T myValue;

    public ObservableValue(@Nullable T initialValue) {
        myValue = initialValue;
    }

    public @Nullable T getValue() {
        return myValue;
    }

    public void setValue(@Nullable T newValue) {
        boolean changed;
        myLock.lock();
        try {
            changed = !Objects.equals(myValue, newValue);
            myValue = newValue;
        }
        finally {
            myLock.unlock();
        }

        if (changed) {
            for (Consumer<T> listener : myListeners) {
                listener.accept(newValue);
            }
        }
    }

    /**
     * Adds a listener that is called whenever the value changes.
     *
     * @param listener the listener to add
     * @return a disposable that removes the listener when disposed
     */
    public Disposable addListener(Consumer<T> listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    /**
     * Adds a listener and immediately invokes it with the current value.
     *
     * @param listener the listener to add
     * @return a disposable that removes the listener when disposed
     */
    public Disposable addAndFireListener(Consumer<T> listener) {
        myListeners.add(listener);
        listener.accept(myValue);
        return () -> myListeners.remove(listener);
    }

    /**
     * Returns a read-only view of this observable value.
     */
    public ReadOnlyObservableValue<T> asReadOnly() {
        return new ReadOnlyObservableValue<>(this);
    }
}
