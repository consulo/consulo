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

import java.util.function.Consumer;

/**
 * Read-only view of an {@link ObservableValue}.
 * Replacement for Kotlin's {@code StateFlow<T>} (read-only variant).
 *
 * @param <T> the type of value held
 */
public final class ReadOnlyObservableValue<T> {
    private final ObservableValue<T> myDelegate;

    ReadOnlyObservableValue(ObservableValue<T> delegate) {
        myDelegate = delegate;
    }

    public @Nullable T getValue() {
        return myDelegate.getValue();
    }

    public Disposable addListener(Consumer<T> listener) {
        return myDelegate.addListener(listener);
    }

    public Disposable addAndFireListener(Consumer<T> listener) {
        return myDelegate.addAndFireListener(listener);
    }
}
